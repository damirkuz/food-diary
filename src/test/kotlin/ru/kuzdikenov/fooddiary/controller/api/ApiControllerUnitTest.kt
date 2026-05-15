package ru.kuzdikenov.fooddiary.controller.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.PageImpl
import org.springframework.http.HttpStatus
import ru.kuzdikenov.api.dto.FoodEntryCalculationRequest
import ru.kuzdikenov.api.dto.FoodEntryCreateRequest
import ru.kuzdikenov.api.dto.FoodEntryUpdateRequest
import ru.kuzdikenov.api.dto.MealType
import ru.kuzdikenov.api.dto.NutritionLookupResponse
import ru.kuzdikenov.api.dto.ProductUpdateRequest
import ru.kuzdikenov.fooddiary.dto.DiaryTotals
import ru.kuzdikenov.fooddiary.dto.FoodEntryCalculation
import ru.kuzdikenov.fooddiary.dto.ProductLookup
import ru.kuzdikenov.fooddiary.entity.FoodEntryEntity
import ru.kuzdikenov.fooddiary.entity.ProductEntity
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.mapper.FoodEntryMapper
import ru.kuzdikenov.fooddiary.mapper.ProductMapper
import ru.kuzdikenov.fooddiary.service.CurrentUserService
import ru.kuzdikenov.fooddiary.service.FoodEntryService
import ru.kuzdikenov.fooddiary.service.NutritionLookupService
import ru.kuzdikenov.fooddiary.service.NutritionNormService
import ru.kuzdikenov.fooddiary.service.ProductService
import ru.kuzdikenov.fooddiary.service.ProfileService
import ru.kuzdikenov.fooddiary.service.command.FoodEntryUpsertCommand
import ru.kuzdikenov.fooddiary.service.command.ProductUpsertCommand
import java.time.LocalDate

class ApiControllerUnitTest {

    private val currentUserService = Mockito.mock(CurrentUserService::class.java)

    @Test
    fun `product api returns public flag in page response`() {
        val productService = Mockito.mock(ProductService::class.java)
        val controller = ProductController(productService, currentUserService, ProductMapper())
        Mockito.`when`(currentUserService.getCurrentUserId()).thenReturn(7)
        Mockito.`when`(productService.getCurrentUserProducts(7, null, 0, 20, "createdAt,desc"))
            .thenReturn(PageImpl(listOf(product(isPublic = true))))

        val response = controller.getCurrentUserProducts(null, 0, 20, "createdAt,desc")

        assertEquals(HttpStatus.OK, response.statusCode)
        assertTrue(response.body!!.content.first().isPublic)
    }

    @Test
    fun `product api update maps request and response`() {
        val productService = Mockito.mock(ProductService::class.java)
        val controller = ProductController(productService, currentUserService, ProductMapper())
        Mockito.`when`(currentUserService.getCurrentUserId()).thenReturn(7)
        Mockito.`when`(productService.updateProduct(Mockito.eq(1L), Mockito.eq(7L), anyProductCommand()))
            .thenReturn(product(name = "Updated", isPublic = false))

        val response = controller.updateProduct(
            1,
            ProductUpdateRequest(
                name = "Updated",
                caloriesPer100g = 60.0,
                proteinsPer100g = 1.0,
                fatsPer100g = 1.0,
                carbohydratesPer100g = 12.0
            )
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Updated", response.body!!.name)
        assertFalse(response.body!!.isPublic)
    }

    @Test
    fun `diary api calculates nutrition`() {
        val foodEntryService = Mockito.mock(FoodEntryService::class.java)
        val controller = diaryController(foodEntryService)
        Mockito.`when`(currentUserService.getCurrentUserId()).thenReturn(7)
        Mockito.`when`(foodEntryService.calculateFoodEntryNutrition(1, 250.0, 7))
            .thenReturn(FoodEntryCalculation(250.0, 130.0, 0.75, 0.5, 35.0))

        val response = controller.calculateFoodEntryNutrition(FoodEntryCalculationRequest(productId = 1, grams = 250.0))

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(130.0, response.body!!.calculatedCalories)
    }

    @Test
    fun `diary api creates and deletes entries`() {
        val foodEntryService = Mockito.mock(FoodEntryService::class.java)
        val controller = diaryController(foodEntryService)
        Mockito.`when`(currentUserService.getCurrentUserId()).thenReturn(7)
        Mockito.`when`(foodEntryService.createFoodEntry(anyFoodEntryCommand(), Mockito.eq(7L)))
            .thenReturn(foodEntry())

        val createResponse = controller.createFoodEntry(
            FoodEntryCreateRequest(
                entryDate = LocalDate.of(2026, 5, 15),
                mealType = MealType.BREAKFAST,
                productId = 1,
                grams = 150.0
            )
        )
        val deleteResponse = controller.deleteFoodEntry(1)

        assertEquals(HttpStatus.CREATED, createResponse.statusCode)
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.statusCode)
    }

    @Test
    fun `diary api returns diary by date`() {
        val foodEntryService = Mockito.mock(FoodEntryService::class.java)
        val controller = diaryController(foodEntryService)
        val date = LocalDate.of(2026, 5, 15)
        Mockito.`when`(currentUserService.getCurrentUserId()).thenReturn(7)
        Mockito.`when`(foodEntryService.getEntriesByDate(7, date)).thenReturn(listOf(foodEntry()))
        Mockito.`when`(foodEntryService.getTotalsByDate(7, date)).thenReturn(DiaryTotals(100.0, 5.0, 2.0, 20.0))

        val response = controller.getDiaryByDate(date)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body!!.propertyEntries.size)
        assertEquals(100.0, response.body!!.totals.calories)
    }

    @Test
    fun `nutrition lookup api maps service response`() {
        val nutritionLookupService = Mockito.mock(NutritionLookupService::class.java)
        val controller = NutritionLookupController(nutritionLookupService)
        Mockito.`when`(nutritionLookupService.lookupProductNutrition("banana"))
            .thenReturn(
                ProductLookup(
                    name = "banana",
                    caloriesPer100g = 89.0,
                    proteinsPer100g = 1.1,
                    fatsPer100g = 0.3,
                    carbohydratesPer100g = 22.8,
                    source = NutritionLookupResponse.Source.EXTERNAL_API,
                    cached = false
                )
            )

        val response = controller.lookupProductNutrition("banana")

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("banana", response.body!!.name)
        assertEquals(NutritionLookupResponse.Source.EXTERNAL_API, response.body!!.source)
    }

    private fun diaryController(foodEntryService: FoodEntryService): DiaryController {
        return DiaryController(
            foodEntryService = foodEntryService,
            foodEntryMapper = FoodEntryMapper(),
            currentUserService = currentUserService,
            profileService = Mockito.mock(ProfileService::class.java),
            nutritionNormService = Mockito.mock(NutritionNormService::class.java)
        )
    }

    private fun anyProductCommand(): ProductUpsertCommand {
        return Mockito.any(ProductUpsertCommand::class.java) ?: ProductUpsertCommand(
            name = "Updated",
            caloriesPer100g = 60.0,
            proteinsPer100g = 1.0,
            fatsPer100g = 1.0,
            carbohydratesPer100g = 12.0
        )
    }

    private fun anyFoodEntryCommand(): FoodEntryUpsertCommand {
        return Mockito.any(FoodEntryUpsertCommand::class.java) ?: FoodEntryUpsertCommand(
            entryDate = LocalDate.of(2026, 5, 15),
            mealType = ru.kuzdikenov.fooddiary.entity.MealType.BREAKFAST,
            productId = 1,
            grams = 150.0
        )
    }

    private fun product(name: String = "Apple", isPublic: Boolean): ProductEntity {
        return ProductEntity(
            id = 1,
            name = name,
            caloriesPer100g = 52.0,
            proteinsPer100g = 0.3,
            fatsPer100g = 0.2,
            carbohydratesPer100g = 14.0,
            isPublic = isPublic,
            owner = UserEntity(id = 7, email = "user@example.com", passwordHash = "hash")
        )
    }

    private fun foodEntry(): FoodEntryEntity {
        return FoodEntryEntity(
            id = 1,
            entryDate = LocalDate.of(2026, 5, 15),
            mealType = ru.kuzdikenov.fooddiary.entity.MealType.BREAKFAST,
            grams = 150.0,
            calories = 100.0,
            proteins = 5.0,
            fats = 2.0,
            carbohydrates = 20.0,
            user = UserEntity(id = 7, email = "user@example.com", passwordHash = "hash"),
            product = product(isPublic = false)
        )
    }
}
