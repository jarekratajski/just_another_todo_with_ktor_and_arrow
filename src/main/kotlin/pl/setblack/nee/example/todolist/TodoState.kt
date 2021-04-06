package pl.setblack.nee.example.todolist

import arrow.core.Either
import arrow.core.Option
import arrow.core.flatMap
import arrow.core.left
import arrow.core.merge
import arrow.core.right
import io.vavr.collection.Map
import io.vavr.kotlin.hashMap

data class TodoState(
    private val nextId: TodoId = TodoId(1),
    private val items: Map<TodoId, TodoItem> = hashMap()
) {
    fun addItem(item: TodoItem): Pair<TodoState, TodoId> =
        nextId.let { id ->
            Pair(this.copy(nextId = id.next(), items = items.put(id, item)), id)
        }

    fun markDone(id: TodoId): Pair<TodoState, Either<TodoError, TodoItem>> = changeItem(id) { item ->
        when (item) {
            is TodoItem.Active -> item.done().right()
            else -> TodoError.InvalidState.left()
        }
    }

    fun markCancelled(id: TodoId): Pair<TodoState, Either<TodoError, TodoItem>> = changeItem(id) { item ->
        when (item) {
            is TodoItem.Active -> item.cancel().right()
            else -> TodoError.InvalidState.left()
        }
    }

    fun getItem(id: TodoId): Option<TodoItem> = this.items[id].kt()

    fun getAll() = this.items

    private fun changeItem(id: TodoId, change: (TodoItem) -> Either<TodoError, TodoItem>):
            Pair<TodoState, Either<TodoError, TodoItem>> =
        items[id].kt().toEither { TodoError.NotFound }
            .flatMap {
                change(it)
            }.map {
                Pair(this.copy(items = items.put(id, it)), it.right())
            }.mapLeft {
                Pair(this, it.left())
            }.merge()
}
