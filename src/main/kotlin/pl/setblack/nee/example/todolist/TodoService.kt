package pl.setblack.nee.example.todolist

import arrow.core.Either
import arrow.core.Option
import arrow.core.some
import arrow.fx.coroutines.Atomic
import io.vavr.Tuple2
import io.vavr.collection.Seq
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
            val res = s.markDone(id)
            println(res)
            res
        }.second

    suspend fun findActive(): Seq<Tuple2<TodoId, TodoItem>> = state.get().getAll()
        .filter { tuple ->
            tuple._2.isActive()
        }.map { tuple -> tuple }

    suspend fun findItem(id: TodoId): Option<TodoItem> =
        state.get().getItem(id)

    suspend fun cancellItem(id: TodoId) = state.modifyGet { s ->
        s.markCancelled(id)
    }.second

    suspend fun findCancelled() = state.get().getAll()
        .filter { tuple ->
            tuple._2 is TodoItem.Cancelled
        }.map { tuple -> tuple }

    suspend fun findDone() = state.get().getAll()
        .filter { tuple ->
            tuple._2 is TodoItem.Done
        }.map { tuple -> tuple }
}

fun <T> io.vavr.control.Option<T>.kt(): Option<T> = this.map {
    it.some()
}.getOrElse(arrow.core.none())
