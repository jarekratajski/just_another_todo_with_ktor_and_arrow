package pl.setblack.nee.example.todolist

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.vavr.Tuple2
import io.vavr.collection.Seq
import pl.setblack.nee.example.todolist.framework.JsonMapper
import java.time.LocalDateTime
import java.time.ZoneOffset

class TodoServerTest : StringSpec({

    "add item should give an id" {
        withTestApplication({
            todoRest()
        }) {
            with(handleRequest(HttpMethod.Post, "/todo?title=hello")) {
                response.content shouldBe ("1")
            }
        }
    }
    "added item should be returned by id" {
        withTestApplication({
            todoRest()
        }) {
            addItemUsingPOST("hello")
            with(handleRequest(HttpMethod.Get, "/todo/1")) {
                val item = JsonMapper.objectMapper.readValue(response.byteContent!!, TodoItem::class.java)
                item.title shouldBe "hello"
            }
        }
    }
    "calling get with text id returns error" {
        withTestApplication({
            todoRest()
        }) {
            addItemUsingPOST("hello")
            with(handleRequest(HttpMethod.Get, "/todo/zupka")) {
                response.status() shouldBe HttpStatusCode.BadRequest
            }
        }
    }
    "added item should be in find all" {
        withTestApplication({
            todoRest()
        }) {
            addItemUsingPOST("hello")
            with(handleRequest(HttpMethod.Get, "/todo")) {
                val items = JsonMapper.objectMapper.readValue(response.byteContent!!,
                    object : TypeReference<Seq<Tuple2<TodoIdAlt, TodoItem>>>() {})
                items.size() shouldBe 1
                items[0]._2.title shouldBe ("hello")
            }
        }
    }
    "added three  items should be in find all" {
        withTestApplication({
            todoRest()
        }) {
            (0 until 3).forEach {
                addItemUsingPOST("hello_$it")
            }
            with(handleRequest(HttpMethod.Get, "/todo")) {
                val items = JsonMapper.objectMapper.readValue(response.byteContent!!,
                    object : TypeReference<Seq<Tuple2<TodoIdAlt, TodoItem>>>() {})
                items.size() shouldBe 3
                items.map { it._2.title } shouldContainAll ((0 until 3).map { "hello_$it" })
            }
        }
    }
    "done should mark as done"{
        withTestApplication({
            todoRest()
        }) {
            val id = addItemUsingPOST("hello")
            handleRequest(HttpMethod.Post, "/todo/done?id=$id")
            with(handleRequest(HttpMethod.Get, "/todo/${id}")) {
                val content = response.byteContent!!
                val item = JsonMapper.objectMapper.readValue(content, TodoItem::class.java)
                item.title shouldBe "hello"
                item.shouldBeTypeOf<TodoItem.Done>()
            }
        }
    }

    "done twice on same item should lead to 409"{
        withTestApplication({
            todoRest()
        }) {
            val id = addItemUsingPOST("hello")
            handleRequest(HttpMethod.Post, "/todo/done?id=$id")
            with(handleRequest(HttpMethod.Post, "/todo/done?id=$id")) {
                response.status().shouldBe(HttpStatusCode.Conflict)
            }
        }
    }
    "done should not be on todo list" {
        withTestApplication({
            todoRest()
        }) {
            val id = addItemUsingPOST("hello")
            handleRequest(HttpMethod.Post, "/todo/done?id=$id")
            with(handleRequest(HttpMethod.Get, "/todo")) {
                val items = JsonMapper.objectMapper.readValue(response.byteContent!!,
                    object : TypeReference<Seq<Tuple2<TodoIdAlt, TodoItem>>>() {})
                items.size() shouldBe 0
            }
        }
    }
    "done should  be on done list" {
        withTestApplication({
            todoRest()
        }) {
            val id = addItemUsingPOST("hello")
            handleRequest(HttpMethod.Post, "/todo/done?id=$id")
            with(handleRequest(HttpMethod.Get, "/todo/done")) {
                val items = JsonMapper.objectMapper.readValue(response.byteContent!!,
                    object : TypeReference<Seq<Tuple2<TodoIdAlt, TodoItem>>>() {})
                items.size() shouldBe 1
            }
        }
    }

    "cancelled should not be on todo list" {
        withTestApplication({
            todoRest()
        }) {
            val id = addItemUsingPOST("hello")
            handleRequest(HttpMethod.Delete, "/todo/$id")
            with(handleRequest(HttpMethod.Get, "/todo")) {
                val items = JsonMapper.objectMapper.readValue(response.byteContent!!,
                    object : TypeReference<Seq<Tuple2<TodoIdAlt, TodoItem>>>() {})
                items.size() shouldBe 0
            }
        }
    }
    "cancelled should be on cancelled list" {
        withTestApplication({
            todoRest()
        }) {
            val id = addItemUsingPOST("hello")
            handleRequest(HttpMethod.Delete, "/todo/$id")
            with(handleRequest(HttpMethod.Get, "/todo/cancelled")) {
                val items = JsonMapper.objectMapper.readValue(response.byteContent!!,
                    object : TypeReference<Seq<Tuple2<TodoIdAlt, TodoItem>>>() {})
                items.size() shouldBe 1
            }
        }
    }
}) {
    companion object {
        val constTime = suspend { LocalDateTime.parse("2021-03-14T10:10:10").toInstant(ZoneOffset.UTC) }

        val todoRest: Application.() -> Unit = {
            install(ContentNegotiation) {
                register(io.ktor.http.ContentType.Application.Json, JacksonConverter(JsonMapper.objectMapper))
            }
            routing {
                TodoServer.defineRouting(constTime).r(this)
            }
        }
    }
}

private fun TestApplicationEngine.addItemUsingPOST(title: String): Int =
    with(handleRequest(HttpMethod.Post, "/todo?title=$title")) {
        response.content!!.toInt()
    }

data class TodoIdAlt(val id: Int)
