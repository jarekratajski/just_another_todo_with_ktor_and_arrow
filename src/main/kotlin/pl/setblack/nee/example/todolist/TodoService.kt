package pl.setblack.nee.example.todolist

import io.vavr.collection.Map
import io.vavr.control.Option
import io.vavr.kotlin.hashMap
import arrow.fx.coroutines.*
import java.time.Instant

//COROUTINE FAIL - cannot initialize state in constructor
data class TodoService(
    val state: Atomic<TodoState>,
    val timeProvider: IO<Instant>) {

    suspend fun addItem(title: String): Pair<TodoState, TodoId> =
        timeProvider().let { time ->
            state.modifyGet { s ->
                val item = TodoItem.Active(title, time)
                s.addItem(item)
            }
        }

    suspend fun findAll() = state.get().getAll().map{tuple -> tuple}
}

data class TodoState(
    private val nextId: TodoId = TodoId(1),
    private val items: Map<TodoId, TodoItem> = hashMap()) {
    fun addItem(item: TodoItem): Pair<TodoState, TodoId> =
        nextId.next().let { id ->
            Pair(this.copy(nextId = id, items = items.put(id, item)), id)
        }


    fun getItem(id: TodoId): Option<TodoItem> = this.items[id]

    fun getAll() = this.items
}
