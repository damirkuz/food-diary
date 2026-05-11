package ru.kuzdikenov.fooddiary.service.nutrition

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body
import ru.kuzdikenov.api.dto.NutritionLookupResponse
import ru.kuzdikenov.fooddiary.config.properties.UsdaProperties
import ru.kuzdikenov.fooddiary.dto.ProductLookup
import ru.kuzdikenov.fooddiary.exception.ExternalNutritionLookupException

@Component
@Order(2)
class UsdaNutritionProvider(
    @Qualifier("externalRestClientBuilder")
    restClientBuilder: RestClient.Builder,
    private val properties: UsdaProperties,
) : NutritionProvider {


    private val restClient = restClientBuilder
        .baseUrl(properties.baseUrl)
        .build()

    override fun lookup(name: String): ProductLookup? {
        if (properties.apiKey.isBlank()) {
            return null
        }

        val response = try {
            restClient.get()
                .uri { builder ->
                    builder
                        .path("/fdc/v1/foods/search")
                        .queryParam("api_key", properties.apiKey)
                        .queryParam("query", name)
                        .queryParam("pageSize", 1)
                        .build()
                }
                .retrieve()
                .body<UsdaFoodSearchResponse>()
        } catch (ex: RestClientException) {
            throw ExternalNutritionLookupException()
        }

        val food = response?.foods?.firstOrNull() ?: return null

        return ProductLookup(
            name = food.description?.takeIf { it.isNotBlank() } ?: name,
            caloriesPer100g = food.nutrientValue("208", "energy") ?: return null,
            proteinsPer100g = food.nutrientValue("203", "protein") ?: return null,
            fatsPer100g = food.nutrientValue("204", "fat", "lipid") ?: return null,
            carbohydratesPer100g = food.nutrientValue("205", "carbohydrate") ?: return null,
            source = NutritionLookupResponse.Source.EXTERNAL_API,
            cached = false
        )
    }

    private fun UsdaFood.nutrientValue(number: String, vararg nameParts: String): Double? {
        return foodNutrients.firstOrNull { nutrient ->
            nutrient.nutrientNumber == number ||
                nameParts.any { part -> nutrient.nutrientName?.contains(part, ignoreCase = true) == true }
        }?.value
    }

    private data class UsdaFoodSearchResponse(
        val foods: List<UsdaFood> = emptyList(),
    )

    private data class UsdaFood(
        val description: String? = null,
        val foodNutrients: List<UsdaNutrient> = emptyList(),
    )

    private data class UsdaNutrient(
        val nutrientNumber: String? = null,
        val nutrientName: String? = null,
        val value: Double? = null,
    )
}
