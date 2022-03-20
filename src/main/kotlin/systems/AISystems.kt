package systems

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import components.*
import core.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

interface JSONConvertable {
    fun toJSON() : String = Gson().toJson(this)
}

data class PlayerData(@SerializedName("pos") val pos: Vec3F,
                      @SerializedName("speed") val speed: Vec3F,
                      @SerializedName("state") val state: States,
                      @SerializedName("health") val health: Float,
                      @SerializedName("score") val score: Float) : JSONConvertable

data class ProjectileData(@SerializedName("pos") val pos: Vec3F,
                          @SerializedName("speed") val speed: Vec3F) : JSONConvertable

data class SnapshotData(@SerializedName("controlledPlayer") val controlledPlayer: PlayerData,
                        @SerializedName("otherPlayers") val otherPlayers: List<PlayerData>,
                        @SerializedName("projectiles") val projectiles: List<ProjectileData>) : JSONConvertable

class PythonClient(
    private val client: Socket,
    private val username: String,
    private val entity: Entity,
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
        val serializedData = snapshotData.toJSON()
        // sends the data to the client
        output.write(serializedData)

        // starts the countdown of the timer

        TODO("Implement countdown")
        
        val serializedResult = input.readLine()

        // parse the result

        // returns the command
    }

    private fun gatherSnapshot(instance: Instance) : SnapshotData {

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
            if (characterComponent != null) {
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
            controlledPlayerScoreComponent.score
        )
        return SnapshotData(controllerPlayer, otherPlayers, projectiles)
    }

    fun abort() {
        TODO("implement")
    }

    fun finished(results: List<GameResult>) {
        TODO("implement")
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
            abort()
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
            val command = client.pollActions(instance)
            val entity = _usernameToEntity[username]!!
            val commandComponent = instance.getComponent<CommandComponent>(entity)
            if (commandComponent.controllerType == ControllerType.AI) {
                commandComponent.commands.add(command)
            }
        }
    }

    override fun onEntityAdded(entity: Entity) {
    }

    override fun onEntityRemoved(entity: Entity) {
    }

    private fun abort() {
        _clients.forEach { (_, session) ->
            session.abort()
        }
    }

    fun finish(results: List<GameResult>) {
        _clients.forEach { (_, session) ->
            session.finished(results)
        }
    }
}