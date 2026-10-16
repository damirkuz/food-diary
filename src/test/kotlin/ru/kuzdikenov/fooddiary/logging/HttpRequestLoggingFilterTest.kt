package ru.kuzdikenov.fooddiary.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.servlet.HandlerMapping

class HttpRequestLoggingFilterTest {
    private val logger = LoggerFactory.getLogger(HttpRequestLoggingFilter::class.java) as Logger
    private val appender = ListAppender<ILoggingEvent>().apply { start() }
    private val previousLevel = logger.level
    private val filter = HttpRequestLoggingFilter()

    init {
        logger.level = Level.INFO
        logger.addAppender(appender)
    }

    @AfterEach
    fun cleanup() {
        logger.detachAppender(appender)
        logger.level = previousLevel
        appender.stop()
        MDC.clear()
    }

    @Test
    fun `request logs structured fields without query headers or body and restores MDC`() {
        MDC.put("request_id", "previous")
        val request = MockHttpServletRequest("POST", "/api/products/42").apply {
            queryString = "token=secret-query"
            addHeader("Authorization", "Bearer secret-header")
            setContent("password=secret-body".toByteArray())
        }
        var requestId: String? = null
        filter.doFilter(request, MockHttpServletResponse(), FilterChain { _, response ->
            requestId = MDC.get("request_id")
            request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/products/{id}")
            (response as MockHttpServletResponse).status = 201
        })

        val event = appender.list.single()
        val fields = event.keyValuePairs.associate { it.key to it.value }
        assertEquals(Level.INFO, event.level)
        assertEquals("http_request_completed", fields["event"])
        assertEquals("POST", fields["http.method"])
        assertEquals("/api/products/{id}", fields["http.route"])
        assertEquals(201, fields["http.status_code"])
        assertTrue((fields["duration_ms"] as Double) >= 0)
        assertNotNull(requestId)
        assertNotEquals("previous", requestId)
        assertEquals(requestId, event.mdcPropertyMap["request_id"])
        assertEquals("previous", MDC.get("request_id"))
        assertFalse((fields.toString() + event.formattedMessage).contains("secret"))
    }

    @Test
    fun `4xx is WARN and 5xx is ERROR including requests rejected before MVC`() {
        for ((status, level) in listOf(401 to Level.WARN, 404 to Level.WARN, 503 to Level.ERROR)) {
            filter.doFilter(MockHttpServletRequest("GET", "/api/products"), MockHttpServletResponse(),
                FilterChain { _, response -> (response as MockHttpServletResponse).status = status })
            assertEquals(level, appender.list.last().level)
            assertEquals(status, appender.list.last().keyValuePairs.single { it.key == "http.status_code" }.value)
        }
        assertNull(MDC.get("request_id"))
    }

    @Test
    fun `propagated exception logs 500 and cleans MDC without swallowing exception`() {
        val failure = IllegalStateException("failure")
        val thrown = assertThrows(IllegalStateException::class.java) {
            filter.doFilter(MockHttpServletRequest("GET", "/api/products"), MockHttpServletResponse(),
                FilterChain { _, _ -> throw failure })
        }
        assertSame(failure, thrown)
        assertEquals(Level.ERROR, appender.list.single().level)
        assertEquals(500, appender.list.single().keyValuePairs.single { it.key == "http.status_code" }.value)
        assertNull(MDC.get("request_id"))
    }

    @Test
    fun `healthy probes are quiet but failed probes are logged`() {
        for (status in listOf(200, 503)) {
            filter.doFilter(MockHttpServletRequest("GET", "/actuator/health/readiness"), MockHttpServletResponse(),
                FilterChain { _, response -> (response as MockHttpServletResponse).status = status })
        }
        assertEquals(Level.ERROR, appender.list.single().level)
    }
}
