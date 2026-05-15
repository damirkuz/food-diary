package ru.kuzdikenov.fooddiary.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.kuzdikenov.fooddiary.entity.GoalEntity
import ru.kuzdikenov.fooddiary.repository.GoalRepository

class GoalServiceTest {

    private val goalRepository = Mockito.mock(GoalRepository::class.java)
    private val service = GoalService(goalRepository)

    @Test
    fun `returns goals from repository`() {
        val goals = listOf(
            GoalEntity(name = "Maintain", caloriesModifier = 1.0, proteinsRatio = 0.25, fatsRatio = 0.25, carbohydratesRatio = 0.5),
            GoalEntity(name = "Cut", caloriesModifier = 0.85, proteinsRatio = 0.3, fatsRatio = 0.25, carbohydratesRatio = 0.45)
        )
        Mockito.`when`(goalRepository.findAll()).thenReturn(goals)

        assertEquals(goals, service.getGoals())
    }
}
