package ru.kuzdikenov.fooddiary.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.kuzdikenov.fooddiary.entity.ActivityLevel
import ru.kuzdikenov.fooddiary.entity.Gender
import ru.kuzdikenov.fooddiary.entity.GoalEntity
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.entity.UserProfileEntity
import java.time.LocalDate

class NutritionNormServiceTest {

    private val service = NutritionNormService()

    @Test
    fun `calculates daily norm with Mifflin St Jeor formula`() {
        val profile = UserProfileEntity(
            user = UserEntity(email = "user@example.com", passwordHash = "hash"),
            gender = Gender.MALE,
            birthDate = LocalDate.now().minusYears(25),
            heightCm = 180,
            weightKg = 80.0,
            activityLevel = ActivityLevel.MODERATE,
            goal = GoalEntity(
                name = "Поддержание",
                caloriesModifier = 1.0,
                proteinsRatio = 0.25,
                fatsRatio = 0.25,
                carbohydratesRatio = 0.50
            )
        )

        val norm = service.calculate(profile)

        assertEquals(25, norm.age)
        assertEquals(2798.0, norm.calories)
        assertEquals(174.9, norm.proteins)
        assertEquals(77.7, norm.fats)
        assertEquals(349.7, norm.carbohydrates)
    }
}
