package ru.kuzdikenov.fooddiary.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.access.AccessDeniedException
import ru.kuzdikenov.fooddiary.entity.FoodEntryEntity
import ru.kuzdikenov.fooddiary.entity.MealType
import ru.kuzdikenov.fooddiary.entity.ProductEntity
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.repository.FoodEntryRepository
import ru.kuzdikenov.fooddiary.repository.ProductRepository
import ru.kuzdikenov.fooddiary.repository.UserRepository
import ru.kuzdikenov.fooddiary.service.command.FoodEntryUpsertCommand
import java.time.LocalDate
import java.util.Optional

class FoodEntryServiceTest {

    private val foodEntryRepository = Mockito.mock(FoodEntryRepository::class.java)
    private val userRepository = Mockito.mock(UserRepository::class.java)
    private val productRepository = Mockito.mock(ProductRepository::class.java)
    private val productDomainService = ProductDomainService(productRepository)
    private val service = FoodEntryService(foodEntryRepository, userRepository, productDomainService)

    @Test
    fun `creates food entry with calculated nutrition`() {
        val user = UserEntity(id = 7, email = "user@example.com", passwordHash = "hash")
        val product = product(ownerId = 7)
        Mockito.`when`(userRepository.findById(7L)).thenReturn(Optional.of(user))
        Mockito.`when`(productRepository.findById(1L)).thenReturn(Optional.of(product))
        Mockito.`when`(foodEntryRepository.save(Mockito.any(FoodEntryEntity::class.java))).thenAnswer { it.getArgument(0) }

        val entry = service.createFoodEntry(command(grams = 250.0), ownerId = 7)

        assertEquals(LocalDate.of(2026, 5, 15), entry.entryDate)
        assertEquals(MealType.BREAKFAST, entry.mealType)
        assertEquals(250.0, entry.grams)
        assertEquals(250.0, entry.calories)
        assertEquals(12.5, entry.proteins)
        assertEquals(5.0, entry.fats)
        assertEquals(50.0, entry.carbohydrates)
    }

    @Test
    fun `rejects food entry owned by another user`() {
        val entry = FoodEntryEntity(
            id = 1,
            entryDate = LocalDate.of(2026, 5, 15),
            mealType = MealType.LUNCH,
            grams = 100.0,
            calories = 100.0,
            proteins = 5.0,
            fats = 2.0,
            carbohydrates = 20.0,
            user = UserEntity(id = 99, email = "other@example.com", passwordHash = "hash"),
            product = product(ownerId = 99)
        )
        Mockito.`when`(foodEntryRepository.findWithProductById(1L)).thenReturn(entry)

        assertThrows(AccessDeniedException::class.java) {
            service.getFoodEntryById(1L, ownerId = 7)
        }
    }

    @Test
    fun `calculates nutrition for accessible public product`() {
        Mockito.`when`(productRepository.findById(1L)).thenReturn(Optional.of(product(ownerId = 99, isPublic = true)))

        val calculation = service.calculateFoodEntryNutrition(productId = 1, grams = 150.0, ownerId = 7)

        assertEquals(150.0, calculation.calculatedCalories)
        assertEquals(7.5, calculation.calculatedProteins)
    }

    private fun command(grams: Double): FoodEntryUpsertCommand {
        return FoodEntryUpsertCommand(
            entryDate = LocalDate.of(2026, 5, 15),
            mealType = MealType.BREAKFAST,
            productId = 1,
            grams = grams
        )
    }

    private fun product(ownerId: Long, isPublic: Boolean = false): ProductEntity {
        return ProductEntity(
            id = 1,
            name = "Bread",
            caloriesPer100g = 100.0,
            proteinsPer100g = 5.0,
            fatsPer100g = 2.0,
            carbohydratesPer100g = 20.0,
            isPublic = isPublic,
            owner = UserEntity(id = ownerId, email = "owner$ownerId@example.com", passwordHash = "hash")
        )
    }
}
