package ru.kuzdikenov.fooddiary.form

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterForm(

    @field:NotBlank
    @field:Email
    @field:Size(max = 254)
    var email: String = "",

    @field:NotBlank
    @field:Size(min = 6, max = 100)
    var password: String = ""
)
