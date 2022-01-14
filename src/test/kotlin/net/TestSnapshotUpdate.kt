package net

import net.packets.DeltaSnapshotPacket
import org.junit.jupiter.api.Test

class TestSnapshotUpdate {

    @Test
    fun testDeltaClientServer() {
        // TODO ("Introduce entities to server")
        var receivedDeltaSnapshot = false

        val server = ServerSession()

        val client = object : ClientSession() {

            override fun handleDeltaSnapshot(packet: DeltaSnapshotPacket) {
                receivedDeltaSnapshot = true
            }

        }
        server.start()

        val clientThread = Thread {
            client.connect("127.0.0.1", "TestUsername", "TestPassword")
            // waits a bit to receive delta snapshots
            Thread.sleep(500)
        }

        val serverThread = Thread {
            server.startInstance()
        }

        clientThread.start()
        serverThread.start()

        clientThread.join()

        server.stopInstance()
        serverThread.stop()

        assert(receivedDeltaSnapshot)
    }

}