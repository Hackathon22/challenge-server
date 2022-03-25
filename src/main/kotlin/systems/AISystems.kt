package systems

import com.google.common.util.concurrent.SimpleTimeLimiter
import com.google.common.util.concurrent.UncheckedTimeoutException
import com.google.gson.*
import com.google.gson.annotations.SerializedName
import components.*
import core.*
import java.io.*
import java.lang.Exception
import java.lang.RuntimeException
import java.lang.reflect.Type
import java.net.ServerSocket
import java.net.Socket
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


// JSON serialization solution from
// https://stackoverflow.com/questions/44117970/kotlin-data-class-from-json-using-gson
interface JSONConvertable {
    fun toJSON(): String = Gson().toJson(this)
}

// adding a fromJSON function to string objects
inline fun <reified T : JSONConvertable> String.toObject(): T = Gson().fromJson(this, T::class.java)


// Snapshot data classes to send to the python AI agent
data class PlayerData(
    @SerializedName("pos") val pos: Vec3F,
    @SerializedName("speed") val speed: Vec3F,
    @SerializedName("state") val state: States,
    @SerializedName("health") val health: Float,
    @SerializedName("team") val team: Int,
    @SerializedName("score") val score: Float
) : JSONConvertable

data class ProjectileData(
    @SerializedName("pos") val pos: Vec3F,
    @SerializedName("speed") val speed: Vec3F
) : JSONConvertable

data class SnapshotData(
    @SerializedName("controlledPlayer") val controlledPlayer: PlayerData,
    @SerializedName("otherPlayers") val otherPlayers: List<PlayerData>,
    @SerializedName("projectiles") val projectiles: List<ProjectileData>
) : JSONConvertable

// Command data classes received from the python AI agent
@NoArg
open class AICommand(@SerializedName("command_type") val commandType: String) : JSONConvertable

data class AIMoveCommand(@SerializedName("move_direction") val moveDirection: List<Float>) :
    AICommand("MOVE"), JSONConvertable

data class AIShootCommand(@SerializedName("shoot_angle") val shootDirection: Float) :
    AICommand("SHOOT"), JSONConvertable

// Messages sent to the python AI to ask for a new command or notice the end of the game.
enum class MessageHeaders : JSONConvertable {
    ASK_COMMAND,
    GAME_FINISHED,
    ABORT
}

abstract class AIMessage(@SerializedName("header") val header: MessageHeaders) : JSONConvertable

data class AskCommandMessage(@SerializedName("snapshot") val snapshot: SnapshotData) :
    AIMessage(MessageHeaders.ASK_COMMAND), JSONConvertable

data class GameFinishedMessage(@SerializedName("score") val score: List<ScoreResult>) :
    AIMessage(MessageHeaders.GAME_FINISHED), JSONConvertable

data class AbortMessage(
    @SerializedName("error") val message: String,
    @SerializedName("blame") val blame: String
) :
    AIMessage(MessageHeaders.ABORT), JSONConvertable

class PythonClient(
    private val client: Socket,
    private val username: String,
    val entity: Entity,
    private val team: Int,
    private var time: Float
) {

    private var _remainingTimeNs = (time * 10e9f).toLong()
    init {
        println("Initialized python client with time: $time, - $_remainingTimeNs")
    }

    fun pollActions(instance: Instance): StateCommand {
        // streams to send/receive messages
        val output = PrintWriter(client.getOutputStream(), true)
        val input = BufferedReader(InputStreamReader(client.getInputStream()))

        // Retrieves the game state and serialized it before sending it to the python AI.
        val snapshotData = gatherSnapshot(instance)
        // serializes the data
        val askCommandMessage = AskCommandMessage(snapshotData)
        val serializedData = askCommandMessage.toJSON()

        // sends the data to the client
        output.println(serializedData + "\n")
        output.flush()

        // waits for the result to come
        val beginTime = Instant.now()
        val serializedResult = input.readLine()
        val duration = Duration.between(beginTime, Instant.now()).toNanos()
        _remainingTimeNs -= duration

        // gets the base class from the command
        val aiCommand = serializedResult.toObject<AICommand>()

        // parses the result
        return when (aiCommand.commandType) {
            "MOVE" -> {
                val moveCommand = serializedResult.toObject<AIMoveCommand>()
                val direction = moveCommand.moveDirection
                MoveCommand(Vec3F(direction[0], direction[1], direction[2]))
            }
            "SHOOT" -> {
                val shootCommand = serializedResult.toObject<AIShootCommand>()
                val angle = shootCommand.shootDirection
                ShootCommand(angle)
            }
            else -> {
                throw IllegalArgumentException("Invalid command type.")
            }
        }
    }

    private fun gatherSnapshot(instance: Instance): SnapshotData {

        // Retrieves all the projectiles in the game
        val projectileSystem = instance.getSystem<ProjectileSystem>()
        val projectiles = ArrayList<ProjectileData>()
        projectileSystem.entities.forEach {
            val projectileComponent =
                instance.getComponentDynamicUnsafe(it, ProjectileComponent::class)
            if (projectileComponent != null) {
                val transformComponent = instance.getComponent<TransformComponent>(it)
                val dynamicComponent = instance.getComponent<DynamicComponent>(it)
                projectiles.add(ProjectileData(transformComponent.pos, dynamicComponent.speed))
            }
        }

        val scoreSystem = instance.getSystem<ScoreSystem>()
        val otherPlayers = ArrayList<PlayerData>()
        scoreSystem.entities.forEach {
            val characterComponent =
                instance.getComponentDynamicUnsafe(it, CharacterComponent::class)
            if (characterComponent != null && it != entity) {
                val characterComponentCasted = characterComponent as CharacterComponent
                val transformComponent = instance.getComponent<TransformComponent>(it)
                val dynamicComponent = instance.getComponent<DynamicComponent>(it)
                val stateComponent = instance.getComponent<StateComponent>(it)
                val scoreComponent = instance.getComponent<ScoreComponent>(it)
                val playerData = PlayerData(
                    transformComponent.pos,
                    dynamicComponent.speed,
                    stateComponent.state,
                    characterComponentCasted.health,
                    scoreComponent.team,
                    scoreComponent.score
                )
                otherPlayers.add(playerData)
            }
        }

        val controlledPlayerTransformComponent = instance.getComponent<TransformComponent>(entity)
        val controlledPlayerDynamicComponent = instance.getComponent<DynamicComponent>(entity)
        val controlledPlayerCharacterComponent = instance.getComponent<CharacterComponent>(entity)
        val controlledPlayerStateComponent = instance.getComponent<StateComponent>(entity)
        val controlledPlayerScoreComponent = instance.getComponent<ScoreComponent>(entity)
        val controllerPlayer = PlayerData(
            controlledPlayerTransformComponent.pos,
            controlledPlayerDynamicComponent.speed,
            controlledPlayerStateComponent.state,
            controlledPlayerCharacterComponent.health,
            controlledPlayerScoreComponent.team,
            controlledPlayerScoreComponent.score
        )
        return SnapshotData(controllerPlayer, otherPlayers, projectiles)
    }

    fun abort(message: String, blame: String) {
        val abortMessage = AbortMessage(message, blame)
        val serializedMessage = abortMessage.toJSON()
        val output = PrintWriter(client.getOutputStream(), true)
        output.println(serializedMessage + "\n")
        Thread.sleep(500)
        client.close()
    }

    fun finished(results: List<ScoreResult>) {
        val gameFinishedMessage = GameFinishedMessage(results)
        val serializedMessage = gameFinishedMessage.toJSON()
        val output = PrintWriter(client.getOutputStream(), true)
        output.println(serializedMessage + "\n")
        Thread.sleep(500)
        client.close()
    }
}

// Agent data is returned by the python AI system upon the connection of a new agent
data class AgentData(
    @SerializedName("valid") val valid: Boolean,
    @SerializedName("username") val username: String,
    @SerializedName("team") val team: Int,
    @SerializedName("entity") val entity: Entity
) : JSONConvertable

data class AgentOutputCommand(
    @SerializedName("time") val time: Float,
    @SerializedName("entity") val entity: Entity,
    @SerializedName("command") val command: StateCommand
) : JSONConvertable

class AgentOutputSaveDeserializer : JsonDeserializer<AgentsOutputSave> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): AgentsOutputSave {
        val outputSave = AgentsOutputSave()
        val document = json.asJsonObject

        outputSave.commandsPerSecond = document.get("commandsPerSecond").asFloat
        outputSave.gameTime = document.get("gameTime").asFloat
        outputSave.aiTime = document.get("aiTime").asFloat

        val documentAgents = document.get("agents").asJsonArray
        documentAgents.forEach { elem ->
            outputSave.addAgent(elem.asJsonObject.toString().toObject())
        }

        val documentCommands = document.get("commands").asJsonArray
        documentCommands.forEach { element ->
            val jsonCommand = element.asJsonObject
            val jsonStateCommand = jsonCommand.get("command").asJsonObject
            when (jsonStateCommand.get("commandType").asString) {
                "moveCommand" -> outputSave.addCommand(
                    jsonStateCommand.asJsonObject.toString().toObject<MoveCommand>(),
                    jsonCommand.get("time").asFloat,
                    jsonCommand.get("entity").asInt
                )
                "shootCommand" -> outputSave.addCommand(
                    jsonStateCommand.asJsonObject.toString().toObject<ShootCommand>(),
                    jsonCommand.get("time").asFloat,
                    jsonCommand.get("entity").asInt
                )
                else -> throw JsonParseException("Unknown state command type: ${jsonCommand.get("commandType").asString}")
            }
        }

        return outputSave
    }

}

// This is a saved file of the commands pushed by the players
class AgentsOutputSave : JSONConvertable {
    @SerializedName("commandsPerSecond")
    var commandsPerSecond = 4.0f

    @SerializedName("gameTime")
    var gameTime = 60.0f

    @SerializedName("aiTime")
    var aiTime = 150.0f

    @SerializedName("agents")
    val agents = ArrayList<AgentData>()

    @SerializedName("commands")
    val commands = ArrayList<AgentOutputCommand>()

    fun addAgent(agentData: AgentData) {
        agents.add(agentData)
    }

    fun addCommand(command: StateCommand, time: Float, agentEntity: Entity) {
        commands.add(AgentOutputCommand(time, agentEntity, command))
    }

    fun saveToFile(filePath: String) {
        val file = FileOutputStream(filePath)
        val output = PrintWriter(file)

        val serializedSave = this.toJSON()
        output.write(serializedSave + "\n")
        output.flush()
        output.close()
    }

    // Static method to load from a file
    companion object {
        fun loadFromFile(filePath: String): AgentsOutputSave {
            val file = FileInputStream(filePath)
            val input = BufferedReader(InputStreamReader(file))

            val serializedSave = input.readText()

            val builder = GsonBuilder()
            builder.registerTypeAdapter(AgentsOutputSave::class.java, AgentOutputSaveDeserializer())
            val gson = builder.create()

            return gson.fromJson(serializedSave, AgentsOutputSave::class.java)
        }
    }

}

class PythonAISystem : System() {

    private var _port = 0

    private var _serverSocket: ServerSocket? = null

    private val _clients = HashMap<String, PythonClient>()

    private val _usernameToEntity = HashMap<String, Entity>()

    private val _commandSave = AgentsOutputSave()

    private var _aiTime: Float? = null

    private var _gameTime: Float? = null

    private var _commandsPerSecond: Float? = null

    private var _savePath: String? = null

    private var _aborted: Boolean = false

    // used to save the timestamps of the commands
    private var _timeCounter = 0f

    fun addAgent(instance: Instance): AgentData {
        try {
            println("Waiting for an agent to connect on port $_port.")
            val client = _serverSocket!!.accept()
            println("Client accepted with address: ${client.inetAddress}")
            val inputStream = BufferedReader(InputStreamReader(client.getInputStream()))

            // gets the username, this operation is blocking and waits for the python script to connect
            val username = inputStream.readLine()
            val team = Integer.parseInt(inputStream.readLine())
            if (username != null && username != "" && username != "\n") {
                val entity = instance.createEntity()
                _clients[username] = PythonClient(client, username, entity, team, _aiTime!!)
                _usernameToEntity[username] = entity
                println(
                    "Agent connected with username: $username and team: $team." +
                            " Assigned to player entity: $entity"
                )
                val agentData = AgentData(true, username, team, entity)
                _commandSave.addAgent(agentData)
                return agentData
            }
            return AgentData(false, "", 0, 0)
        } catch (exc: IOException) {
            abort("Exception when connecting the client.", "server")
            return AgentData(false, "", 0, 0)
        }
    }

    override fun initializeLogic(vararg arg: Any): Boolean {
        if (arg.size < 4) {
            return false
        }
        return try {
            _aiTime = arg[0] as Float
            _gameTime = arg[1] as Float
            _commandsPerSecond = arg[2] as Float
            _savePath = arg[3] as String
            _port = arg[4] as Int

            _serverSocket = ServerSocket(_port)

            _commandSave.commandsPerSecond = _commandsPerSecond!!
            _commandSave.aiTime = _aiTime!!
            _commandSave.gameTime = _gameTime!!
            true
        } catch (exc: TypeCastException) {
            false
        }
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        // gives the game state and ask for each client the action to play (game state command)
        _clients.forEach { (username, client) ->
            try {
                val command = client.pollActions(instance)
                _commandSave.addCommand(command, _timeCounter, client.entity)
                val entity = _usernameToEntity[username]!!
                val commandComponent = instance.getComponent<CommandComponent>(entity)
                if (commandComponent.controllerType == ControllerType.AI) {
                    commandComponent.commands.add(command)
                }
            } catch (exc: Exception) {
                println("Exception occurred when parsing the action for user: $username")
                exc.printStackTrace()
                val scoreSystem = instance.getSystem<ScoreSystem>()
                scoreSystem.forceFinishGame(instance, client.entity)
                abort(
                    "Exception occurred when parsing the action for user $username:\n${exc}",
                    username
                )
                return
            }
        }
        // updates the time counter
        _timeCounter += delta
    }

    override fun onEntityAdded(entity: Entity) {
    }

    override fun onEntityRemoved(entity: Entity) {
    }

    fun abort(message: String, blame: String) {
        _clients.forEach { (_, session) ->
            session.abort(message, blame)
        }
        _aborted = true
    }

    fun finish(results: List<ScoreResult>) {
        _clients.forEach { (_, session) ->
            session.finished(results)
        }
    }

    fun aborted(): Boolean {
        return _aborted
    }

    fun saveToFile() {
        _commandSave.saveToFile(_savePath!!)
    }
}

class ReplaySystem : System() {

    private var _agentOutputSave: AgentsOutputSave? = null

    private var _commands: Queue<StateCommand> = LinkedList()

    var finished: Boolean = false
        private set

    override fun initializeLogic(vararg arg: Any): Boolean {
        assert(arg.isNotEmpty())
        return try {
            val filePath = arg[0] as String
            _agentOutputSave = AgentsOutputSave.loadFromFile(filePath)
            _agentOutputSave!!.commands.forEach {
                _commands.add(it.command)
            }
            true
        } catch (exc: TypeCastException) {
            false
        }
    }

    override fun updateLogic(instance: Instance, delta: Float) {
        _agentOutputSave!!.agents.forEach { agent ->
            val stateCommand = _commands.poll()
            if (_commands.isEmpty())
                finished = true
            val commandComponent = instance.getComponent<CommandComponent>(agent.entity)
            if (commandComponent.controllerType == ControllerType.AI)
                commandComponent.commands.add(stateCommand)
        }
    }

    override fun onEntityAdded(entity: Entity) {
    }

    override fun onEntityRemoved(entity: Entity) {
    }

    fun gameTime(): Float = _agentOutputSave!!.gameTime

    fun aiTime(): Float = _agentOutputSave!!.aiTime

    fun commandsPerSeconds(): Float = _agentOutputSave!!.commandsPerSecond

    fun agents(): List<AgentData> = _agentOutputSave!!.agents
}