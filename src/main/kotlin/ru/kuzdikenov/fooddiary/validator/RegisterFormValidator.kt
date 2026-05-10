package ru.kuzdikenov.fooddiary.validator

import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import ru.kuzdikenov.fooddiary.form.RegisterForm
import ru.kuzdikenov.fooddiary.repository.UserRepository

@Component
class RegisterFormValidator(
    private val userRepository: UserRepository
) : Validator {

    override fun supports(clazz: Class<*>): Boolean {
        return RegisterForm::class.java.isAssignableFrom(clazz)
    }

    override fun validate(target: Any, errors: Errors) {
        val form = target as RegisterForm
        val email = form.email.trim().lowercase()

        if (email.isNotBlank() && userRepository.existsByEmail(email)) {
            errors.rejectValue(
                "email",
                "email.exists",
                "Пользователь с такой почтой уже существует"
            )
        }
    }
}
