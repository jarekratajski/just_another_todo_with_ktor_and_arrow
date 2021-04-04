package pl.setblack.nee.example.todolist

import arrow.fx.coroutines.Atomic
import io.vavr.collection.Map
import io.vavr.control.Option
import io.vavr.kotlin.hashMap
import java.time.Instant

// COROUTINE FAIL - cannot initialize state in constructor (unsafe used)
data class TodoService(
    val timeProvider: IO<Instant>,
    val state: Atomic<TodoState> = Atomic.unsafe(TodoState())
) {

    suspend fun addItem(title: String): Pair<TodoState, TodoId> =
        timeProvider().let { time ->
            state.modifyGet { s ->
                val item = TodoItem.Active(title, time)
                s.addItem(item)
            }
        }

    suspend fun findAll() = state.get().getAll().map { tuple -> tuple }

    suspend fun findItem(id: TodoId): Option<TodoItem> =
        state.get().getItem(id)
}

data class TodoState(
    private val nextId: TodoId = TodoId(1),
    private val items: Map<TodoId, TodoItem> = hashMap()
) {
    fun addItem(item: TodoItem): Pair<TodoState, TodoId> =
        nextId.let { id ->
            Pair(this.copy(nextId = id, items = items.put(id, item)), id)
        }

    fun getItem(id: TodoId): Option<TodoItem> = this.items[id]

    fun getAll() = this.items
}
