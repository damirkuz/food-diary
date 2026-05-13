package ru.kuzdikenov.fooddiary.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import ru.kuzdikenov.fooddiary.dto.FoodEntryCalculation
import ru.kuzdikenov.fooddiary.entity.ProductEntity
import ru.kuzdikenov.fooddiary.exception.ProductNotFoundException
import ru.kuzdikenov.fooddiary.exception.ProductUsedInFoodEntriesException
import ru.kuzdikenov.fooddiary.exception.UserNotFoundException
import ru.kuzdikenov.fooddiary.repository.ProductRepository
import ru.kuzdikenov.fooddiary.service.command.ProductUpsertCommand

@Service
class ProductDomainService(
    private val productRepository: ProductRepository,
) {

    fun getProduct(id: Long): ProductEntity {
        return productRepository.findById(id).orElseThrow { ProductNotFoundException() }
    }

    fun getAccessibleProduct(id: Long, ownerId: Long): ProductEntity {
        val product = getProduct(id)
        val productOwnerId = product.owner.id ?: throw UserNotFoundException()

        if (!product.isPublic && productOwnerId != ownerId) {
            throw AccessDeniedException("User has no access to this product")
        }

        return product
    }

    fun getOwnedProduct(id: Long, ownerId: Long): ProductEntity {
        val product = getProduct(id)
        val productOwnerId = product.owner.id ?: throw UserNotFoundException()

        if (productOwnerId != ownerId) {
            throw AccessDeniedException("User has no access to this product")
        }

        return product
    }

    fun applyUpsert(product: ProductEntity, command: ProductUpsertCommand) {
        product.name = command.name
        product.caloriesPer100g = command.caloriesPer100g
        product.proteinsPer100g = command.proteinsPer100g
        product.fatsPer100g = command.fatsPer100g
        product.carbohydratesPer100g = command.carbohydratesPer100g
    }

    fun requireNotUsed(product: ProductEntity) {
        if (product.foodEntries.isNotEmpty()) {
            throw ProductUsedInFoodEntriesException()
        }
    }

    fun calculateNutrition(product: ProductEntity, grams: Double): FoodEntryCalculation {
        val multiplier = grams / 100.0
        return FoodEntryCalculation(
            grams = grams,
            calculatedCalories = product.caloriesPer100g * multiplier,
            calculatedProteins = product.proteinsPer100g * multiplier,
            calculatedFats = product.fatsPer100g * multiplier,
            calculatedCarbohydrates = product.carbohydratesPer100g * multiplier,
        )
    }
}
