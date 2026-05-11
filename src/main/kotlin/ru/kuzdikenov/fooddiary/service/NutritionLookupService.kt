package ru.kuzdikenov.fooddiary.service

import org.springframework.stereotype.Service
import ru.kuzdikenov.fooddiary.dto.ProductLookup
import ru.kuzdikenov.fooddiary.exception.ExternalNutritionLookupException
import ru.kuzdikenov.fooddiary.exception.NutritionLookupNotFoundException
import ru.kuzdikenov.fooddiary.service.nutrition.NutritionProvider

@Service
class NutritionLookupService(
    private val lookupProviders: List<NutritionProvider>,
    private val productSearchQueryService: ProductSearchQueryService,
) {

    fun lookupProductNutrition(name: String): ProductLookup {
        var externalFailure = false

        val queries = productSearchQueryService.buildQueries(name)

        queries.forEach { query ->
            lookupProviders.forEach { provider ->
                try {
                    provider.lookup(query)?.let { return it }
                } catch (ex: ExternalNutritionLookupException) {
                    externalFailure = true
                }
            }
        }

        if (externalFailure) {
            throw ExternalNutritionLookupException()
        }

        throw NutritionLookupNotFoundException()
    }
}
