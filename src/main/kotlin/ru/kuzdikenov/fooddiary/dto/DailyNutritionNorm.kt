package ru.kuzdikenov.fooddiary.dto

data class DailyNutritionNorm(
    val age: Int,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbohydrates: Double
)
