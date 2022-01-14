package net

import NetworkComponent
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Server
import com.esotericsoftware.minlog.Log
import components.DynamicComponent
import components.TransformComponent
import core.Instance
import net.packets.*
import systems.MovementSystem
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger


const val DEFAULT_PORT_TCP = 2049
const val DEFAULT_PORT_UDP = 2050

const val DEFAULT_TICKRATE = 15

object NetworkLogger {
    private val _logger = Logger.getLogger("Server")

    init {
        val handler = object : Handler() {
            override fun publish(record: LogRecord) {
                println("${record.level} - ${LocalDateTime.now()} - ${record.message}")
            }

            override fun flush() {
            }

            override fun close() {
            }
        }
        _logger.useParentHandlers = false
        _logger.addHandler(handler)
    }

    fun log(level: Level, message: String) {
        _logger.log(level, message)
    }
}

object InstanceLogger {
    private val _logger = Logger.getLogger("Instance")

    init {
        val handler = object : Handler() {
            override fun publish(record: LogRecord) {
                println("${record.level} - ${LocalDateTime.now()} - ${record.message}")
            }

            override fun flush() {
            }

            override fun close() {
            }
        }
        _logger.useParentHandlers = false
        _logger.addHandler(handler)
    }

    fun log(level: Level, message: String) {
        _logger.log(level, message)
    }
}

enum class ClientStatus {
    DISCONNECTED, // no connection established
    CONNECTED, // connection established but not authenticated
    AUTHENTICATED // connection established and authenticated
}

class ClientInstance {

    var status = ClientStatus.DISCONNECTED

    var username: String? = null
        get() { // username getter checks if the username is initialized
            assert(field != null)
            return field!!
        }

    fun connect(username: String) {
        this.username = username
        this.status = ClientStatus.AUTHENTICATED
    }

    fun logout() {
        assert(this.status == ClientStatus.AUTHENTICATED)
        this.status = ClientStatus.CONNECTED
    }

    fun disconnect() {
        this.status = ClientStatus.DISCONNECTED
    }

    override fun toString(): String {
        return "ClientInstance(status=$status, username=$username)"
    }
}

class ServerSession(
    private val _sessionId: Int = 0,
    private val _tcpPort: Int = DEFAULT_PORT_TCP,
    private val _udpPort: Int = DEFAULT_PORT_UDP,
    private val _tickRate: Int = DEFAULT_TICKRATE) {

    private var server = Server()
    private var _initializedClass = false
    private val _instanceMap = HashMap<Int, ClientInstance>()
    private val _usernameMap = HashMap<String, Connection>()

    /** Tick counter */
    private var _tick = AtomicInteger(0)

    /** Flag to check whatever the server is currently running a game */
    private var _running = AtomicBoolean(false)

    init {
        Log.set(Log.LEVEL_NONE)
    }

    fun start() {
        if (!_initializedClass) {
            registerPackets(server.kryo)
            _initializedClass = true
        }

        server.start()
        server.bind(_tcpPort, _udpPort)

        val listener = object : Listener() {

            override fun connected(connection: Connection) {
                NetworkLogger.log(Level.INFO, "Connection established with id: ${connection.id}")
                super.connected(connection)
            }

            override fun disconnected(connection: Connection) {
                NetworkLogger.log(Level.INFO, "Connection closed with id: ${connection.id}")

                // sets the instance as offline
                val instance = _instanceMap[connection.id]
                if (instance != null) {
                    instance.disconnect()
                    _instanceMap.remove(connection.id)
                }
                // find the key of the disconnecting connection
                val reversedUsernameMap = _usernameMap.entries.associate { (k, v) -> v to k }
                val resultKey = reversedUsernameMap[connection]
                // remove the connection from the username map
                if (resultKey != null) {
                    _usernameMap.remove(resultKey)
                }

                super.disconnected(connection)
            }

            override fun received(connection: Connection, obj: Any) {
                NetworkLogger.log(
                    Level.ALL,
                    "Packet of type ${obj::class.simpleName} received from connection ${connection.id}"
                )
                when (obj) {
                    is LoginPacket -> handleLogin(connection, obj)
                    is LogoffPacket -> handleLogoff(connection)
                }
            }
        }
        server.addListener(listener)

        NetworkLogger.log(Level.INFO, "Server started with ports TCP: $DEFAULT_PORT_TCP | UDP: $DEFAULT_PORT_UDP")
    }

    fun stop() {
        // kick every user
        server.sendToAllTCP(KickPacket("Server instance closing."))
        server.stop()
        NetworkLogger.log(Level.INFO, "Server stopped.")
    }

    /**
     * Start the instance, entering a game loop that will simulate all the ticks and send network information.
     */
    fun startInstance() {
        _running.set(true)

        val timePerTickNs = (10e9f / _tickRate.toFloat()).toLong()
        val timePerTickMs = (timePerTickNs / 10e3)
        val timePerTickS = (timePerTickNs.toFloat() / 10e9f)

        // game instance on the stack as all the network logic shouldn't need to directly access it. (Thread safety +
        // decoupling)
        val instance = Instance()

        InstanceLogger.log(Level.INFO, "New game instance created on session $_sessionId.")

        // registers statically all the components
        instance.registerComponent<TransformComponent>()
        instance.registerComponent<DynamicComponent>()
        instance.registerComponent<NetworkComponent>()

        InstanceLogger.log(Level.INFO, "Registered components on instance $_sessionId.")

        // registers statically all the systems
        val movementSystem = instance.registerSystem<MovementSystem>()
        val networkSystem = instance.registerSystem<ServerNetworkSystem>()


        // network synchronizer implementation for our network system
        val networkSynchronizer = object : NetworkSynchronizer {
            override fun sendProperties(changes: ChangedProperties) = sendAllLogged(DeltaSnapshotPacket(_tick.get(), changes))
        }
        movementSystem.initialize()
        networkSystem.initialize(networkSynchronizer)

        InstanceLogger.log(Level.INFO, "Registered and initialized systems on instance $_sessionId.")

        // TODO ("Notify the users and send a full snapshot")
        while (_running.get()) {
            val startTime = Instant.now()

            // game loop
            movementSystem.update(instance, timePerTickS)
            networkSystem.update(instance, timePerTickS)

            // increments tick
            _tick.incrementAndGet()
            // calculates how much time did the loop computed.
            val endTime = Instant.now()
            val elapsed = Duration.between(startTime, endTime).toNanos()
            if (elapsed <= timePerTickNs) {
                Thread.sleep(((timePerTickNs - elapsed)/10e6).toLong())
            } else {
                // tick update took too long, this might cause severe lag issues
                NetworkLogger.log(
                    Level.WARNING,
                    "Server is overloaded, tick update took too long: ${elapsed / 1000}ms (Max time: $timePerTickMs)"
                )
            }
        }
    }

    /**
     * Stops the instance, finishing the game (but not kicking the users)
     */
    fun stopInstance() {
        _running.set(false)
        _tick.set(0)
        // TODO ("Send packets to notify the users")
    }

    private fun handleLogin(connection: Connection, packet: LoginPacket) {
        // at this point the credentials are alright

        // checks if there is already a session for this username and kicks it if the case
        val kickedConnection = _usernameMap[packet.username]
        kickedConnection?.sendTCP(KickPacket("Another session has opened with the same username: ${packet.username}."))
        kickedConnection?.close()

        if (_usernameMap[packet.username] == null) {
            // insert a new session as this user connected for the first time
            val instance = ClientInstance()
            instance.connect(packet.username)

            _instanceMap[connection.id] = instance
            _usernameMap[packet.username] = connection
            NetworkLogger.log(Level.INFO, "User logged for the first time with username: ${packet.username}")
        } else {
            // there is already a session connected, the user was only delogged
            _instanceMap[connection.id]!!.connect(packet.username)
            _usernameMap[packet.username] = connection
            NetworkLogger.log(Level.INFO, "User ${packet.username} re-logged to the server.")
        }

        _usernameMap[packet.username] = connection
    }

    private fun handleLogoff(connection: Connection) {
        if (_instanceMap[connection.id] != null) {
            _instanceMap[connection.id]!!.logout()
            NetworkLogger.log(Level.INFO, "Connection ${connection.id} logged off.")
        }
    }

    /**
     * Send a packet to all the clients that have a logged session.
     */
    private fun sendAllLogged(packet: Any, exception: Int? = null) {
        _instanceMap.forEach { id, session ->
            if (session.status == ClientStatus.AUTHENTICATED) {
                if (exception != null && id == exception) return@forEach
                _usernameMap[session.username]!!.sendTCP(packet)
            }
        }
    }

}
