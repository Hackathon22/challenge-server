package net

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class TestServerClient {

    @Test
    fun testConnection() {
        val server = ServerSession()
        val client = ClientSession()


        assertDoesNotThrow {
            server.start()

            val clientThread = Thread {
                client.connect("127.0.0.1", "TestUsername", "TestPassword")
            }
            clientThread.start()

            clientThread.join()

            server.stop()
        }
    }

    @Test
    fun testDisconnection() {
        val server = ServerSession()
        val client = ClientSession()

        server.start()

        val clientThread = Thread {
            client.connect("127.0.0.1", "TestUsername", "TestPassword")
            client.disconnect()
        }
        clientThread.start()
        clientThread.join()

        server.stop()

    }

    @Test
    fun testReconnection() {
        val server = ServerSession()
        val client = ClientSession()

        server.start()

        val clientThread = Thread {
            client.connect("127.0.0.1", "TestUsername", "TestPassword")
            client.disconnect()
            client.connect("127.0.0.1", "TestUsername", "TestPassword")
            Thread.sleep(100)
            client.disconnect()
        }
        clientThread.start()
        clientThread.join()

        server.stop()
    }

    @Test
    fun testMultipleSessionsSameUsername() {
        val server = ServerSession()
        val client1 = ClientSession()
        val client2 = ClientSession()

        server.start()

        val clientThread = Thread {
            client1.connect("127.0.0.1", "TestUsername", "TestPassword")
            Thread.sleep(100)
            client2.connect("127.0.0.1", "TestUsername", "TestPassword")
        }
        clientThread.start()
        clientThread.join()

    }
}