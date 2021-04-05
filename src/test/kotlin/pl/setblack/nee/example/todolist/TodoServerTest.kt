package pl.setblack.nee.example.todolist

import com.fasterxml.jackson.core.type.TypeReference
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
import io.vavr.Tuple2
import io.vavr.collection.Seq
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
    "added item should be returned by id" {
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
    "added item should be in find all" {
        withTestApplication({
            TodoServer(constTime).definition(this)
        }) {
            handleRequest(HttpMethod.Post, "/todo?title=hello")
            with(handleRequest (HttpMethod.Get, "/todo" ) ) {
                val items = Json.objectMapper.readValue(response.byteContent!!,
                    object : TypeReference<Seq<Tuple2<TodoIdAlt, TodoItem>>>(){})
                items.size() shouldBe 1
                items[0]._2.title shouldBe ("hello")
            }
        }
    }
})

data class TodoIdAlt(val id:Int)

