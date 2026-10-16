package ru.kuzdikenov.fooddiary.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerMapping
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class HttpRequestLoggingFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val previousRequestId = MDC.get("request_id")
        MDC.put("request_id", UUID.randomUUID().toString())
        val startedAt = System.nanoTime()
        var failed = false
        try {
            filterChain.doFilter(request, response)
        } catch (ex: Exception) {
            failed = true
            throw ex
        } finally {
            try {
                val status = if (failed) 500 else response.status
                // Kubernetes probes run constantly; retain only unsuccessful ones.
                val healthPath = request.contextPath + "/actuator/health"
                val healthProbe = request.requestURI == healthPath || request.requestURI.startsWith("$healthPath/")
                if (!healthProbe || status >= 400) {
                    val event = when {
                        status >= 500 -> requestLogger.atError()
                        status >= 400 -> requestLogger.atWarn()
                        else -> requestLogger.atInfo()
                    }
                    event
                        .addKeyValue("event", "http_request_completed")
                        .addKeyValue("http.method", request.method)
                        .addKeyValue("http.route", request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
                            ?: request.requestURI)
                        .addKeyValue("http.status_code", status)
                        .addKeyValue("duration_ms", (System.nanoTime() - startedAt) / 1_000_000.0)
                        .log("HTTP request completed")
                }
            } finally {
                if (previousRequestId == null) MDC.remove("request_id") else MDC.put("request_id", previousRequestId)
            }
        }
    }

    companion object {
        private val requestLogger = LoggerFactory.getLogger(HttpRequestLoggingFilter::class.java)
    }
}
