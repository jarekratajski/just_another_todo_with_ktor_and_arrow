package pl.setblack.nee.example.todolist

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.ktor.http.HttpStatusCode
import pl.setblack.nee.example.todolist.impure.HttpError
import pl.setblack.nee.example.todolist.impure.JsonMapper
import pl.setblack.nee.example.todolist.impure.adelete
import pl.setblack.nee.example.todolist.impure.aget
import pl.setblack.nee.example.todolist.impure.apost
import pl.setblack.nee.example.todolist.impure.nested
import pl.setblack.nee.example.todolist.impure.startNettyServer
import java.time.Instant

typealias IO<A> = suspend () -> A

object TodoServer {

    fun defineRouting(timeProvider: IO<Instant>) = run {
        val service = TodoService(timeProvider)
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
        }
    }
}

const val REST_LISTENING_PORT = 8000

fun main() {
    val time = suspend { Instant.now() }
    startNettyServer(
        REST_LISTENING_PORT,
        JsonMapper.objectMapper,
        TodoServer.defineRouting(time)
    )
}

fun String?.asId(): Either<TodoError, Int> =
    if (this != null && this.matches(Regex("-?[0-9]+"))) {
        this.toInt().right()
    } else {
        TodoError.InvalidParam.left()
    }
