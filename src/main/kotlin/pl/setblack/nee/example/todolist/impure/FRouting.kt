@file:Suppress("ReturnUnit")

package pl.setblack.nee.example.todolist.impure

import arrow.core.Either
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route

data class HttpError(val status: HttpStatusCode, val msg: String = "")

typealias F<A> = suspend (ApplicationCall) -> Either<HttpError, A>

data class FR(val r: (Route) -> Unit) {
    operator fun plus(other: FR) = FR { route ->
        r(route)
        other.r(route)
    }
}

inline fun <reified A : Any> aget(
    path: String = "",
    crossinline f: suspend (ApplicationCall) -> Either<HttpError, A>
) =
    FR { r: Route ->
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
    FR { r: Route ->
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
    FR { r: Route ->
        with(r) {
            delete(path) {
                render(call, f(call))
            }
        }
    }

inline fun nested(path: String, crossinline nestedRoutes: () -> FR) = FR { r ->
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
