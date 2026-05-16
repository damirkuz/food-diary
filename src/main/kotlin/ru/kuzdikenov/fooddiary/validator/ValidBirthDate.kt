package ru.kuzdikenov.fooddiary.validator

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import kotlin.reflect.KClass
import java.time.LocalDate
import java.time.Period

@MustBeDocumented
@Constraint(validatedBy = [BirthDateValidator::class])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ValidBirthDate(
    val message: String = "Возраст должен быть от 1 до 120 лет",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out jakarta.validation.Payload>> = [],
)

class BirthDateValidator : ConstraintValidator<ValidBirthDate, LocalDate?> {

    override fun isValid(value: LocalDate?, context: ConstraintValidatorContext): Boolean {
        if (value == null) {
            return true
        }

        val today = LocalDate.now()
        if (!value.isBefore(today)) {
            return false
        }

        val age = Period.between(value, today).years
        return age in MIN_AGE..MAX_AGE
    }

    companion object {
        private const val MIN_AGE = 1
        private const val MAX_AGE = 120
    }
}
