package pl.setblack.nee.example.todolist

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.vavr.Tuple2
import io.vavr.collection.Seq
import java.time.LocalDateTime
import java.time.ZoneOffset

class TodoServerTest : StringSpec({
    val constTime = suspend { LocalDateTime.parse("2021-03-14T10:10:10").toInstant(ZoneOffset.UTC) }

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
            addItemUsingPOST("hello")
            with(handleRequest(HttpMethod.Get, "/todo/1")) {
                val item = Json.objectMapper.readValue(response.byteContent!!, TodoItem::class.java)
                item.title shouldBe "hello"
            }
        }
    }
    "added item should be in find all" {
        withTestApplication({
            TodoServer(constTime).definition(this)
        }) {
            addItemUsingPOST("hello")
            with(handleRequest(HttpMethod.Get, "/todo")) {
                val items = Json.objectMapper.readValue(response.byteContent!!,
                    object : TypeReference<Seq<Tuple2<TodoIdAlt, TodoItem>>>() {})
                items.size() shouldBe 1
                items[0]._2.title shouldBe ("hello")
            }
        }
    }
    "added three  items should be in find all" {
        withTestApplication({
            TodoServer(constTime).definition(this)
        }) {
            (0 until 3).forEach {
                addItemUsingPOST("hello_$it")
            }
            with(handleRequest(HttpMethod.Get, "/todo")) {
                val items = Json.objectMapper.readValue(response.byteContent!!,
                    object : TypeReference<Seq<Tuple2<TodoIdAlt, TodoItem>>>() {})
                items.size() shouldBe 3
                items.map { it._2.title } shouldContainAll ((0 until 3).map { "hello_$it" })
            }
        }
    }
    "done should mark as done"{
        withTestApplication({
            TodoServer(constTime).definition(this)
        }) {
            val id = addItemUsingPOST("hello")
            handleRequest(HttpMethod.Post, "/todo/done?id=$id")
            with(handleRequest(HttpMethod.Get, "/todo/${id}")) {
                val content= response.byteContent!!
                val item = Json.objectMapper.readValue(content, TodoItem::class.java)
                item.title shouldBe "hello"
                item.shouldBeTypeOf<TodoItem.Done>()
            }
        }
    }

    "done twice on same item should lead to 409"{
        withTestApplication({
            TodoServer(constTime).definition(this)
        }) {
            val id = addItemUsingPOST("hello")
            handleRequest(HttpMethod.Post, "/todo/done?id=$id")
            with(handleRequest(HttpMethod.Post, "/todo/done?id=$id")) {
               response.status().shouldBe(HttpStatusCode.Conflict)
            }
        }
    }
})

private fun TestApplicationEngine.addItemUsingPOST(title: String): Int =
    with(handleRequest(HttpMethod.Post, "/todo?title=$title")) {
        response.content!!.toInt()
    }

data class TodoIdAlt(val id: Int)

