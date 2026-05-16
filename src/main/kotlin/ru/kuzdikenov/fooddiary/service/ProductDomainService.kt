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
        validateUpsert(command)

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
        require(grams in MIN_GRAMS..MAX_GRAMS) {
            "Масса должна быть от $MIN_GRAMS до $MAX_GRAMS г"
        }

        val multiplier = grams / 100.0
        return FoodEntryCalculation(
            grams = grams,
            calculatedCalories = product.caloriesPer100g * multiplier,
            calculatedProteins = product.proteinsPer100g * multiplier,
            calculatedFats = product.fatsPer100g * multiplier,
            calculatedCarbohydrates = product.carbohydratesPer100g * multiplier,
        )
    }

    fun validateUpsert(command: ProductUpsertCommand) {
        require(command.name.isNotBlank()) { "Название продукта обязательно" }
        require(command.name.length <= MAX_PRODUCT_NAME_LENGTH) {
            "Название продукта должно быть не длиннее $MAX_PRODUCT_NAME_LENGTH символов"
        }
        require(command.caloriesPer100g in MIN_NUTRITION_VALUE..MAX_CALORIES_PER_100G) {
            "Калорийность должна быть от $MIN_NUTRITION_VALUE до $MAX_CALORIES_PER_100G ккал"
        }
        require(command.proteinsPer100g in MIN_NUTRITION_VALUE..MAX_MACRO_PER_100G) {
            "Белки должны быть от $MIN_NUTRITION_VALUE до $MAX_MACRO_PER_100G г"
        }
        require(command.fatsPer100g in MIN_NUTRITION_VALUE..MAX_MACRO_PER_100G) {
            "Жиры должны быть от $MIN_NUTRITION_VALUE до $MAX_MACRO_PER_100G г"
        }
        require(command.carbohydratesPer100g in MIN_NUTRITION_VALUE..MAX_MACRO_PER_100G) {
            "Углеводы должны быть от $MIN_NUTRITION_VALUE до $MAX_MACRO_PER_100G г"
        }
    }

    companion object {
        const val MIN_GRAMS = 0.1
        const val MAX_GRAMS = 10000.0
        private const val MAX_PRODUCT_NAME_LENGTH = 255
        private const val MIN_NUTRITION_VALUE = 0.0
        private const val MAX_CALORIES_PER_100G = 1000.0
        private const val MAX_MACRO_PER_100G = 100.0
    }
}
