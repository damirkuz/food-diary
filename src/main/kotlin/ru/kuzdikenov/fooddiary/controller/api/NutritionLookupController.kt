package ru.kuzdikenov.fooddiary.controller.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import ru.kuzdikenov.api.NutritionLookupApi
import ru.kuzdikenov.api.dto.NutritionLookupResponse
import ru.kuzdikenov.fooddiary.dto.ProductLookup
import ru.kuzdikenov.fooddiary.service.NutritionLookupService

@RestController
class NutritionLookupController(
    private val nutritionLookupService: NutritionLookupService,
) : NutritionLookupApi {

    override fun lookupProductNutrition(name: String): ResponseEntity<NutritionLookupResponse> {
        return ResponseEntity.ok(nutritionLookupService.lookupProductNutrition(name).toResponse())
    }

    private fun ProductLookup.toResponse(): NutritionLookupResponse {
        return NutritionLookupResponse(
            name = name,
            caloriesPer100g = caloriesPer100g,
            proteinsPer100g = proteinsPer100g,
            fatsPer100g = fatsPer100g,
            carbohydratesPer100g = carbohydratesPer100g,
            source = source,
            cached = cached
        )
    }
}
