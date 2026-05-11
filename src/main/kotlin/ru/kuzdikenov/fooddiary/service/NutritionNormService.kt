package ru.kuzdikenov.fooddiary.service

import org.springframework.stereotype.Service
import ru.kuzdikenov.fooddiary.dto.DailyNutritionNorm
import ru.kuzdikenov.fooddiary.entity.Gender
import ru.kuzdikenov.fooddiary.entity.UserProfileEntity
import kotlin.math.pow
import kotlin.math.round
import java.time.LocalDate
import java.time.Period

@Service
class NutritionNormService {

    fun calculate(profile: UserProfileEntity): DailyNutritionNorm {
        val age = Period.between(profile.birthDate, LocalDate.now()).years

        val bmr = 10.0 * profile.weightKg +
            6.25 * profile.heightCm -
            5.0 * age +
            when (profile.gender) {
                Gender.MALE -> 5.0
                Gender.FEMALE -> -161.0
            }

        val targetCalories = bmr
            .times(profile.activityLevel.coefficient)
            .times(profile.goal.caloriesModifier)

        return DailyNutritionNorm(
            age = age,
            calories = targetCalories.roundTo(0),
            proteins = targetCalories.macroGrams(profile.goal.proteinsRatio, PROTEIN_CALORIES_PER_GRAM),
            fats = targetCalories.macroGrams(profile.goal.fatsRatio, FAT_CALORIES_PER_GRAM),
            carbohydrates = targetCalories.macroGrams(profile.goal.carbohydratesRatio, CARBOHYDRATE_CALORIES_PER_GRAM)
        )
    }

    private fun Double.macroGrams(ratio: Double, caloriesPerGram: Double): Double {
        return (this * ratio / caloriesPerGram).roundTo(1)
    }

    private fun Double.roundTo(scale: Int): Double {
        val factor = 10.0.pow(scale)
        return round(this * factor) / factor
    }

    companion object {
        private const val PROTEIN_CALORIES_PER_GRAM = 4.0
        private const val FAT_CALORIES_PER_GRAM = 9.0
        private const val CARBOHYDRATE_CALORIES_PER_GRAM = 4.0
    }
}
