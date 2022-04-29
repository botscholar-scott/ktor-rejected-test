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
import java.time.LocalDateTime
import java.time.LocalDateTime.*

fun getServer(): NettyApplicationEngine {
    return embeddedServer(Netty, 8080) {
        routing {
            get("/") {
                call.respondText("Hello, world!", ContentType.Text.Html)
            }
        }
    }
}

class KtorClientTest {

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
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
            System.err.println("[${now()}] Before server launch delay()")
            delay(60000)
            System.err.println("[${now()}] After server launch delay()")
        } finally {
            System.err.println("[${now()}] Calling server.stop() in finally block...")
            server.stop(1000, 20000)
        }
    }

    private fun onStarted(application: Application) {
        System.err.println("[${now()}] ******** onStarted ********")
        runBlocking {
            val client = HttpClient(CIO)
            delay(10000)
            System.err.println("[${now()}] Before client.get()")
            try {
                val response: HttpResponse = client.get("http://localhost:8080/")
                System.err.println("[${now()}] After client.get() ${response.status}")
                val actual: String = response.receive()
                System.err.println("actual? ${actual}")
            } catch (e: Throwable) {
                System.err.println(e.printStackTrace())
            }
            finally {
                client.close()
            }
        }
    }
}