package net

import game.DesktopClient
import org.junit.jupiter.api.Test

class TestSnapshotUpdate {

    @Test
    fun testDeltaClientServer() {
        var receivedDeltaSnapshot = false

        val server = ServerSession()

        val client = object : DesktopClient() {

        }
        server.start()

        val clientThread = Thread {
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