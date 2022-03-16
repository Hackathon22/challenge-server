package systems

import components.*
import core.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import javax.swing.plaf.nimbus.State


data class PlayerData(val pos: Vec3F, val speed: Vec3F, val state: States, val health: Float)

class PythonClient(
    private val client: Socket,
    private val username: String,
    private val entity: Entity
) {
    fun pollActions(instance: Instance): StateCommand {
        val output = PrintWriter(client.getOutputStream(), true)
        val input = BufferedReader(InputStreamReader(client.getInputStream()))

        // poll the game state for the AI

        // gets all the entities
        val allEntities = instance.getAllEntities()

        val projectiles = ArrayList<Pair<Vec3F, Vec3F>>()
        allEntities.forEach {
            val projectileComponent =
                instance.getComponentDynamicUnsafe(it, ProjectileComponent::class)
            if (projectileComponent != null) {
                val transformComponent = instance.getComponent<TransformComponent>(it)
                val dynamicComponent = instance.getComponent<DynamicComponent>(it)
                projectiles.add(Pair(transformComponent.pos, dynamicComponent.speed))
            }
        }

        val otherPlayers = ArrayList<PlayerData>()
        allEntities.forEach {
            val characterComponent =
                instance.getComponentDynamicUnsafe(it, CharacterComponent::class)
            if (characterComponent != null) {
                val characterComponentCasted = characterComponent as CharacterComponent
                val transformComponent = instance.getComponent<TransformComponent>(it)
                val dynamicComponent = instance.getComponent<DynamicComponent>(it)
                val stateComponent = instance.getComponent<StateComponent>(it)
                val playerData = PlayerData(
                    transformComponent.pos,
                    dynamicComponent.speed,
                    stateComponent.state,
                    characterComponentCasted.health
                )
                otherPlayers.add(playerData)
            }
        }

        val walls = ArrayList<Pair<Vec3F, Vec2F>>()
        allEntities.forEach {
            val bodyComponent = instance.getComponentDynamicUnsafe(it, BodyComponent::class)
            if (bodyComponent != null) {
                val bodyComponentCasted = bodyComponent as BodyComponent
                if (bodyComponentCasted.static) {
                    val transformComponent = instance.getComponent<TransformComponent>(it)
                    walls.add(
                        Pair(
                            transformComponent.pos,
                            Vec2F(
                                bodyComponent.width * transformComponent.scale.x,
                                bodyComponent.height * transformComponent.scale.y
                            )
                        )
                    )
                }
            }
        }

        val controlledPlayerTransformComponent = instance.getComponent<TransformComponent>(entity)
        val controlledPlayerDynamicComponent = instance.getComponent<DynamicComponent>(entity)
        val controlledPlayerCharacterComponent = instance.getComponent<CharacterComponent>(entity)
        val controlledPlayerStateComponent = instance.getComponent<StateComponent>(entity)
        val controllerPlayer = PlayerData(
            controlledPlayerTransformComponent.pos,
            controlledPlayerDynamicComponent.speed,
            controlledPlayerStateComponent.state,
            controlledPlayerCharacterComponent.health
        )

        TODO("Serialize the data")
        TODO("ask for action")
    }
}

class PythonAISystem : System() {

    private val _serverSocket = ServerSocket(2049)

    private val _clients = HashMap<String, PythonClient>()

    private val _usernameToEntity = HashMap<String, Entity>()

    fun addAgent(entity: Entity, username: String, port: Int): Boolean {
        try {
            val client = _serverSocket.accept()
            val inputStream = BufferedReader(InputStreamReader(client.getInputStream()))

            // gets the username, this operation is blocking and waits for the python script to connect
            val username = inputStream.readLine()
            if (username != null && username != "" && username != "\n") {
                _clients[username] = PythonClient(client, username, entity)
                _usernameToEntity[username] = entity
                println("Agent connected with username $username for entity $entity")
                return true
            }
            return false
        } catch (exc: IOException) {
            return false
        }
    }

    override fun initializeLogic(vararg arg: Any): Boolean {
        return true
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
}