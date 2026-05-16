package ru.kuzdikenov.fooddiary.form

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import ru.kuzdikenov.fooddiary.entity.FoodEntryEntity
import ru.kuzdikenov.fooddiary.entity.MealType
import ru.kuzdikenov.fooddiary.service.command.FoodEntryUpsertCommand
import java.time.LocalDate

class FoodEntryForm(

    @field:NotNull(message = "Дата обязательна")
    var entryDate: LocalDate? = null,

    @field:NotNull(message = "Тип приёма пищи обязателен")
    var mealType: MealType? = null,

    @field:NotNull(message = "Продукт обязателен")
    var productId: Long? = null,

    @field:NotNull(message = "Масса обязательна")
    @field:DecimalMin(value = "0.1", message = "Масса должна быть больше 0")
    @field:DecimalMax(value = "10000.0", message = "Масса должна быть не больше 10000 г")
    var grams: Double? = null
) {
    fun toCommand(): FoodEntryUpsertCommand {
        return FoodEntryUpsertCommand(
            entryDate = entryDate!!,
            mealType = mealType!!,
            productId = productId!!,
            grams = grams!!
        )
    }

    companion object {
        fun forDate(date: LocalDate): FoodEntryForm {
            return FoodEntryForm(entryDate = date)
        }

        fun fromFoodEntry(foodEntry: FoodEntryEntity): FoodEntryForm {
            return FoodEntryForm(
                entryDate = foodEntry.entryDate,
                mealType = foodEntry.mealType,
                productId = foodEntry.product.id,
                grams = foodEntry.grams
            )
        }
    }
}
