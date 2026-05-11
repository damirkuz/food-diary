package ru.kuzdikenov.fooddiary.service.command

import ru.kuzdikenov.fooddiary.entity.ActivityLevel
import ru.kuzdikenov.fooddiary.entity.Gender
import java.math.BigDecimal
import java.time.LocalDate

data class ProfileUpsertCommand(
    val gender: Gender,
    val birthDate: LocalDate,
    val heightCm: Int,
    val weightKg: BigDecimal,
    val activityLevel: ActivityLevel,
    val goalId: Long,
)
