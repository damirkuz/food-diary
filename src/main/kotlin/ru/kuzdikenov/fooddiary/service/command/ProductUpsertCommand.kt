package ru.kuzdikenov.fooddiary.service.command

import java.math.BigDecimal

data class ProductUpsertCommand(
    val name: String,
    val caloriesPer100g: BigDecimal,
    val proteinsPer100g: BigDecimal,
    val fatsPer100g: BigDecimal,
    val carbohydratesPer100g: BigDecimal,
)
