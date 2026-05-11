package ru.kuzdikenov.fooddiary.service

import org.springframework.stereotype.Service
import ru.kuzdikenov.fooddiary.entity.GoalEntity
import ru.kuzdikenov.fooddiary.repository.GoalRepository

@Service
class GoalService (
    private val goalRepository: GoalRepository,
) {
    fun getGoals(): List<GoalEntity> {
        return goalRepository.findAll()
    }
}