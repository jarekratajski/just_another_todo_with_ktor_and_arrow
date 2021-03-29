package pl.setblack.nee.example.todolist

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication

class HelloTest : StringSpec({
    "should do calc" {
        withTestApplication({
            HelloServer.helloRouting(this)

        }) {
            with(handleRequest(HttpMethod.Get, "/hello")) {
                response.content shouldBe ("hello world")
            }
        }

    }
})
