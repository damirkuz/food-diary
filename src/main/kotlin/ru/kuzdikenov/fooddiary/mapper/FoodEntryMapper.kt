package ru.kuzdikenov.fooddiary.mapper

import org.springframework.stereotype.Component
import ru.kuzdikenov.api.dto.DailyNormResponse
import ru.kuzdikenov.api.dto.DiaryDayResponse
import ru.kuzdikenov.api.dto.DiaryTotalsResponse
import ru.kuzdikenov.api.dto.FoodEntryCalculationResponse
import ru.kuzdikenov.api.dto.FoodEntryCreateRequest
import ru.kuzdikenov.api.dto.FoodEntryResponse
import ru.kuzdikenov.api.dto.FoodEntryUpdateRequest
import ru.kuzdikenov.api.dto.ProductShortResponse
import ru.kuzdikenov.fooddiary.dto.DailyNutritionNorm
import ru.kuzdikenov.fooddiary.dto.DiaryTotals
import ru.kuzdikenov.fooddiary.dto.FoodEntryCalculation
import ru.kuzdikenov.fooddiary.entity.FoodEntryEntity
import ru.kuzdikenov.fooddiary.entity.MealType
import ru.kuzdikenov.fooddiary.service.command.FoodEntryUpsertCommand
import java.time.LocalDate

@Component
class FoodEntryMapper {

    fun toCommand(foodEntryCreateRequest: FoodEntryCreateRequest): FoodEntryUpsertCommand {
        return FoodEntryUpsertCommand(
            entryDate = foodEntryCreateRequest.entryDate,
            mealType = MealType.valueOf(foodEntryCreateRequest.mealType.value),
            productId = foodEntryCreateRequest.productId,
            grams = foodEntryCreateRequest.grams
        )
    }

    fun toCommand(foodEntryUpdateRequest: FoodEntryUpdateRequest): FoodEntryUpsertCommand {
        return FoodEntryUpsertCommand(
            entryDate = foodEntryUpdateRequest.entryDate,
            mealType = MealType.valueOf(foodEntryUpdateRequest.mealType.value),
            productId = foodEntryUpdateRequest.productId,
            grams = foodEntryUpdateRequest.grams
        )
    }

    fun toResponse(foodEntry: FoodEntryEntity): FoodEntryResponse {
        return FoodEntryResponse(
            id = foodEntry.id!!,
            entryDate = foodEntry.entryDate,
            mealType = ru.kuzdikenov.api.dto.MealType.forValue(foodEntry.mealType.name),
            grams = foodEntry.grams,
            product = ProductShortResponse(
                id = foodEntry.product.id!!,
                name = foodEntry.product.name
            ),
            calculatedCalories = foodEntry.calories,
            calculatedProteins = foodEntry.proteins,
            calculatedFats = foodEntry.fats,
            calculatedCarbohydrates = foodEntry.carbohydrates
        )
    }

    fun toResponse(foodEntryCalculation: FoodEntryCalculation): FoodEntryCalculationResponse {
        return FoodEntryCalculationResponse(
            grams = foodEntryCalculation.grams,
            calculatedCalories = foodEntryCalculation.calculatedCalories,
            calculatedProteins = foodEntryCalculation.calculatedProteins,
            calculatedFats = foodEntryCalculation.calculatedFats,
            calculatedCarbohydrates = foodEntryCalculation.calculatedCarbohydrates
        )
    }

    fun toDiaryDayResponse(
        date: LocalDate,
        entries: List<FoodEntryEntity>,
        totals: DiaryTotals,
        dailyNorm: DailyNutritionNorm?
    ): DiaryDayResponse {
        return DiaryDayResponse(
            date = date,
            propertyEntries = entries.map(::toResponse),
            totals = DiaryTotalsResponse(
                calories = totals.calories,
                proteins = totals.proteins,
                fats = totals.fats,
                carbohydrates = totals.carbohydrates
            ),
            dailyNorm = dailyNorm?.let {
                DailyNormResponse(
                    dailyCalories = it.calories,
                    dailyProteins = it.proteins,
                    dailyFats = it.fats,
                    dailyCarbohydrates = it.carbohydrates
                )
            }
        )
    }
}
