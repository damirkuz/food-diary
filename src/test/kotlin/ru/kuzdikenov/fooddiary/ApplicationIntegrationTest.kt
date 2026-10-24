package ru.kuzdikenov.fooddiary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import io.micrometer.core.instrument.MeterRegistry
import org.mockito.Mockito
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
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.Testcontainers as ContainerHost
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.images.builder.Transferable
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.UUID
import ru.kuzdikenov.api.dto.NutritionLookupResponse
import ru.kuzdikenov.fooddiary.dto.ProductLookup
import ru.kuzdikenov.fooddiary.entity.MealType
import ru.kuzdikenov.fooddiary.exception.EmailAlreadyExistsException
import ru.kuzdikenov.fooddiary.exception.ExternalNutritionLookupException
import ru.kuzdikenov.fooddiary.exception.NutritionLookupNotFoundException
import ru.kuzdikenov.fooddiary.form.RegisterForm
import ru.kuzdikenov.fooddiary.repository.UserRepository
import ru.kuzdikenov.fooddiary.service.*
import ru.kuzdikenov.fooddiary.service.command.FoodEntryUpsertCommand
import ru.kuzdikenov.fooddiary.service.command.ProductUpsertCommand
import ru.kuzdikenov.fooddiary.service.nutrition.NutritionProvider

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["management.server.port=0"],
)
@Testcontainers
class ApplicationIntegrationTest {

    @Autowired
    lateinit var environment: Environment

    @Autowired
    lateinit var redisConnectionFactory: RedisConnectionFactory

    @Autowired lateinit var meters: MeterRegistry
    @Autowired lateinit var registrations: RegisterService
    @Autowired lateinit var products: ProductService
    @Autowired lateinit var entries: FoodEntryService
    @Autowired lateinit var lookups: NutritionLookupService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var transactionManager: PlatformTransactionManager
    @Autowired lateinit var objectMapper: ObjectMapper

    @MockitoBean
    lateinit var nutritionProvider: NutritionProvider

    private val baseUrl: String
        get() = "http://localhost:${environment.getProperty("local.server.port")}"

    private val managementUrl: String
        get() = "http://localhost:${environment.getProperty("local.management.port")}"

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
        val response = restTemplate.getForEntity("$managementUrl/actuator/health", String::class.java)

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

    @Test
    fun `prometheus exposes business metrics without session only on management port`() {
        val response = restTemplate.getForEntity("$managementUrl/actuator/prometheus", String::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertTrue(response.headers.contentType.toString().contains("text/plain"))
        listOf(
            "food_diary_registrations_total",
            "food_diary_products_creations_total",
            "food_diary_entries_creations_total",
            "food_diary_nutrition_lookups_total",
        ).forEach { name ->
            assertTrue(response.body!!.contains(name), "Missing $name; exported business metrics: " +
                response.body!!.lineSequence().filter { it.startsWith("food_diary_") }.joinToString("\n"))
        }

        val mainResponse = restTemplate.getForEntity("$baseUrl/actuator/prometheus", String::class.java)
        assertEquals(HttpStatus.NOT_FOUND, mainResponse.statusCode)
    }

    @Test
    fun `successful domain counters increase only after commit`() {
        val names = listOf("food.diary.registrations", "food.diary.products.creations", "food.diary.entries.creations")
        val before = names.map { counter(it) }
        TransactionTemplate(transactionManager).executeWithoutResult {
            createUserProductAndEntry()
            names.forEachIndexed { index, name -> assertEquals(before[index], counter(name), name) }
        }
        names.forEachIndexed { index, name -> assertEquals(before[index] + 1, counter(name), name) }
    }

    @Test
    fun `rollback does not increment successful domain counters`() {
        val names = listOf("food.diary.registrations", "food.diary.products.creations", "food.diary.entries.creations")
        val before = names.map { counter(it) }
        TransactionTemplate(transactionManager).executeWithoutResult { status ->
            createUserProductAndEntry()
            status.setRollbackOnly()
        }
        names.forEachIndexed { index, name -> assertEquals(before[index], counter(name), name) }
    }

    @Test
    fun `rejected registration does not count as successful`() {
        val form = RegisterForm("metrics-${UUID.randomUUID()}@test.local", "password123")
        registrations.register(form)
        val before = counter("food.diary.registrations")
        assertThrows(EmailAlreadyExistsException::class.java) { registrations.register(form) }
        assertEquals(before, counter("food.diary.registrations"))
    }

    @Test
    fun `nutrition lookups count provider result cache hit and each error once`() {
        val query = "metrics-${UUID.randomUUID()}"
        Mockito.`when`(nutritionProvider.lookup(query)).thenReturn(
            ProductLookup(query, 100.0, 1.0, 1.0, 1.0, NutritionLookupResponse.Source.EXTERNAL_API, false)
        )
        val success = lookupCount("success", false)
        val cacheHit = lookupCount("success", true)
        lookups.lookupProductNutrition(query)
        assertEquals(success + 1, lookupCount("success", false))
        assertTrue(lookups.lookupProductNutrition(query).cached)
        assertEquals(cacheHit + 1, lookupCount("success", true))
        assertEquals(success + 1, lookupCount("success", false))

        val notFound = lookupCount("not_found", false)
        assertThrows(NutritionLookupNotFoundException::class.java) {
            lookups.lookupProductNutrition("missing-${UUID.randomUUID()}")
        }
        assertEquals(notFound + 1, lookupCount("not_found", false))

        val unavailableQuery = "unavailable-${UUID.randomUUID()}"
        Mockito.`when`(nutritionProvider.lookup(unavailableQuery)).thenThrow(ExternalNutritionLookupException())
        val externalError = lookupCount("external_error", false)
        assertThrows(ExternalNutritionLookupException::class.java) { lookups.lookupProductNutrition(unavailableQuery) }
        assertEquals(externalError + 1, lookupCount("external_error", false))

        val brokenQuery = "broken-${UUID.randomUUID()}"
        Mockito.`when`(nutritionProvider.lookup(brokenQuery)).thenThrow(IllegalStateException("provider failure"))
        val error = lookupCount("error", false)
        assertThrows(IllegalStateException::class.java) { lookups.lookupProductNutrition(brokenQuery) }
        assertEquals(error + 1, lookupCount("error", false))
    }

    @Test
    fun `victoria metrics scrapes and stores all four business metric families`() {
        createUserProductAndEntry()
        assertThrows(NutritionLookupNotFoundException::class.java) {
            lookups.lookupProductNutrition("vm-${UUID.randomUUID()}")
        }
        val expected = mapOf(
            "food_diary_registrations_total" to counter("food.diary.registrations"),
            "food_diary_products_creations_total" to counter("food.diary.products.creations"),
            "food_diary_entries_creations_total" to counter("food.diary.entries.creations"),
            "food_diary_nutrition_lookups_total" to meters.find("food.diary.nutrition.lookups").counters().sumOf { it.count() },
        )
        val port = requireNotNull(environment.getProperty("local.management.port", Int::class.java))
        ContainerHost.exposeHostPorts(port)
        val scrapeConfig = """
            global:
              scrape_interval: 1s
            scrape_configs:
              - job_name: food-diary-test
                metrics_path: /actuator/prometheus
                static_configs:
                  - targets: ['host.testcontainers.internal:$port']
        """.trimIndent()

        GenericContainer<Nothing>("victoriametrics/victoria-metrics:v1.154.0").apply {
            withExposedPorts(8428)
            withCopyToContainer(Transferable.of(scrapeConfig), "/etc/vm/scrape.yml")
            withCommand("-promscrape.config=/etc/vm/scrape.yml", "-storageDataPath=/tmp/vm", "-search.latencyOffset=0s")
            waitingFor(Wait.forHttp("/health").forPort(8428))
        }.use { vm ->
            vm.start()
            val query = URLEncoder.encode(
                "{__name__=~\"food_diary_.*_total\",job=\"food-diary-test\"}", StandardCharsets.UTF_8
            )
            val uri = URI.create("http://${vm.host}:${vm.getMappedPort(8428)}/api/v1/query?query=$query")
            var actual: Map<String, Double> = emptyMap()
            val deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(30)
            while (System.nanoTime() < deadline) {
                val body = restTemplate.getForObject(uri, String::class.java)!!
                actual = objectMapper.readTree(body).path("data").path("result")
                    .groupBy { it.path("metric").path("__name__").asString() }
                    .mapValues { (_, series) -> series.sumOf { it.path("value").path(1).asDouble() } }
                if (actual == expected) break
                Thread.sleep(250)
            }
            assertEquals(expected, actual, "VictoriaMetrics must store the values exposed by Actuator")
        }
    }

    private fun counter(name: String): Double = meters.find(name).counter()?.count() ?: 0.0

    private fun lookupCount(outcome: String, cached: Boolean): Double = meters
        .find("food.diary.nutrition.lookups").tags("outcome", outcome, "cached", cached.toString())
        .counter()?.count() ?: 0.0

    private fun createUserProductAndEntry() {
        val email = "metrics-${UUID.randomUUID()}@test.local"
        registrations.register(RegisterForm(email, "password123"))
        val ownerId = requireNotNull(users.findByEmail(email)?.id)
        val product = products.createProduct(ProductUpsertCommand("Test meal", 100.0, 1.0, 1.0, 1.0), ownerId)
        entries.createFoodEntry(
            FoodEntryUpsertCommand(LocalDate.now(), MealType.LUNCH, requireNotNull(product.id), 100.0), ownerId
        )
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
