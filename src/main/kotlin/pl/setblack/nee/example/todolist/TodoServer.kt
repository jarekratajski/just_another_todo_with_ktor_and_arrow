package pl.setblack.nee.example.todolist

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.response.respond
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import pl.setblack.nee.example.todolist.impure.JsonMapper
import pl.setblack.nee.example.todolist.impure.adelete
import pl.setblack.nee.example.todolist.impure.aget
import pl.setblack.nee.example.todolist.impure.apost
import pl.setblack.nee.example.todolist.impure.nested
import java.time.Instant

typealias IO<A> = suspend () -> A

class TodoServer(val timeProvider: IO<Instant>) {

    fun startServer() =
        embeddedServer(Netty, port = 8000) {
            definition()
        }.start(wait = true)

    val definition: Application.() -> Unit = {
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(JsonMapper.objectMapper))
        }
        routing()
    }

    val routing: Application.() -> Unit = {

        val service = TodoService(timeProvider)

        routing {
            nested("/todo") {
                aget("{id}") {
                    it.parameters["id"].asId().flatMap { id ->
                        service.findItem(TodoId(id)).toEither { TodoError.NotFound }
                    }.mapLeft { it.toHttpError() }
                } + adelete("{id}") {
                    it.parameters["id"].asId().flatMap { id ->
                        service.cancellItem(TodoId(id))
                    }.mapLeft { it.toHttpError() }
                } + nested("/done") {
                    aget {
                        service.findDone().right()
                    } + apost {
                        it.parameters["id"].asId().flatMap { id ->
                            service.markDone(TodoId(id))
                        }.mapLeft { it.toHttpError() }
                    }
                }
            }.r(this)
            route("/otodo") {

                route("/done") {

                    post {
                        render(call,
                            call.parameters["id"].asId().flatMap { id ->
                                service.markDone(TodoId(id))
                            }
                        )
                    }
                }

                route("/cancelled") {
                    get {
                        call.respond(service.findCancelled())
                    }
                }

                post {
                    call.parameters["title"]?.let { itemTitle ->
                        val result = service.addItem(itemTitle)
                        call.respond(result.id.toString())
                    } ?: call.respond(HttpStatusCode.BadRequest, "no title given")
                }

                get {
                    call.respond(service.findActive())
                }
            }
        }
    }
}

fun main() {
    val time = suspend { Instant.now() }
    TodoServer(time).startServer()
}

suspend inline fun <reified A : Any> render(call: ApplicationCall, obj: Either<TodoError, A>) =
    obj.mapLeft {
        call.respond(it.code, "nok")
    }.map { a: A ->
        call.respond(a)
    }

fun String?.asId(): Either<TodoError, Int> =
    if (this != null && this.matches(Regex("-?[0-9]+"))) {
        this.toInt().right()
    } else {
        TodoError.InvalidParam.left()
    }
