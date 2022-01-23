package net

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import components.*
import core.*
import net.packets.*
import parser.SceneParser
import render.SpriteRegister
import systems.CameraSystem
import systems.MovementSystem
import systems.SpriteRenderSystem
import java.util.concurrent.atomic.AtomicBoolean

const val DEFAULT_TIMEOUT = 10000

const val BASE_WIDTH = 1200
const val BASE_HEIGHT = 800

open class ClientSession(private val scenePath : String? = "src/main/resources/scenes/base_scene.xml",
                         private val tcpPort: Int = DEFAULT_PORT_TCP,
                         private val udpPort: Int = DEFAULT_PORT_UDP) : ApplicationAdapter(), IObservable {

    private val _client = Client()

    private var _status = ClientStatus.DISCONNECTED
    private var _initializedClass = false
    private var _username : String? = null

    private val _running = AtomicBoolean(false)

    private val _instance = Instance()

    private val _movementSystem : System = _instance.registerSystem<MovementSystem>()

    private val _cameraSystem : System = _instance.registerSystem<CameraSystem>()

    private val _spriteSystem : System = _instance.registerSystem<SpriteRenderSystem>()

    override val observers = ArrayList<IObserver>()

    init {
        // registers components
        _instance.registerComponent<NetworkComponent>()
        _instance.registerComponent<TransformComponent>()
        _instance.registerComponent<DynamicComponent>()
        _instance.registerComponent<CameraComponent>()
        _instance.registerComponent<SpriteComponent>()

        // initializes non-graphical systems
        _movementSystem.initialize()
        val movementSignature = Signature()
        movementSignature.set(_instance.getComponentType<TransformComponent>(), true)
        movementSignature.set(_instance.getComponentType<DynamicComponent>(), true)
        _instance.setSystemSignature<MovementSystem>(movementSignature)

        // sets observers on non-graphical systems

    }

    fun connect(address: String, username: String, password: String) {
        _username = username

        if (!_initializedClass) {
            registerPackets(_client.kryo)
            _initializedClass = true
        }

        _client.start()
        val listener = object : Listener() {
            override fun received(connection: Connection, obj: Any) {
                when (obj) {
                    is DeltaSnapshotPacket -> handleDeltaSnapshot(obj)
                    is LoginResponsePacket -> handleLoginResponse(obj)
                    is KickPacket -> handleKick(obj)
                    is FullSnapshotResponsePacket -> handleFullSnapshot(obj)
                }
            }
        }
        _client.addListener(listener)

        _client.connect(DEFAULT_TIMEOUT, address, tcpPort, udpPort)
        _status = ClientStatus.CONNECTED

        // tries to log-in
        _client.sendTCP(LoginPacket(username, password))
    }

    fun disconnect() {
        _client.close()
        _status = ClientStatus.DISCONNECTED
    }

    private fun handleLoginResponse(packet: LoginResponsePacket) {
        if (packet.success) {
            _status = ClientStatus.AUTHENTICATED
        }
        else {
            disconnect()
        }
    }

    private fun handleKick(packet: KickPacket) {
        println("Kicked by server. Reason: ${packet.reason}")
        disconnect()
    }

    protected fun handleDeltaSnapshot(packet: DeltaSnapshotPacket) {
        println("Delta snapshot received: $packet")
        TODO("Implement")
    }

    protected fun handleFullSnapshot(packet: FullSnapshotResponsePacket) {
        TODO("Implement")
    }

    override fun create() {
        // Loads all the game sprites
        SpriteRegister.initialize()

        // initializes graphical systems
        _cameraSystem.initialize(BASE_WIDTH, BASE_HEIGHT)
        val cameraSignature = Signature()
        cameraSignature.set(_instance.getComponentType<CameraComponent>(), true)
        cameraSignature.set(_instance.getComponentType<TransformComponent>(), true)
        _instance.setSystemSignature<CameraSystem>(cameraSignature)

        _spriteSystem.initialize()
        val spriteSignature = Signature()
        spriteSignature.set(_instance.getComponentType<TransformComponent>(), true)
        spriteSignature.set(_instance.getComponentType<SpriteComponent>(), true)
        _instance.setSystemSignature<SpriteRenderSystem>(spriteSignature)

        // set observers on graphical systems
        this.addObserver(_cameraSystem)

        // loads the scene
        if (scenePath != null) {
            val scene = SceneParser.loadScene(scenePath)
            scene.forEach { (_, components) ->
                val entity = _instance.createEntity()
                components.forEach {
                    _instance.addComponentDynamic(entity, it)
                }
            }
        }

        _running.set(true)
    }

    override fun dispose() {
        _running.set(false)
    }

    override fun resize(width: Int, height: Int) {
        notifyObservers(WindowResizeEvent(Vec2F(width.toFloat(), height.toFloat())))
    }

    override fun render() {
        val deltaTime = Gdx.graphics.deltaTime
        _cameraSystem.update(_instance, deltaTime)
        _spriteSystem.update(_instance, deltaTime)
    }
}
