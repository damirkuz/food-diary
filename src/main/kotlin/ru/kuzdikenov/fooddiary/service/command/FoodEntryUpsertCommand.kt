package ru.kuzdikenov.fooddiary.service.command

import ru.kuzdikenov.fooddiary.entity.MealType
import java.time.LocalDate

data class FoodEntryUpsertCommand(
    val entryDate: LocalDate,
    val mealType: MealType,
    val productId: Long,
    val grams: Double
) {
}
