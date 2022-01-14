package net

import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Server
import com.esotericsoftware.minlog.Log
import net.packets.*
import java.time.LocalDateTime
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger


const val DEFAULT_PORT_TCP = 2049
const val DEFAULT_PORT_UDP = 2050


class NetworkLogger {
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
        _logger.addHandler(handler)
    }

    fun log(level: Level, message: String) {
        _logger.log(level, message)
    }
}

val NETWORK_LOGGER = NetworkLogger()

enum class ClientStatus {
    DISCONNECTED, // no connection established
    CONNECTED, // connection established but not authenticated
    AUTHENTICATED // connection established and authenticated
}

class ClientInstance {

    var status = ClientStatus.DISCONNECTED

    private var username: String? = null
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

class ServerSession(private val _tcpPort: Int = DEFAULT_PORT_TCP, private val _udpPort: Int = DEFAULT_PORT_UDP) {

    private var server = Server()
    private var _initializedClass = false
    private val _instanceMap = HashMap<Int, ClientInstance>()
    private val _usernameMap = HashMap<String, Connection>()

    private val _systems = ArrayList<System>()

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
                NETWORK_LOGGER.log(Level.INFO, "Connection established with id: ${connection.id}")
                super.connected(connection)
            }

            override fun disconnected(connection: Connection) {
                NETWORK_LOGGER.log(Level.INFO, "Connection closed with id: ${connection.id}")

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
                NETWORK_LOGGER.log(
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

        NETWORK_LOGGER.log(Level.INFO, "Server started with ports TCP: $DEFAULT_PORT_TCP | UDP: $DEFAULT_PORT_UDP")
    }

    fun stop() {
        // kick every user
        server.sendToAllTCP(KickPacket("Server instance closing."))
        server.stop()
        NETWORK_LOGGER.log(Level.INFO, "Server stopped.")
    }

    /**
     * Start the instance, entering a game loop that will simulate all the ticks and send network information.
     */
    private fun startInstance() {
        // registers statistically all the systems and components


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
            NETWORK_LOGGER.log(Level.INFO, "User logged for the first time with username: ${packet.username}")
        } else {
            // there is already a session connected, the user was only delogged
            _instanceMap[connection.id]!!.connect(packet.username)
            _usernameMap[packet.username] = connection
            NETWORK_LOGGER.log(Level.INFO, "User ${packet.username} re-logged to the server.")
        }

        _usernameMap[packet.username] = connection
    }

    private fun handleLogoff(connection: Connection) {
        if (_instanceMap[connection.id] != null) {
            _instanceMap[connection.id]!!.logout()
            NETWORK_LOGGER.log(Level.INFO, "Connection ${connection.id} logged off.")
        }
    }
}
