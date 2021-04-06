package pl.setblack.nee.example.todolist.impure

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import io.vavr.jackson.datatype.VavrModule

object JsonMapper {
    val objectMapper = ObjectMapper().apply {
        enable(SerializationFeature.INDENT_OUTPUT)
        registerModule(VavrModule())
        registerModule(JavaTimeModule())
        registerModule(KotlinModule())
        registerModule(ParameterNamesModule())
    }
}
