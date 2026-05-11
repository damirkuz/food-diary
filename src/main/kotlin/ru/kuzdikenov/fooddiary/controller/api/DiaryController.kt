package ru.kuzdikenov.fooddiary.controller.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import ru.kuzdikenov.api.DiaryApi
import ru.kuzdikenov.api.dto.DiaryDayResponse
import ru.kuzdikenov.api.dto.FoodEntryCalculationRequest
import ru.kuzdikenov.api.dto.FoodEntryCalculationResponse
import ru.kuzdikenov.api.dto.FoodEntryCreateRequest
import ru.kuzdikenov.api.dto.FoodEntryResponse
import ru.kuzdikenov.api.dto.FoodEntryUpdateRequest
import ru.kuzdikenov.fooddiary.mapper.FoodEntryMapper
import ru.kuzdikenov.fooddiary.service.CurrentUserService
import ru.kuzdikenov.fooddiary.service.FoodEntryService
import ru.kuzdikenov.fooddiary.service.NutritionNormService
import ru.kuzdikenov.fooddiary.service.ProfileService
import java.time.LocalDate

@RestController
class DiaryController (
    private val foodEntryService: FoodEntryService,
    private val foodEntryMapper: FoodEntryMapper,
    private val currentUserService: CurrentUserService,
    private val profileService: ProfileService,
    private val nutritionNormService: NutritionNormService,
) : DiaryApi {
    override fun calculateFoodEntryNutrition(foodEntryCalculationRequest: FoodEntryCalculationRequest): ResponseEntity<FoodEntryCalculationResponse> {
        val ownerId = currentUserService.getCurrentUserId()
        val foodEntryCalculation = foodEntryService.calculateFoodEntryNutrition(
            productId = foodEntryCalculationRequest.productId,
            grams = foodEntryCalculationRequest.grams,
            ownerId = ownerId
        )

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(foodEntryMapper.toResponse(foodEntryCalculation))
    }

    override fun createFoodEntry(foodEntryCreateRequest: FoodEntryCreateRequest): ResponseEntity<FoodEntryResponse> {
        val ownerId = currentUserService.getCurrentUserId()
        val foodEntry = foodEntryService.createFoodEntry(foodEntryMapper.toCommand(foodEntryCreateRequest), ownerId)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(foodEntryMapper.toResponse(foodEntry))
    }

    override fun deleteFoodEntry(id: Long): ResponseEntity<Unit> {
        val ownerId = currentUserService.getCurrentUserId()
        foodEntryService.deleteFoodEntry(id, ownerId)
        return ResponseEntity.noContent().build()
    }

    override fun getDiaryByDate(date: LocalDate): ResponseEntity<DiaryDayResponse> {
        val ownerId = currentUserService.getCurrentUserId()
        val entries = foodEntryService.getEntriesByDate(ownerId, date)
        val totals = foodEntryService.getTotalsByDate(ownerId, date)
        val dailyNorm = profileService.findByUserId(ownerId)
            ?.let { nutritionNormService.calculate(it) }

        return ResponseEntity.ok(
            foodEntryMapper.toDiaryDayResponse(
                date = date,
                entries = entries,
                totals = totals,
                dailyNorm = dailyNorm
            )
        )
    }

    override fun getHighCalorieEntries(date: LocalDate): ResponseEntity<List<FoodEntryResponse>> {
        val ownerId = currentUserService.getCurrentUserId()
        val entries = foodEntryService.getHighCalorieEntries(ownerId, date)
        return ResponseEntity.ok(entries.map(foodEntryMapper::toResponse))
    }

    override fun updateFoodEntry(
        id: Long,
        foodEntryUpdateRequest: FoodEntryUpdateRequest
    ): ResponseEntity<FoodEntryResponse> {
        val ownerId = currentUserService.getCurrentUserId()
        val foodEntry = foodEntryService.updateFoodEntry(
            id = id,
            command = foodEntryMapper.toCommand(foodEntryUpdateRequest),
            ownerId = ownerId
        )

        return ResponseEntity.ok(foodEntryMapper.toResponse(foodEntry))
    }
}
