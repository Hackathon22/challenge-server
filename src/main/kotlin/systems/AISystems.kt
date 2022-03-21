package systems

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import components.*
import core.*
import java.io.*
import java.lang.Exception
import java.net.ServerSocket
import java.net.Socket


// JSON serialization solution from
// https://stackoverflow.com/questions/44117970/kotlin-data-class-from-json-using-gson
interface JSONConvertable {
    fun toJSON(): String = Gson().toJson(this)
}

inline fun <reified T : JSONConvertable> String.toObject(): T = Gson().fromJson(this, T::class.java)

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

@NoArg
open class AICommand(@SerializedName("command_type") val commandType: String) : JSONConvertable

data class AIMoveCommand(@SerializedName("move_direction") val moveDirection: List<Float>) :
    AICommand("MOVE"), JSONConvertable

data class AIShootCommand(@SerializedName("shoot_angle") val shootDirection: Float) :
    AICommand("SHOOT"), JSONConvertable

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

        val serializedResult = input.readLine()

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
        client.close()
    }

    fun finished(results: List<ScoreResult>) {
        val gameFinishedMessage = GameFinishedMessage(results)
        val serializedMessage = gameFinishedMessage.toJSON()
        val output = PrintWriter(client.getOutputStream(), true)
        output.println(serializedMessage + "\n")
        client.close()
    }
}

data class AgentData(val valid: Boolean, val username: String, val team: Int, val entity: Entity)

class PythonAISystem : System() {

    private val _port = 2049

    private val _serverSocket = ServerSocket(_port)

    private val _clients = HashMap<String, PythonClient>()

    private val _usernameToEntity = HashMap<String, Entity>()

    private var _aiTime: Float? = null

    private var _savePath: String? = null

    fun addAgent(instance: Instance): AgentData {
        try {
            println("Waiting for an agent to connect on port $_port.")
            val client = _serverSocket.accept()
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
                return AgentData(true, username, team, entity)
            }
            return AgentData(false, "", 0, 0)
        } catch (exc: IOException) {
            abort("Exception when connecting the client.", "server")
            return AgentData(false, "", 0, 0)
        }
    }

    override fun initializeLogic(vararg arg: Any): Boolean {
        if (arg.size < 2) {
            return false
        }
        return try {
            _aiTime = arg[0] as Float
            _savePath = arg[1] as String
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
                val entity = _usernameToEntity[username]!!
                val commandComponent = instance.getComponent<CommandComponent>(entity)
                if (commandComponent.controllerType == ControllerType.AI) {
                    commandComponent.commands.add(command)
                }
            } catch (exc: Exception) {
                println("Exception occurred when parsing the action for user: $username")
                val scoreSystem = instance.getSystem<ScoreSystem>()
                scoreSystem.forceFinishGame(instance, client.entity)
                abort(
                    "Exception occurred when parsing the action for user $username:\n${exc}",
                    "player: $username"
                )
            }
        }
    }

    override fun onEntityAdded(entity: Entity) {
    }

    override fun onEntityRemoved(entity: Entity) {
    }

    private fun abort(message: String, blame: String) {
        _clients.forEach { (_, session) ->
            session.abort(message, blame)
        }
    }

    fun finish(results: List<ScoreResult>) {
        _clients.forEach { (_, session) ->
            session.finished(results)
        }
    }
}