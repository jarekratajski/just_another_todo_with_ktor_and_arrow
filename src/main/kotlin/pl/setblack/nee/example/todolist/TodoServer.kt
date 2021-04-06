package pl.setblack.nee.example.todolist

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import pl.setblack.nee.example.todolist.impure.HttpError
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
                } + nested("/cancelled") {
                    aget {
                        service.findCancelled().right()
                    }
                } + apost {
                    it.parameters["title"]?.let { itemTitle ->
                        val result = service.addItem(itemTitle)
                        result.id.toString().right()
                    } ?: HttpError(HttpStatusCode.BadRequest, "no title given").left()
                } + aget {
                    service.findActive().right()
                }
            }.r(this)
        }
    }
}

fun main() {
    val time = suspend { Instant.now() }
    TodoServer(time).startServer()
}

fun String?.asId(): Either<TodoError, Int> =
    if (this != null && this.matches(Regex("-?[0-9]+"))) {
        this.toInt().right()
    } else {
        TodoError.InvalidParam.left()
    }
