package pl.setblack.nee.example.todolist

import io.ktor.http.HttpStatusCode
import pl.setblack.nee.example.todolist.impure.HttpError

sealed class TodoError(val code: HttpStatusCode) {
    fun toHttpError() = HttpError(this.code, this.toString())
    object NotFound : TodoError(HttpStatusCode.NotFound)
    object InvalidState : TodoError(HttpStatusCode.Conflict)
    object InvalidParam : TodoError(HttpStatusCode.BadRequest)
}
