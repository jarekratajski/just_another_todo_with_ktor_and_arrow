package pl.setblack.nee.example.todolist

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.vavr.jackson.datatype.VavrModule
import java.time.LocalDateTime
import java.time.ZoneOffset

class TodoServerTest : StringSpec({
    val constTime = suspend { LocalDateTime.parse("2021-03-14T10:10:10").toInstant(ZoneOffset.UTC)}

    "add item should give an id" {
        withTestApplication({
            TodoServer(constTime).definition(this)
        }) {
            with(handleRequest(HttpMethod.Post, "/todo?title=hello")) {
                response.content shouldBe ("1")
            }
        }
    }
    "added item should be returned" {
        withTestApplication({
            TodoServer(constTime).definition(this)
        }) {
            handleRequest(HttpMethod.Post, "/todo?title=hello")
            with(handleRequest (HttpMethod.Get, "/todo/1" ) ) {
                val item = Json.objectMapper.readValue<TodoItem>(response.byteContent!!, TodoItem::class.java)
                item.title shouldBe "hello"
            }
        }
    }
})

object Json  {
    val objectMapper = ObjectMapper().apply {
        registerModule(VavrModule())
        registerModule(JavaTimeModule())
        registerModule(KotlinModule())
        registerModule(ParameterNamesModule())
    }
}
