package ru.kuzdikenov.fooddiary.controller.web

import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import ru.kuzdikenov.fooddiary.form.RegisterForm
import ru.kuzdikenov.fooddiary.service.RegisterService
import ru.kuzdikenov.fooddiary.validator.RegisterFormValidator

@Controller
class AuthController (
    private val registerService: RegisterService,
    private val registerFormValidator: RegisterFormValidator
) {

    @InitBinder("form")
    fun initRegisterFormBinder(binder: WebDataBinder) {
        binder.addValidators(registerFormValidator)
    }

    @GetMapping("/login")
    fun login() : String {
        return "auth/login"
    }

    @GetMapping("/register")
    fun register(model: Model): String {
        model.addAttribute("form", RegisterForm())
        return "auth/register"
    }

    @PostMapping("/register")
    fun register(
        @Valid @ModelAttribute("form") form: RegisterForm,
        bindingResult: BindingResult
    ): String {
        if (bindingResult.hasErrors()) {
            return "auth/register"
        }

        registerService.register(form)

        return "redirect:/login?registered"
    }

}
