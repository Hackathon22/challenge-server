package net.packets

import com.esotericsoftware.kryo.Kryo
import core.NoArg
import systems.FullSnapshot
import systems.NetworkedProperties
import java.util.*

/**
 * Packet sent by the client to the server in order to log-in.
 * @param username the username, will be used in-game
 * @param password the password, will be used by the server to authenticate on the API.
 */
@NoArg
data class LoginPacket(val username: String, val password: String)

/**
 * Packet sent by the client to the server in order to log-off.
 */
@NoArg
class LogoffPacket

/**
 * Packet sent by the server to the client as a response as a Login attempt.
 * @param success if the authentication was successful or not.
 * @param message empty if successful, explaining the error if not.
 */
@NoArg
data class LoginResponsePacket(val success: Boolean, val message: String)

/**
 * Packet sent by the server to the client when kicking it.
 *
 * @param reason, message explaining why is the user kicked
 */
@NoArg
data class KickPacket(val reason: String)

/**
 * Packet sent by the server to the client when the tick delta update is ready
 */
@NoArg
data class DeltaSnapshotPacket(val tick: Int,
                               val properties: NetworkedProperties,
                               val addedEntities: FullSnapshot,
                               val removedEntities: List<UUID>)

/**
 * Packet sent by the client to the server to request a full Snapshot.
 */
@NoArg
class FullSnapshotPacket

/**
 * Packet sent by the server to the username as a FullSnapshotPacket response.
 * It contains the complete state of all the entities that have a network component.
 */
@NoArg
data class FullSnapshotResponsePacket(val tick: Int, val sceneName: String, val entities: FullSnapshot)

/**
 * Static function that registers all the defined packets in the kryo object.
 */
fun registerPackets(kryo: Kryo) {
    kryo.register(LoginPacket::class.java)
    kryo.register(LogoffPacket::class.java)
    kryo.register(LoginResponsePacket::class.java)
    kryo.register(KickPacket::class.java)
    kryo.register(DeltaSnapshotPacket::class.java)
    kryo.register(FullSnapshotPacket::class.java)
    kryo.register(FullSnapshotResponsePacket::class.java)
    kryo.register(java.util.HashMap::class.java)
}
