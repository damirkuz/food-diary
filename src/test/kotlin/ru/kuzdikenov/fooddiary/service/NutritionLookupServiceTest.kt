package ru.kuzdikenov.fooddiary.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.kuzdikenov.api.dto.NutritionLookupResponse
import ru.kuzdikenov.fooddiary.dto.ProductLookup
import ru.kuzdikenov.fooddiary.exception.ExternalNutritionLookupException
import ru.kuzdikenov.fooddiary.exception.NutritionLookupNotFoundException
import ru.kuzdikenov.fooddiary.service.nutrition.NutritionProvider
import ru.kuzdikenov.fooddiary.service.translate.TranslationProvider

class NutritionLookupServiceTest {

    private val cacheService = Mockito.mock(ProductLookupCacheService::class.java)
    private val provider = Mockito.mock(NutritionProvider::class.java)
    private val queryService = ProductSearchQueryService(
        translationProvider = object : TranslationProvider {
            override fun translate(text: String): String? = "apple"
        }
    )
    private val service = NutritionLookupService(listOf(provider), queryService, cacheService)

    @Test
    fun `returns cached product without calling providers`() {
        val cached = lookup(name = "banana", cached = true)
        Mockito.`when`(cacheService.get("banana")).thenReturn(cached)

        assertEquals(cached, service.lookupProductNutrition("banana"))
        Mockito.verify(provider, Mockito.never()).lookup(Mockito.anyString())
    }

    @Test
    fun `stores provider result and marks response as external`() {
        val providerResult = lookup(name = "banana", cached = true)
        Mockito.`when`(cacheService.get("banana")).thenReturn(null)
        Mockito.`when`(provider.lookup("banana")).thenReturn(providerResult)

        val result = service.lookupProductNutrition("banana")

        assertFalse(result.cached)
        assertEquals("banana", result.name)
        Mockito.verify(cacheService).put("banana", providerResult)
    }

    @Test
    fun `translates cyrillic query before lookup`() {
        val translatedResult = lookup(name = "apple", cached = false)
        Mockito.`when`(cacheService.get("apple")).thenReturn(null)
        Mockito.`when`(provider.lookup("apple")).thenReturn(translatedResult)

        assertEquals(translatedResult, service.lookupProductNutrition("яблоко"))
    }

    @Test
    fun `throws external exception when provider fails`() {
        Mockito.`when`(cacheService.get("banana")).thenReturn(null)
        Mockito.`when`(provider.lookup("banana")).thenThrow(ExternalNutritionLookupException())

        assertThrows(ExternalNutritionLookupException::class.java) {
            service.lookupProductNutrition("banana")
        }
    }

    @Test
    fun `throws not found when providers return no data`() {
        Mockito.`when`(cacheService.get("banana")).thenReturn(null)
        Mockito.`when`(provider.lookup("banana")).thenReturn(null)

        assertThrows(NutritionLookupNotFoundException::class.java) {
            service.lookupProductNutrition("banana")
        }
    }

    private fun lookup(name: String, cached: Boolean): ProductLookup {
        return ProductLookup(
            name = name,
            caloriesPer100g = 89.0,
            proteinsPer100g = 1.1,
            fatsPer100g = 0.3,
            carbohydratesPer100g = 22.8,
            source = NutritionLookupResponse.Source.EXTERNAL_API,
            cached = cached
        )
    }
}
