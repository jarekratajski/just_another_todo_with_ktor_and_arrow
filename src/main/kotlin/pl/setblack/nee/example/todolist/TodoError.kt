package pl.setblack.nee.example.todolist

import io.ktor.http.HttpStatusCode

sealed class TodoError(val code: HttpStatusCode) {
    object NotFound : TodoError(HttpStatusCode.NotFound)
    object InvalidState : TodoError(HttpStatusCode.Conflict)
    object InvalidParam : TodoError(HttpStatusCode.BadRequest)
}
