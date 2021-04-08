@file:Suppress("ReturnUnit")

package pl.setblack.nee.example.todolist.framework

import arrow.core.Either
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import pl.setblack.nee.example.todolist.IO

data class HttpError(val status: HttpStatusCode, val msg: String = "")

fun startNettyServer(
    port: Int,
    mapper: ObjectMapper,
    aRouting: RoutingDef
): IO<Unit> =
    suspend {
        embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(mapper))
            }
            routing {
                aRouting.r(this)
            }
        }.start(wait = true)
    }

data class RoutingDef(val r: (Route) -> Unit) {
    operator fun plus(other: RoutingDef) = RoutingDef { route ->
        r(route)
        other.r(route)
    }
}

inline fun <reified A : Any> aget(
    path: String = "",
    crossinline f: suspend (ApplicationCall) -> Either<HttpError, A>
) =
    RoutingDef { r: Route ->
        with(r) {
            get(path) {
                render(call, f(call))
            }
        }
    }

inline fun <reified A : Any> apost(
    path: String = "",
    crossinline f: suspend (ApplicationCall) -> Either<HttpError, A>
) =
    RoutingDef { r: Route ->
        with(r) {
            post(path) {
                render(call, f(call))
            }
        }
    }

inline fun <reified A : Any> adelete(
    path: String,
    crossinline f: suspend (ApplicationCall) -> Either<HttpError, A>
) =
    RoutingDef { r: Route ->
        with(r) {
            delete(path) {
                render(call, f(call))
            }
        }
    }

inline fun nested(path: String, crossinline nestedRoutes: () -> RoutingDef) = RoutingDef { r ->
    with(r) {
        route(path) {
            println(this)
            nestedRoutes().r(this)
        }
    }
}

suspend inline fun <reified A : Any> render(call: ApplicationCall, obj: Either<HttpError, A>) =
    obj.mapLeft {
        call.respond(it.status, it.msg)
    }.map { a: A ->
        call.respond(a)
    }
