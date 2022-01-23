package net

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import core.Instance
import core.System
import net.packets.*
import render.SpriteRegister
import systems.CameraSystem
import systems.SpriteRenderSystem
import systems.WindowSystem
import java.util.concurrent.atomic.AtomicBoolean

const val DEFAULT_TIMEOUT = 10000

const val BASE_WIDTH = 1200
const val BASE_HEIGHT = 800

open class ClientSession(val tcpPort: Int = DEFAULT_PORT_TCP,
                         val udpPort: Int = DEFAULT_PORT_UDP) : ApplicationAdapter() {

    private val _client = Client()

    private var _status = ClientStatus.DISCONNECTED
    private var _initializedClass = false
    private var _username : String? = null

    private val _running = AtomicBoolean(false)

    private val _instance = Instance()

    private var _windowSystem : System = _instance.registerSystem<WindowSystem>()

    private var _cameraSystem : System = _instance.registerSystem<CameraSystem>()

    private var _spriteSystem : System = _instance.registerSystem<SpriteRenderSystem>()

    init {
        // initializes systems
        _windowSystem.initialize(BASE_WIDTH, BASE_HEIGHT)
        _cameraSystem.initialize(BASE_WIDTH, BASE_HEIGHT)
        _spriteSystem.initialize()

        // sets observers
        _windowSystem.addObserver(_cameraSystem)
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

    }

    fun isConnected() = (_status == ClientStatus.CONNECTED)

    override fun create() {
        // Loads all the game sprites
        SpriteRegister.initialize()
    }

    override fun render() {
        val deltaTime = Gdx.graphics.deltaTime
        _windowSystem.update(_instance, deltaTime)
        _cameraSystem.update(_instance, deltaTime)
        _spriteSystem.update(_instance, deltaTime)
    }
}
