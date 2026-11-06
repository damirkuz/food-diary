package ru.kuzdikenov.fooddiary.metrics

import io.micrometer.common.KeyValue
import io.micrometer.common.KeyValues
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention
import org.springframework.http.server.observation.ServerRequestObservationContext
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.Locale

private const val TRAFFIC_ATTRIBUTE = "food-diary.observability.traffic"

/** Preserve default HTTP labels; add bounded source and API scope classes. */
@Component
class TrafficObservationConvention : DefaultServerRequestObservationConvention() {
    override fun getLowCardinalityKeyValues(context: ServerRequestObservationContext): KeyValues =
        super.getLowCardinalityKeyValues(context).and(
            KeyValue.of("traffic", context.carrier?.getAttribute(TRAFFIC_ATTRIBUTE) as? String ?: "user"),
            KeyValue.of("http_scope", if (context.carrier?.let {
                    it.requestURI.removePrefix(it.contextPath).startsWith("/api/")
                } == true) "api" else "other")
        )
}

/** Runs inside Spring Security, while the authenticated principal is available. */
class TrafficClassificationFilter(private val syntheticUsers: Set<String>) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val authentication = SecurityContextHolder.getContext().authentication
        val synthetic = authentication != null && authentication.isAuthenticated &&
            authentication !is AnonymousAuthenticationToken &&
            authentication.name.lowercase(Locale.ROOT) in syntheticUsers
        request.setAttribute(TRAFFIC_ATTRIBUTE, if (synthetic) "synthetic" else "user")
        chain.doFilter(request, response)
    }
}
