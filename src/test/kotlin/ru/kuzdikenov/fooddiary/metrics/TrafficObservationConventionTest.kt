package ru.kuzdikenov.fooddiary.metrics

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.server.observation.ServerRequestObservationContext
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class TrafficObservationConventionTest {
    @AfterEach
    fun clearSecurityContext() = SecurityContextHolder.clearContext()

    @Test
    fun `authenticated technical account is synthetic without exposing its name`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken.authenticated("probe@test.local", "unused", emptyList())
        assertEquals("synthetic", traffic())
    }

    @Test
    fun `regular authenticated user remains user`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken.authenticated("user@test.local", "unused", emptyList())
        assertEquals("user", traffic())
    }

    @Test
    fun `spoofed client header cannot classify anonymous request as synthetic`() {
        assertEquals("user", traffic())
    }

    @Test
    fun `unauthenticated technical username is not trusted`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken.unauthenticated("probe@test.local", "unused")
        assertEquals("user", traffic())
    }

    @Test
    fun `API failure before handler mapping remains in API scope`() {
        val request = MockHttpServletRequest("GET", "/api/diary")
        val response = MockHttpServletResponse().apply { status = 500 }
        val tags = TrafficObservationConvention()
            .getLowCardinalityKeyValues(ServerRequestObservationContext(request, response))
        assertEquals("api", tags.firstOrNull { it.key == "http_scope" }?.value)
    }

    private fun traffic(): String {
        val request = MockHttpServletRequest("GET", "/api/diary")
        request.addHeader("X-Synthetic", "true")
        val response = MockHttpServletResponse()
        TrafficClassificationFilter(setOf("probe@test.local"))
            .doFilter(request, response, FilterChain { _, _ -> })
        val context = ServerRequestObservationContext(request, response)
        // SecurityContext is cleared before the outer HTTP observation stops.
        SecurityContextHolder.clearContext()
        return TrafficObservationConvention().getLowCardinalityKeyValues(context)
            .first { it.key == "traffic" }.value
    }
}
