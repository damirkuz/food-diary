package ru.kuzdikenov.fooddiary.service

import org.slf4j.event.Level
import org.springframework.stereotype.Service
import ru.kuzdikenov.fooddiary.dto.ProductLookup
import ru.kuzdikenov.fooddiary.exception.ExternalNutritionLookupException
import ru.kuzdikenov.fooddiary.exception.NutritionLookupNotFoundException
import ru.kuzdikenov.fooddiary.logging.BusinessEventLogger
import ru.kuzdikenov.fooddiary.metrics.BusinessMetrics
import ru.kuzdikenov.fooddiary.service.nutrition.NutritionProvider
import java.util.Locale

@Service
class NutritionLookupService(
    private val lookupProviders: List<NutritionProvider>,
    private val productLookupCacheService: ProductLookupCacheService,
    private val businessMetrics: BusinessMetrics,
) {

    fun lookupProductNutrition(name: String): ProductLookup {
        val startedAt = System.nanoTime()
        var outcome = "error"
        var source = "none"
        var cached = false
        try {
            return lookup(name).also {
                outcome = "success"
                source = it.source.name.lowercase(Locale.ROOT)
                cached = it.cached
            }
        } catch (ex: NutritionLookupNotFoundException) {
            outcome = "not_found"
            throw ex
        } catch (ex: ExternalNutritionLookupException) {
            outcome = "external_error"
            source = "external_api"
            throw ex
        } finally {
            businessMetrics.nutritionLookupCompleted(outcome, cached)
            val level = when (outcome) {
                "success" -> Level.INFO
                "not_found" -> Level.WARN
                else -> Level.ERROR
            }
            BusinessEventLogger.record(
                "nutrition_lookup_completed", "Завершён поиск состава продукта", outcome,
                mapOf(
                    "source" to source,
                    "cached" to cached,
                    "duration_ms" to (System.nanoTime() - startedAt) / 1_000_000.0,
                ),
                level,
            )
        }
    }

    private fun lookup(name: String): ProductLookup {
        val query = name.trim()

        productLookupCacheService.get(query)?.let { return it }

        var externalFailure = false

        lookupProviders.forEach { provider ->
            try {
                provider.lookup(query)?.let {
                    productLookupCacheService.put(query, it)
                    return it.copy(cached = false)
                }
            } catch (ex: ExternalNutritionLookupException) {
                externalFailure = true
            }
        }

        if (externalFailure) {
            throw ExternalNutritionLookupException()
        }

        throw NutritionLookupNotFoundException()
    }
}
