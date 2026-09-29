package ru.kuzdikenov.fooddiary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.http.client.ClientHttpResponse
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.nio.charset.StandardCharsets

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApplicationIntegrationTest {

    @Autowired
    lateinit var environment: Environment

    @Autowired
    lateinit var redisConnectionFactory: RedisConnectionFactory

    private val baseUrl: String
        get() = "http://localhost:${environment.getProperty("local.server.port")}"

    /** RestTemplate, который не ходит за редиректами сам и не бросает исключение на 4xx/5xx. */
    private val restTemplate: RestTemplate = RestTemplate(
        JdkClientHttpRequestFactory(
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()
        )
    ).apply {
        errorHandler = object : ResponseErrorHandler {
            override fun hasError(response: ClientHttpResponse): Boolean = false
            override fun handleError(url: URI, method: HttpMethod, response: ClientHttpResponse) {}
        }
    }

    @Test
    fun `health endpoint reports up with database and redis`() {
        val response = restTemplate.getForEntity("$baseUrl/actuator/health", String::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertTrue(response.body!!.contains("\"status\":\"UP\""))
    }

    @Test
    fun `register login and work with api storing session in redis`() {
        val email = "e2e-${System.currentTimeMillis()}@test.local"
        val password = "password123"

        register(email, password)
        val sessionCookie = login(email, password)

        val apiHeaders = HttpHeaders().apply { add(HttpHeaders.COOKIE, sessionCookie) }
        val apiResponse = restTemplate.exchange(
            "$baseUrl/api/products",
            HttpMethod.GET,
            HttpEntity<Void>(apiHeaders),
            String::class.java
        )

        assertEquals(HttpStatus.OK, apiResponse.statusCode, "API должен пускать с сессией из Redis")

        val sessionKeys = StringRedisTemplate(redisConnectionFactory).keys("spring:session*")
        assertFalse(sessionKeys.isNullOrEmpty(), "HTTP-сессии должны храниться в Redis")
    }

    @Test
    fun `api rejects request without session`() {
        val response = restTemplate.exchange(
            "$baseUrl/api/products",
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders()),
            String::class.java
        )

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    private fun register(email: String, password: String) {
        val page = fetchFormPage("/register")

        val response = postForm(
            "/register",
            mapOf("email" to email, "password" to password, "_csrf" to page.csrf),
            page.cookie
        )

        assertEquals(HttpStatus.FOUND, response.statusCode, "Регистрация должна редиректить на /login")
        assertTrue(
            response.headers.location.toString().contains("/login"),
            "После регистрации должен быть редирект на /login"
        )
    }

    private fun login(email: String, password: String): String {
        val page = fetchFormPage("/login")

        val response = postForm(
            "/login",
            mapOf("email" to email, "password" to password, "_csrf" to page.csrf),
            page.cookie
        )

        assertEquals(HttpStatus.FOUND, response.statusCode, "Логин должен редиректить на /profile")

        return sessionIdCookie(response)
            ?: error("Логин не вернул cookie сессии")
    }

    private data class FormPage(val csrf: String, val cookie: String?)

    /** Spring Session по умолчанию называет cookie SESSION, Tomcat без него — JSESSIONID. */
    private fun sessionIdCookie(response: org.springframework.http.ResponseEntity<String>): String? {
        return response.headers[HttpHeaders.SET_COOKIE]
            ?.firstOrNull { it.startsWith("SESSION=") || it.startsWith("JSESSIONID=") }
            ?.substringBefore(";")
            ?.takeIf { !it.endsWith("=") }
    }

    private fun fetchFormPage(path: String): FormPage {
        val response = restTemplate.exchange(
            "$baseUrl$path",
            HttpMethod.GET,
            HttpEntity<Void>(HttpHeaders()),
            String::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode, "GET $path должен вернуть страницу формы")

        val csrf = Regex("name=\"_csrf\" value=\"([^\"]+)\"").find(response.body!!)
            ?.groupValues?.get(1)
            ?: error("CSRF-токен не найден на странице $path")

        return FormPage(csrf, sessionIdCookie(response))
    }

    private fun postForm(path: String, fields: Map<String, String>, cookie: String?): org.springframework.http.ResponseEntity<String> {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            if (cookie != null) add(HttpHeaders.COOKIE, cookie)
        }

        val body = fields.entries.joinToString("&") { (name, value) ->
            "${URLEncoder.encode(name, StandardCharsets.UTF_8)}=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
        }

        return restTemplate.exchange(
            URI.create("$baseUrl$path"),
            HttpMethod.POST,
            HttpEntity(body, headers),
            String::class.java
        )
    }

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<Nothing> = PostgreSQLContainer("postgres:17-alpine")

        @Container
        @JvmStatic
        val redis: GenericContainer<Nothing> = GenericContainer<Nothing>("redis:7").apply {
            withExposedPorts(6379)
        }

        @JvmStatic
        @DynamicPropertySource
        fun containerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.data.redis.host", redis::getHost)
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379).toString() }
        }
    }
}
