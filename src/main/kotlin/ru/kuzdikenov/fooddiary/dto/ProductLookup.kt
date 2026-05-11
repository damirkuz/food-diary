package ru.kuzdikenov.fooddiary.dto

import ru.kuzdikenov.api.dto.NutritionLookupResponse

data class ProductLookup(
    val name: String,
    val caloriesPer100g: Double,
    val proteinsPer100g: Double,
    val fatsPer100g: Double,
    val carbohydratesPer100g: Double,
    val source: NutritionLookupResponse.Source,
    val cached: Boolean,
)
