package ru.kuzdikenov.fooddiary.service.command

data class ProductUpsertCommand(
    val name: String,
    val caloriesPer100g: Double,
    val proteinsPer100g: Double,
    val fatsPer100g: Double,
    val carbohydratesPer100g: Double,
)
