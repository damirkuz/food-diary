package ru.kuzdikenov.fooddiary.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.access.AccessDeniedException
import ru.kuzdikenov.fooddiary.entity.FoodEntryEntity
import ru.kuzdikenov.fooddiary.entity.ProductEntity
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.exception.ProductUsedInFoodEntriesException
import ru.kuzdikenov.fooddiary.repository.ProductRepository
import ru.kuzdikenov.fooddiary.service.command.ProductUpsertCommand
import java.util.Optional

class ProductDomainServiceTest {

    private val productRepository = Mockito.mock(ProductRepository::class.java)
    private val service = ProductDomainService(productRepository)

    @Test
    fun `allows accessible public product for another user`() {
        val product = product(ownerId = 10, isPublic = true)
        Mockito.`when`(productRepository.findById(1L)).thenReturn(Optional.of(product))

        assertEquals(product, service.getAccessibleProduct(1L, ownerId = 99))
    }

    @Test
    fun `rejects private product owned by another user`() {
        Mockito.`when`(productRepository.findById(1L)).thenReturn(Optional.of(product(ownerId = 10)))

        assertThrows(AccessDeniedException::class.java) {
            service.getAccessibleProduct(1L, ownerId = 99)
        }
    }

    @Test
    fun `rejects editing product owned by another user even when public`() {
        Mockito.`when`(productRepository.findById(1L)).thenReturn(Optional.of(product(ownerId = 10, isPublic = true)))

        assertThrows(AccessDeniedException::class.java) {
            service.getOwnedProduct(1L, ownerId = 99)
        }
    }

    @Test
    fun `applies upsert command to product`() {
        val product = product(ownerId = 10)

        service.applyUpsert(
            product,
            ProductUpsertCommand(
                name = "Rice",
                caloriesPer100g = 130.0,
                proteinsPer100g = 2.7,
                fatsPer100g = 0.3,
                carbohydratesPer100g = 28.2
            )
        )

        assertEquals("Rice", product.name)
        assertEquals(130.0, product.caloriesPer100g)
        assertEquals(2.7, product.proteinsPer100g)
        assertEquals(0.3, product.fatsPer100g)
        assertEquals(28.2, product.carbohydratesPer100g)
    }

    @Test
    fun `rejects deleting product used in food entries`() {
        val product = product(ownerId = 10)
        product.foodEntries.add(Mockito.mock(FoodEntryEntity::class.java))

        assertThrows(ProductUsedInFoodEntriesException::class.java) {
            service.requireNotUsed(product)
        }
    }

    @Test
    fun `calculates nutrition by grams`() {
        val calculation = service.calculateNutrition(product(ownerId = 10), grams = 250.0)

        assertEquals(250.0, calculation.grams)
        assertEquals(250.0, calculation.calculatedCalories)
        assertEquals(12.5, calculation.calculatedProteins)
        assertEquals(5.0, calculation.calculatedFats)
        assertEquals(50.0, calculation.calculatedCarbohydrates)
    }

    @Test
    fun `does not reject unused product`() {
        val product = product(ownerId = 10)

        service.requireNotUsed(product)

        assertFalse(product.foodEntries.isNotEmpty())
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
