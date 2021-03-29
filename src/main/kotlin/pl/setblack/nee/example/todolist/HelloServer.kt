package pl.setblack.nee.example.todolist

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

@Suppress("ReturnUnit")
object HelloServer {

    fun startServer() =
        embeddedServer(Netty, port = 8000) {
            helloRouting()
        }.start(wait = true)

    private fun Application.helloRouting() {
        routing {
            route("/hello") {
                get {
                    call.respond("hello world")
                }
            }
        }
    }
}

fun main() {
    HelloServer.startServer()
}
