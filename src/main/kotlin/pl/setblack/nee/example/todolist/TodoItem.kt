package pl.setblack.nee.example.todolist

import java.time.Instant

inline class TodoId(val id:Int) {
    fun next() =
        TodoId(id+1)
}

sealed class TodoItem(val title: String, val created: Instant) {
    class Active(title: String, created: Instant) : TodoItem(title, created)
    class Done(todoItem: TodoItem) : TodoItem(todoItem.title, todoItem.created)
    class Cancelled(todoItem: TodoItem) : TodoItem(todoItem.title, todoItem.created)
}
