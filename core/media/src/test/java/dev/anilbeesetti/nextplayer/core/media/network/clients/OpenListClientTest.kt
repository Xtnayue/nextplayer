package dev.anilbeesetti.nextplayer.core.media.network.clients

import dev.anilbeesetti.nextplayer.core.model.NetworkConnection
import dev.anilbeesetti.nextplayer.core.model.NetworkProtocol
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OpenListClientTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `authenticated client lists files and streams with range`() = runTest {
        server.enqueue(jsonResponse("""{"code":200,"data":{"token":"secret-token"}}"""))
        server.enqueue(listResponse())
        server.enqueue(listResponse())
        server.enqueue(
            jsonResponse(
                """{"code":200,"data":{"name":"movie.mp4","size":4,"raw_url":"${server.url("d/movie.mp4")}"}}""",
            ),
        )
        server.enqueue(MockResponse().setResponseCode(206).setBody("data"))

        val client = OpenListClient(connection())
        assertTrue(client.connect().isSuccess)
        val files = client.listFiles("").getOrThrow()
        assertEquals("movie.mp4", files.single().name)
        assertEquals("data", client.openStream("movie.mp4", 1).bufferedReader().readText())

        val login = server.takeRequest()
        assertEquals("/api/auth/login", login.path)
        assertTrue(login.body.readUtf8().contains("\"username\":\"alice\""))

        val initialList = server.takeRequest()
        assertEquals("secret-token", initialList.getHeader("Authorization"))
        assertTrue(initialList.body.readUtf8().contains("\"password\":\"folder-secret\""))

        val explicitList = server.takeRequest()
        assertEquals("/api/fs/list", explicitList.path)
        val get = server.takeRequest()
        assertEquals("/api/fs/get", get.path)
        val download = server.takeRequest()
        assertEquals("bytes=1-", download.getHeader("Range"))
        assertEquals("secret-token", download.getHeader("Authorization"))
    }

    @Test
    fun `guest connection skips login`() = runTest {
        server.enqueue(listResponse())

        val client = OpenListClient(connection(username = "", password = ""))

        assertTrue(client.connect().isSuccess)
        assertEquals("/api/fs/list", server.takeRequest().path)
    }

    @Test
    fun `download does not send token to a different origin`() = runTest {
        val downloadServer = MockWebServer().apply { start() }
        try {
            server.enqueue(jsonResponse("""{"code":200,"data":{"token":"secret-token"}}"""))
            server.enqueue(listResponse())
            server.enqueue(
                jsonResponse(
                    """{"code":200,"data":{"size":4,"raw_url":"${downloadServer.url("movie.mp4")}"}}""",
                ),
            )
            downloadServer.enqueue(MockResponse().setBody("data"))

            val client = OpenListClient(connection())
            assertTrue(client.connect().isSuccess)
            assertEquals("data", client.openStream("movie.mp4").bufferedReader().readText())

            server.takeRequest()
            server.takeRequest()
            server.takeRequest()
            assertEquals(null, downloadServer.takeRequest().getHeader("Authorization"))
        } finally {
            downloadServer.shutdown()
        }
    }

    private fun connection(username: String = "alice", password: String = "password") = NetworkConnection(
        name = "OpenList",
        protocol = NetworkProtocol.OPENLIST,
        host = server.hostName,
        port = server.port,
        username = username,
        password = password,
        directoryPassword = "folder-secret",
    )

    private fun listResponse() = jsonResponse(
        """{"code":200,"data":{"content":[{"name":"movie.mp4","size":4,"is_dir":false,"modified":"2026-09-03T00:00:00Z"}]}}""",
    )

    private fun jsonResponse(body: String) = MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
