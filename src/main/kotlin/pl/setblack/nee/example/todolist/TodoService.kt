package pl.setblack.nee.example.todolist

import arrow.core.Either
import arrow.core.Option
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.core.some
import arrow.fx.coroutines.Atomic
import io.ktor.http.HttpStatusCode
import io.vavr.Tuple2
import io.vavr.collection.Map
import io.vavr.collection.Seq
import io.vavr.kotlin.hashMap
import java.time.Instant

// COROUTINE FAIL - cannot initialize state in constructor (unsafe used)
data class TodoService(
    val timeProvider: IO<Instant>,
    val state: Atomic<TodoState> = Atomic.unsafe(TodoState())
) {

    suspend fun addItem(title: String): TodoId =
        timeProvider().let { time ->
            state.modifyGet { s ->
                val item = TodoItem.Active(title, time)
                s.addItem(item)
            }.second
        }

    suspend fun markDone(id: TodoId): Either<TodoError, TodoItem> =
        state.modifyGet { s ->
            s.markDone(id)
        }.second

    suspend fun findAll(): Seq<Tuple2<TodoId, TodoItem>> = state.get().getAll().map { tuple -> tuple }

    suspend fun findItem(id: TodoId): Option<TodoItem> =
        state.get().getItem(id)
}

data class TodoState(
    private val nextId: TodoId = TodoId(1),
    private val items: Map<TodoId, TodoItem> = hashMap()
) {
    fun addItem(item: TodoItem): Pair<TodoState, TodoId> =
        nextId.let { id ->
            Pair(this.copy(nextId = id.next(), items = items.put(id, item)), id)
        }

    fun markDone(id: TodoId): Pair<TodoState, Either<TodoError, TodoItem>> =
        items[id].kt().toEither { TodoError.NotFound }
            .flatMap {
                when (it) {
                    is TodoItem.Active -> it.done().right()
                    else -> TodoError.InvalidState.left()
                }
            }.map {
                Pair(this.copy(items = items.put(id, it)), (it as TodoItem).right())
            }.getOrElse { Pair(this, TodoError.NotFound.left()) }

    fun getItem(id: TodoId): Option<TodoItem> = this.items[id].kt()

    fun getAll() = this.items
}

sealed class TodoError(val code: HttpStatusCode) {
    object NotFound : TodoError(HttpStatusCode.NotFound)
    object InvalidState : TodoError(HttpStatusCode.Conflict)
    object InvalidParam : TodoError(HttpStatusCode.BadRequest)
}

fun <T> io.vavr.control.Option<T>.kt(): Option<T> = this.map {
    it.some()
}.getOrElse(arrow.core.none())
