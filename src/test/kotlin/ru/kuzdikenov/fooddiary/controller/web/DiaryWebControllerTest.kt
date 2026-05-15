package ru.kuzdikenov.fooddiary.controller.web

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.PageImpl
import org.springframework.ui.ExtendedModelMap
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap
import ru.kuzdikenov.fooddiary.dto.DiaryTotals
import ru.kuzdikenov.fooddiary.entity.MealType
import ru.kuzdikenov.fooddiary.entity.ProductEntity
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.form.FoodEntryForm
import ru.kuzdikenov.fooddiary.service.CurrentUserService
import ru.kuzdikenov.fooddiary.service.FoodEntryService
import ru.kuzdikenov.fooddiary.service.NutritionNormService
import ru.kuzdikenov.fooddiary.service.ProductService
import ru.kuzdikenov.fooddiary.service.ProfileService
import java.time.LocalDate

class DiaryWebControllerTest {

    private val foodEntryService = Mockito.mock(FoodEntryService::class.java)
    private val currentUserService = Mockito.mock(CurrentUserService::class.java)
    private val productService = Mockito.mock(ProductService::class.java)
    private val profileService = Mockito.mock(ProfileService::class.java)
    private val nutritionNormService = Mockito.mock(NutritionNormService::class.java)
    private val controller = DiaryWebController(
        foodEntryService,
        currentUserService,
        productService,
        profileService,
        nutritionNormService
    )

    @Test
    fun `index adds diary data`() {
        val date = LocalDate.of(2026, 5, 15)
        val model = ExtendedModelMap()
        Mockito.`when`(currentUserService.getCurrentUserId()).thenReturn(7)
        Mockito.`when`(foodEntryService.getEntriesByDate(7, date)).thenReturn(emptyList())
        Mockito.`when`(foodEntryService.getTotalsByDate(7, date)).thenReturn(DiaryTotals(0.0, 0.0, 0.0, 0.0))
        Mockito.`when`(profileService.findByUserId(7)).thenReturn(null)
        Mockito.`when`(foodEntryService.getHighCalorieEntries(7, date)).thenReturn(emptyList())

        assertEquals("diary/index", controller.index(date, model))
        assertEquals(date, model["selectedDate"])
        assertNotNull(model["totals"])
    }

    @Test
    fun `new entry adds products and meal types`() {
        val model = ExtendedModelMap()
        Mockito.`when`(currentUserService.getCurrentUserId()).thenReturn(7)
        Mockito.`when`(productService.getCurrentUserProducts(7, null, 0, 1000, "name,asc"))
            .thenReturn(PageImpl(listOf(product())))

        assertEquals("diary/new", controller.newFoodEntry(LocalDate.of(2026, 5, 15), model))
        assertNotNull(model["products"])
        assertEquals(MealType.entries, model["mealTypes"])
    }

    @Test
    fun `create entry redirects to selected date`() {
        val form = FoodEntryForm(
            entryDate = LocalDate.of(2026, 5, 15),
            mealType = MealType.BREAKFAST,
            productId = 1,
            grams = 150.0
        )
        Mockito.`when`(currentUserService.getCurrentUserId()).thenReturn(7)

        val view = controller.createFoodEntry(
            form,
            BeanPropertyBindingResult(form, "form"),
            ExtendedModelMap(),
            RedirectAttributesModelMap()
        )

        assertEquals("redirect:/diary?date=2026-05-15", view)
    }

    private fun product(): ProductEntity {
        return ProductEntity(
            id = 1,
            name = "Apple",
            caloriesPer100g = 52.0,
            proteinsPer100g = 0.3,
            fatsPer100g = 0.2,
            carbohydratesPer100g = 14.0,
            owner = UserEntity(id = 7, email = "user@example.com", passwordHash = "hash")
        )
    }
}
