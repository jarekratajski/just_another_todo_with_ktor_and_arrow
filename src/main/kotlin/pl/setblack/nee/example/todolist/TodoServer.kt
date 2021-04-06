package pl.setblack.nee.example.todolist

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
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
import io.vavr.jackson.datatype.VavrModule
import java.time.Instant

typealias IO<A> = suspend () -> A

@Suppress("ReturnUnit")
class TodoServer(val timeProvider: IO<Instant>) {

    fun startServer() =
        embeddedServer(Netty, port = 8000) {
            definition()
        }.start(wait = true)

    val definition: Application.() -> Unit = {
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(Json.objectMapper))
        }
        routing()
    }

    val routing: Application.() -> Unit = {

        val service = TodoService(timeProvider)

        routing {
            route("/todo") {
                get("{id}") {
                    render(call, call.parameters["id"].asId().flatMap { id ->
                        service.findItem(TodoId(id)).toEither { TodoError.NotFound }
                    })
                }
                delete("{id}") {
                    render(call, call.parameters["id"].asId().flatMap { id ->
                        service.cancellItem(TodoId(id))
                    })
                }

                route("/done") {
                    get {
                        call.respond(service.findDone())
                    }
                    post {
                        val id = call.parameters["id"]
                        val done = service.markDone(TodoId(id!!.toInt())) // TODO toInt
                        render(call, done)
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

object Json {
    val objectMapper = ObjectMapper().apply {
        enable(SerializationFeature.INDENT_OUTPUT)
        registerModule(VavrModule())
        registerModule(JavaTimeModule())
        registerModule(KotlinModule())
        registerModule(ParameterNamesModule())
    }
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
