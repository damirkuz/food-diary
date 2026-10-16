package ru.kuzdikenov.fooddiary.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.context.annotation.Configuration
import jakarta.servlet.FilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.databind.json.JsonMapper
import java.time.OffsetDateTime

@ExtendWith(OutputCaptureExtension::class)
class StructuredLoggingTest {

    @Test
    fun `console uses one JSON object per line including exceptions`(output: CapturedOutput) {
        SpringApplicationBuilder(LoggingConfiguration::class.java)
            .web(WebApplicationType.NONE)
            .run().use {
                LoggerFactory.getLogger("logging-probe")
                    .atError()
                    .addKeyValue("event", "logging_probe")
                    .setCause(IllegalStateException("first line\nsecond line"))
                    .log("Example with \"quotes\" and\na newline")
                HttpRequestLoggingFilter().doFilter(
                    MockHttpServletRequest("GET", "/api/products"), MockHttpServletResponse(),
                    FilterChain { _, response -> (response as MockHttpServletResponse).status = 403 }
                )
            }

        val lines = output.out.lineSequence().filter { it.isNotBlank() }.toList()
        assertTrue(lines.isNotEmpty())
        val mapper = JsonMapper.builder().build()
        val records = lines.map { mapper.readTree(it) }
        records.forEach {
            OffsetDateTime.parse(it.path("timestamp").asString())
            assertFalse(it.path("level").asString().isBlank())
            assertFalse(it.path("message").asString().isBlank())
            assertEquals("food-diary", it.path("service.name").asString())
        }
        val probe = records.single { it.path("event").asString() == "logging_probe" }
        assertEquals("ERROR", probe.path("level").asString())
        assertEquals("Example with \"quotes\" and\na newline", probe.path("message").asString())
        assertTrue(probe.path("stack_trace").asString().contains("first line\nsecond line"))
        val request = records.single { it.path("event").asString() == "http_request_completed" }
        assertEquals("WARN", request.path("level").asString())
        assertEquals(403, request.path("http.status_code").asInt())
        assertTrue(request.path("duration_ms").isNumber)
        assertFalse(request.path("request_id").asString().isBlank())
        assertTrue(output.err.isBlank(), output.err)
    }

    @Configuration(proxyBeanMethods = false)
    class LoggingConfiguration
}
