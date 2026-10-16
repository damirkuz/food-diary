package ru.kuzdikenov.fooddiary.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.slf4j.LoggerFactory
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import ru.kuzdikenov.api.dto.NutritionLookupResponse
import ru.kuzdikenov.fooddiary.dto.ProductLookup
import ru.kuzdikenov.fooddiary.entity.*
import ru.kuzdikenov.fooddiary.exception.ExternalNutritionLookupException
import ru.kuzdikenov.fooddiary.exception.NutritionLookupNotFoundException
import ru.kuzdikenov.fooddiary.repository.*
import ru.kuzdikenov.fooddiary.service.*
import ru.kuzdikenov.fooddiary.service.command.*
import ru.kuzdikenov.fooddiary.service.nutrition.NutritionProvider
import java.time.LocalDate
import java.util.Optional

class BusinessEventLoggingTest {
    private val logger = LoggerFactory.getLogger("ru.kuzdikenov.fooddiary.logging.BusinessEventLogger") as Logger
    private val previousLevel = logger.level
    private val appender = object : ListAppender<ILoggingEvent>() {
        override fun append(event: ILoggingEvent) {
            event.prepareForDeferredProcessing()
            super.append(event)
        }
    }.apply { start() }
    private val transaction = TransactionTemplate(object : AbstractPlatformTransactionManager() {
        override fun doGetTransaction(): Any = Any()
        override fun doBegin(transaction: Any, definition: TransactionDefinition) {}
        override fun doCommit(status: DefaultTransactionStatus) {}
        override fun doRollback(status: DefaultTransactionStatus) {}
    })
    private val products = Mockito.mock(ProductRepository::class.java)
    private val users = Mockito.mock(UserRepository::class.java)
    private val domain = ProductDomainService(products)
    private val owner = UserEntity(id = 7, email = "private@example.com", passwordHash = "secret")

    init {
        logger.level = Level.INFO
        logger.addAppender(appender)
        given(users.findById(7L)).willReturn(Optional.of(owner))
    }

    @AfterEach
    fun cleanup() {
        logger.detachAppender(appender)
        logger.level = previousLevel
        appender.stop()
    }

    @Test
    fun productCreationLogsOnlyAfterCommit() {
        val service = productService()
        transaction.executeWithoutResult {
            service.createProduct(productCommand(), 7)
            assertTrue(appender.list.isEmpty())
        }
        assertEquals("product_created", fields()["event"])
        assertEquals("success", fields()["outcome"])
        assertEquals(42L, fields()["product_id"])
        assertFalse(fields().toString().contains("private"))
        assertFalse(fields().toString().contains("secret"))
    }

    @Test
    fun rolledBackProductCreationDoesNotLogSuccess() {
        val service = productService()
        transaction.executeWithoutResult {
            service.createProduct(productCommand(), 7)
            it.setRollbackOnly()
        }
        assertTrue(appender.list.isEmpty())
    }

    @Test
    fun foodEntryCreationLogsOnlyAfterCommit() {
        val entries = Mockito.mock(FoodEntryRepository::class.java)
        given(products.findById(42L)).willReturn(Optional.of(
            ProductEntity(id = 42, name = "private-food", caloriesPer100g = 100.0,
                proteinsPer100g = 1.0, fatsPer100g = 1.0, carbohydratesPer100g = 1.0, owner = owner)
        ))
        given(entries.save(Mockito.any(FoodEntryEntity::class.java)))
            .willAnswer { it.getArgument<FoodEntryEntity>(0).apply { id = 15 } }
        val service = FoodEntryService(entries, users, domain)
        transaction.executeWithoutResult {
            service.createFoodEntry(FoodEntryUpsertCommand(LocalDate.now(), MealType.LUNCH, 42, 100.0), 7)
            assertTrue(appender.list.isEmpty())
        }
        assertEquals("food_entry_created", fields()["event"])
        assertEquals(15L, fields()["food_entry_id"])
        assertFalse(fields().toString().contains("private-food"))
    }

    @Test
    fun lookupLogsCacheHitWithoutSearchQuery() {
        val cache = Mockito.mock(ProductLookupCacheService::class.java)
        given(cache.get("private-query")).willReturn(lookup(true))
        NutritionLookupService(emptyList(), cache).lookupProductNutrition("private-query")
        assertEquals("nutrition_lookup_completed", fields()["event"])
        assertEquals("success", fields()["outcome"])
        assertEquals(true, fields()["cached"])
        assertEquals("external_api", fields()["source"])
        assertTrue((fields()["duration_ms"] as Double) >= 0)
        assertFalse(fields().toString().contains("private-query"))
    }

    @Test
    fun lookupLogsProviderSuccessOnce() {
        val cache = Mockito.mock(ProductLookupCacheService::class.java)
        val provider = Mockito.mock(NutritionProvider::class.java)
        given(provider.lookup("private-query")).willReturn(lookup(false))
        NutritionLookupService(listOf(provider), cache).lookupProductNutrition("private-query")
        assertEquals("success", fields()["outcome"])
        assertEquals(false, fields()["cached"])
        assertEquals("external_api", fields()["source"])
    }

    @Test
    fun lookupLogsExternalFailureAndPreservesException() {
        val cache = Mockito.mock(ProductLookupCacheService::class.java)
        val provider = Mockito.mock(NutritionProvider::class.java)
        given(provider.lookup("private-query")).willThrow(ExternalNutritionLookupException())
        assertThrows(ExternalNutritionLookupException::class.java) {
            NutritionLookupService(listOf(provider), cache).lookupProductNutrition("private-query")
        }
        assertEquals("external_error", fields()["outcome"])
        assertEquals(Level.ERROR, appender.list.single().level)
    }

    @Test
    fun lookupLogsNotFoundAsWarning() {
        val cache = Mockito.mock(ProductLookupCacheService::class.java)
        assertThrows(NutritionLookupNotFoundException::class.java) {
            NutritionLookupService(emptyList(), cache).lookupProductNutrition("private-query")
        }
        assertEquals("not_found", fields()["outcome"])
        assertEquals(Level.WARN, appender.list.single().level)
    }

    private fun fields() = appender.list.single().keyValuePairs.associate { it.key to it.value }

    private fun productService(): ProductService {
        given(products.save(Mockito.any(ProductEntity::class.java)))
            .willAnswer { it.getArgument<ProductEntity>(0).apply { id = 42 } }
        return ProductService(products, users, domain)
    }

    private fun productCommand() = ProductUpsertCommand("private-food", 100.0, 1.0, 1.0, 1.0)

    private fun lookup(cached: Boolean) = ProductLookup(
        "private-query", 100.0, 1.0, 1.0, 1.0, NutritionLookupResponse.Source.EXTERNAL_API, cached
    )
}
