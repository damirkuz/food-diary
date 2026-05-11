package ru.kuzdikenov.fooddiary.dto

data class FoodEntryCalculation(
    val grams: Double,
    val calculatedCalories: Double,
    val calculatedProteins: Double,
    val calculatedFats: Double,
    val calculatedCarbohydrates: Double
)
