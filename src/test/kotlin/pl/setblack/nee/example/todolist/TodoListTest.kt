package pl.setblack.nee.example.todolist

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import java.time.LocalDateTime
import java.time.ZoneOffset

class TodoListTest : StringSpec({

    "add item should give an id" {
        val constTime = suspend { LocalDateTime.parse("2021-03-14T10:10:10").toInstant(ZoneOffset.UTC)}
        withTestApplication({
            TodoServer(constTime).routing(this)

        }) {
            with(handleRequest(HttpMethod.Post, "/todo")) {
                response.content shouldBe ("hello world")
            }
        }

    }
})
