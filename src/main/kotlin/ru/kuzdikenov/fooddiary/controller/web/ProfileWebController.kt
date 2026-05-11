package ru.kuzdikenov.fooddiary.controller.web

import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import ru.kuzdikenov.fooddiary.entity.ActivityLevel
import ru.kuzdikenov.fooddiary.entity.Gender
import ru.kuzdikenov.fooddiary.form.ProfileForm
import ru.kuzdikenov.fooddiary.service.CurrentUserService
import ru.kuzdikenov.fooddiary.service.GoalService
import ru.kuzdikenov.fooddiary.service.NutritionNormService
import ru.kuzdikenov.fooddiary.service.ProfileService

@Controller
@RequestMapping("/profile")
class ProfileWebController(
    private val profileService: ProfileService,
    private val currentUserService: CurrentUserService,
    private val goalService: GoalService,
    private val nutritionNormService: NutritionNormService,
) {

    @GetMapping
    fun index(
        model: Model
    ): String {
        val ownerId = currentUserService.getCurrentUserId()
        val profile = profileService.findByUserId(ownerId)
        model.addAttribute("form", profile?.let { ProfileForm.fromProfile(it) } ?: ProfileForm())
        val norm = profile?.let { nutritionNormService.calculate(it) }
        model.addAttribute("nutritionNorm", norm)
        addConstants(model)
        return "profile"
    }

    @PostMapping
    fun createProfile(
        @Valid @ModelAttribute("form") form: ProfileForm,
        bindingResult: BindingResult,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {

        if (bindingResult.hasErrors()) {
            addConstants(model)
            return "profile"
        }

        val userId = currentUserService.getCurrentUserId()
        profileService.updateProfile(userId, form.toCommand())

        redirectAttributes.addFlashAttribute("successMessage", "Профиль сохранён")
        return "redirect:/profile"
    }

    private fun addConstants(model: Model) {
        val goals = goalService.getGoals()
        model.addAttribute("genders", Gender.entries)
        model.addAttribute("activityLevels", ActivityLevel.entries)
        model.addAttribute("goals", goals)
    }

}
