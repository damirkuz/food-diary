package ru.kuzdikenov.fooddiary.form

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Past
import ru.kuzdikenov.fooddiary.entity.ActivityLevel
import ru.kuzdikenov.fooddiary.entity.Gender
import ru.kuzdikenov.fooddiary.entity.UserProfileEntity
import ru.kuzdikenov.fooddiary.service.command.ProfileUpsertCommand
import ru.kuzdikenov.fooddiary.validator.ValidBirthDate
import java.time.LocalDate

class ProfileForm(
    @field:NotNull(message = "Пол обязателен")
    var gender: Gender? = null,

    @field:NotNull(message = "Дата рождения обязательна")
    @field:Past(message = "Дата рождения должна быть в прошлом")
    @field:ValidBirthDate
    var birthDate: LocalDate? = null,

    @field:NotNull(message = "Рост обязателен")
    @field:Min(value = 50, message = "Рост должен быть не меньше 50 см")
    @field:Max(value = 250, message = "Рост должен быть не больше 250 см")
    var heightCm: Int? = null,

    @field:NotNull(message = "Вес обязателен")
    @field:DecimalMin(value = "2.0", message = "Вес должен быть не меньше 2 кг")
    @field:DecimalMax(value = "500.0", message = "Вес должен быть не больше 500 кг")
    var weightKg: Double? = null,

    @field:NotNull(message = "Уровень активности обязателен")
    var activityLevel: ActivityLevel? = null,

    @field:NotNull(message = "Цель обязательна")
    var goalId: Long? = null,
) {
    fun toCommand(): ProfileUpsertCommand {
        return ProfileUpsertCommand(
            gender = gender!!,
            birthDate = birthDate!!,
            heightCm = heightCm!!,
            weightKg = weightKg!!,
            activityLevel = activityLevel!!,
            goalId = goalId!!
        )
    }

    companion object {
        fun fromProfile(profile: UserProfileEntity): ProfileForm {
            return ProfileForm(
                gender = profile.gender,
                birthDate = profile.birthDate,
                heightCm = profile.heightCm,
                weightKg = profile.weightKg,
                activityLevel = profile.activityLevel,
                goalId = profile.goal.id
            )
        }
    }
}
