package pl.setblack.nee.example.todolist

import arrow.fx.coroutines.Atomic
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.vavr.jackson.datatype.VavrModule
import java.time.Instant

typealias IO<A> = suspend ()->A

@Suppress("ReturnUnit")
class TodoServer(val timeProvider: IO<Instant>) {

    fun startServer() =
        embeddedServer(Netty, port = 8000) {
            install(ContentNegotiation) {
                jackson {
                    enable(SerializationFeature.INDENT_OUTPUT)
                    registerModule(VavrModule())
                     registerModule(JavaTimeModule())
                }
            }
            routing()
        }.start(wait = true)



    val routing: Application.() -> Unit = {

        val service =  TodoService(Atomic.unsafe(TodoState()), timeProvider)

            routing {
            route("/todo") {
                get("{id}") {
                    val id = call.parameters["id"]
                    call.respond("asked $id")
                }

                post {
                    call.parameters["title"]?.let { itemTitle->
                        val result = service.addItem(itemTitle)
                        call.respond("posted ${result.second}")
                    } ?: call.respond(HttpStatusCode.BadRequest, "no title given")
                }

                get {
                    call.respond(service.findAll())
                }
            }
        }
    }
}

fun main() {
    val time = suspend { Instant.now() }
    TodoServer(time).startServer()
}
