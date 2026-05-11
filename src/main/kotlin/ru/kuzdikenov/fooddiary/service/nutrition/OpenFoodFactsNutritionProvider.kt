package ru.kuzdikenov.fooddiary.service.nutrition

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body
import ru.kuzdikenov.api.dto.NutritionLookupResponse
import ru.kuzdikenov.fooddiary.config.properties.OpenFoodFactsProperties
import ru.kuzdikenov.fooddiary.dto.ProductLookup
import ru.kuzdikenov.fooddiary.exception.ExternalNutritionLookupException

@Component
@Order(1)
class OpenFoodFactsNutritionProvider(
    @Qualifier("externalRestClientBuilder")
    restClientBuilder: RestClient.Builder,
    properties: OpenFoodFactsProperties,
) : NutritionProvider {

    private val restClient = restClientBuilder
        .baseUrl(properties.baseUrl)
        .build()

    override fun lookup(name: String): ProductLookup? {
        val response = try {
            restClient.get()
                .uri { builder ->
                    builder
                        .path("/cgi/search.pl")
                        .queryParam("search_terms", name)
                        .queryParam("search_simple", 1)
                        .queryParam("action", "process")
                        .queryParam("json", 1)
                        .queryParam("page_size", 1)
                        .queryParam("fields", "product_name,nutriments")
                        .build()
                }
                .retrieve()
                .body<OpenFoodFactsSearchResponse>()
        } catch (ex: RestClientException) {
            throw ExternalNutritionLookupException()
        }

        val product = response?.products?.firstOrNull() ?: return null
        val nutriments = product.nutriments ?: return null

        return ProductLookup(
            name = product.productName?.takeIf { it.isNotBlank() } ?: name,
            caloriesPer100g = nutriments.energyKcal100g ?: return null,
            proteinsPer100g = nutriments.proteins100g ?: return null,
            fatsPer100g = nutriments.fat100g ?: return null,
            carbohydratesPer100g = nutriments.carbohydrates100g ?: return null,
            source = NutritionLookupResponse.Source.EXTERNAL_API,
            cached = false
        )
    }

    private data class OpenFoodFactsSearchResponse(
        val products: List<OpenFoodFactsProduct> = emptyList(),
    )

    private data class OpenFoodFactsProduct(
        @JsonProperty("product_name")
        val productName: String? = null,
        val nutriments: OpenFoodFactsNutriments? = null,
    )

    private data class OpenFoodFactsNutriments(
        @JsonProperty("energy-kcal_100g")
        val energyKcal100g: Double? = null,
        @JsonProperty("proteins_100g")
        val proteins100g: Double? = null,
        @JsonProperty("fat_100g")
        val fat100g: Double? = null,
        @JsonProperty("carbohydrates_100g")
        val carbohydrates100g: Double? = null,
    )
}
