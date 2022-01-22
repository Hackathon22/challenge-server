package net

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import core.Instance
import net.packets.*
import systems.CameraSystem
import systems.SpriteRenderSystem
import systems.WindowSystem
import java.util.concurrent.atomic.AtomicBoolean

const val DEFAULT_TIMEOUT = 10000

open class ClientSession(val tcpPort: Int = DEFAULT_PORT_TCP,
                         val udpPort: Int = DEFAULT_PORT_UDP) : ApplicationAdapter() {

    private val _client = Client()

    private var _status = ClientStatus.DISCONNECTED
    private var _initializedClass = false
    private var _username : String? = null

    private val _running = AtomicBoolean(false)

    private val _instance = Instance()

    private val _windowSystem = WindowSystem()

    private val _cameraSystem = CameraSystem()

    private val _spriteSystem = SpriteRenderSystem()

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
        println("Created")
        val windowWidth = 1200
        val windowHeight = 800

        // initializes systems
        _windowSystem.initialize(windowWidth, windowHeight)
        _cameraSystem.initialize(windowWidth, windowHeight)
        _spriteSystem.initialize()

        // sets observers
        _windowSystem.addObserver(_cameraSystem)
    }

    override fun render() {
        val deltaTime = Gdx.graphics.deltaTime
        _windowSystem.update(_instance, deltaTime)
        _cameraSystem.update(_instance, deltaTime)
        _spriteSystem.update(_instance, deltaTime)
    }
}

fun main() {
    val applicationConfiguration = LwjglApplicationConfiguration()
    applicationConfiguration.title = "ARSWA - Client"
    applicationConfiguration.width = 1200
    applicationConfiguration.height = 800

    val clientSession = ClientSession()

    LwjglApplication(clientSession, applicationConfiguration)
}
