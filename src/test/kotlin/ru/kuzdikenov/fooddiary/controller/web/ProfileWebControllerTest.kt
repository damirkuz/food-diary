package ru.kuzdikenov.fooddiary.controller.web

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.ui.ExtendedModelMap
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap
import ru.kuzdikenov.fooddiary.form.ProfileForm
import ru.kuzdikenov.fooddiary.service.CurrentUserService
import ru.kuzdikenov.fooddiary.service.GoalService
import ru.kuzdikenov.fooddiary.service.NutritionNormService
import ru.kuzdikenov.fooddiary.service.ProfileService

class ProfileWebControllerTest {

    private val profileService = Mockito.mock(ProfileService::class.java)
    private val currentUserService = Mockito.mock(CurrentUserService::class.java)
    private val goalService = Mockito.mock(GoalService::class.java)
    private val nutritionNormService = Mockito.mock(NutritionNormService::class.java)
    private val controller = ProfileWebController(profileService, currentUserService, goalService, nutritionNormService)

    @Test
    fun `index adds empty form and constants`() {
        val model = ExtendedModelMap()
        Mockito.`when`(currentUserService.getCurrentUserId()).thenReturn(7)
        Mockito.`when`(profileService.findByUserId(7)).thenReturn(null)
        Mockito.`when`(goalService.getGoals()).thenReturn(emptyList())

        assertEquals("profile", controller.index(model))
        assertNotNull(model["form"])
        assertNotNull(model["genders"])
        assertNotNull(model["activityLevels"])
        assertEquals(emptyList<Any>(), model["goals"])
    }

    @Test
    fun `profile validation errors return profile page`() {
        val form = ProfileForm()
        val bindingResult = BeanPropertyBindingResult(form, "form")
        bindingResult.rejectValue("heightCm", "required")
        Mockito.`when`(goalService.getGoals()).thenReturn(emptyList())

        assertEquals("profile", controller.createProfile(form, bindingResult, ExtendedModelMap(), RedirectAttributesModelMap()))
    }
}
