import org.junit.jupiter.api.Assertions.*

import io.ktor.server.netty.*
import io.ktor.routing.*
import io.ktor.application.*
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.server.engine.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime.*

private const val MESSAGE = "Hello, world!"

fun getServer(): NettyApplicationEngine {
    return embeddedServer(Netty, 8080) {
        routing {
            get("/") {
                call.respondText(MESSAGE, ContentType.Text.Html)
            }
        }
    }
}

@ExperimentalCoroutinesApi
class KtorClientTest {
    private val clientThreads = newSingleThreadContext("client")
    private val syncJob = SupervisorJob()
    private lateinit var actualStatusCode: HttpStatusCode
    private lateinit var actualMessage: String


    @Test
    fun `connect from within onStarted`() = runBlocking {
        val serverThreads = newSingleThreadContext("server")
        val server = getServer()
        server.environment.monitor.subscribe(ApplicationStarted, ::onStarted)

        try {
            launch(serverThreads) {
                System.err.println("[${now()}] Before server.start() in launch block")
                server.start(wait = true)
                System.err.println("[${now()}] after server.start() in launch block")
            }
            System.err.println("[${now()}] Before call to getData")
            getData()
            System.err.println("[${now()}] After call to getData")
        } finally {
            System.err.println("[${now()}] Calling server.stop() in finally block...")
            server.stop(1000, 20000)
        }
        assertEquals(HttpStatusCode.OK, actualStatusCode)
        assertEquals(MESSAGE, actualMessage)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onStarted(application: Application) {
        System.err.println("[${now()}] ******** onStarted ********")
        syncJob.complete()
    }
    private suspend fun getData() = withContext(clientThreads) {
        launch(clientThreads) {
            System.err.println("[${now()}] Waiting for start")
            syncJob.join()
            System.err.println("[${now()}] Started")
            val client = HttpClient(CIO)
            delay(150)
            System.err.println("[${now()}] Before client.get()")
            try {
                val response: HttpResponse = client.get("http://localhost:8080/")
                System.err.println("[${now()}] After client.get()")
                actualStatusCode = response.status
                actualMessage = response.receive()
            } catch (e: Throwable) {
                System.err.println(e.printStackTrace())
            } finally {
                client.close()
            }
        }
    }
}