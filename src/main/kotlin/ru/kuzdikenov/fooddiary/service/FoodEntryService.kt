package ru.kuzdikenov.fooddiary.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.kuzdikenov.fooddiary.dto.DiaryTotals
import ru.kuzdikenov.fooddiary.dto.FoodEntryCalculation
import ru.kuzdikenov.fooddiary.entity.FoodEntryEntity
import ru.kuzdikenov.fooddiary.entity.ProductEntity
import ru.kuzdikenov.fooddiary.exception.FoodEntryNotFoundException
import ru.kuzdikenov.fooddiary.exception.ProductNotFoundException
import ru.kuzdikenov.fooddiary.exception.UserNotFoundException
import ru.kuzdikenov.fooddiary.repository.FoodEntryRepository
import ru.kuzdikenov.fooddiary.repository.ProductRepository
import ru.kuzdikenov.fooddiary.repository.UserRepository
import ru.kuzdikenov.fooddiary.service.command.FoodEntryUpsertCommand
import java.time.LocalDate

@Service
class FoodEntryService (
    private val foodEntryRepository: FoodEntryRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
) {

    @Transactional
    fun createFoodEntry(command: FoodEntryUpsertCommand, ownerId: Long): FoodEntryEntity {
        val user = userRepository.findById(ownerId).orElseThrow { UserNotFoundException() }
        val product = getProductForOwner(command.productId, ownerId)
        val foodEntryCalculation = calculateNutrition(product, command.grams)

        return foodEntryRepository.save(
            FoodEntryEntity(
                entryDate = command.entryDate,
                mealType = command.mealType,
                grams = command.grams,
                calories = foodEntryCalculation.calculatedCalories,
                proteins = foodEntryCalculation.calculatedProteins,
                fats = foodEntryCalculation.calculatedFats,
                carbohydrates = foodEntryCalculation.calculatedCarbohydrates,
                user = user,
                product = product
            )
        )
    }

    @Transactional
    fun updateFoodEntry(id: Long, command: FoodEntryUpsertCommand, ownerId: Long): FoodEntryEntity {
        val foodEntry = getFoodEntryForOwner(id, ownerId)
        val product = getProductForOwner(command.productId, ownerId)
        val foodEntryCalculation = calculateNutrition(product, command.grams)

        foodEntry.entryDate = command.entryDate
        foodEntry.mealType = command.mealType
        foodEntry.grams = command.grams
        foodEntry.calories = foodEntryCalculation.calculatedCalories
        foodEntry.proteins = foodEntryCalculation.calculatedProteins
        foodEntry.fats = foodEntryCalculation.calculatedFats
        foodEntry.carbohydrates = foodEntryCalculation.calculatedCarbohydrates
        foodEntry.product = product

        return foodEntryRepository.save(foodEntry)
    }

    @Transactional
    fun deleteFoodEntry(id: Long, ownerId: Long) {
        foodEntryRepository.delete(getFoodEntryForOwner(id, ownerId))
    }

    @Transactional(readOnly = true)
    fun getEntriesByDate(ownerId: Long, entryDate: LocalDate): List<FoodEntryEntity> {
        return foodEntryRepository.findAllByUserIdAndEntryDateOrderByMealTypeAscCreatedAtAsc(ownerId, entryDate)
    }

    @Transactional(readOnly = true)
    fun getFoodEntryById(id: Long, ownerId: Long): FoodEntryEntity {
        return getFoodEntryForOwner(id, ownerId)
    }

    @Transactional(readOnly = true)
    fun getTotalsByDate(ownerId: Long, entryDate: LocalDate): DiaryTotals {
        return foodEntryRepository.sumNutritionByUserIdAndEntryDate(ownerId, entryDate)
    }

    @Transactional(readOnly = true)
    fun getHighCalorieEntries(ownerId: Long, entryDate: LocalDate): List<FoodEntryEntity> {
        return foodEntryRepository.findHighCalorieEntriesByUserIdAndEntryDate(ownerId, entryDate)
    }

    @Transactional(readOnly = true)
    fun calculateFoodEntryNutrition(productId: Long, grams: Double, ownerId: Long): FoodEntryCalculation {
        return calculateNutrition(getProductForOwner(productId, ownerId), grams)
    }

    private fun getFoodEntryForOwner(id: Long, ownerId: Long): FoodEntryEntity {
        val foodEntry = foodEntryRepository.findWithProductById(id) ?: throw FoodEntryNotFoundException()
        val foodEntryOwnerId = foodEntry.user.id ?: throw UserNotFoundException()

        if (foodEntryOwnerId != ownerId) {
            throw AccessDeniedException("User has no access to this food entry")
        }

        return foodEntry
    }

    private fun getProductForOwner(id: Long, ownerId: Long): ProductEntity {
        val product = productRepository.findById(id).orElseThrow { ProductNotFoundException() }
        val productOwnerId = product.owner.id ?: throw UserNotFoundException()

        if (productOwnerId != ownerId) {
            throw AccessDeniedException("User has no access to this product")
        }

        return product
    }

    private fun calculateNutrition(product: ProductEntity, grams: Double): FoodEntryCalculation {
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
