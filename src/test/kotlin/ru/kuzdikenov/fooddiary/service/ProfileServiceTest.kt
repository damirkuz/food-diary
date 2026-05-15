package ru.kuzdikenov.fooddiary.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.kuzdikenov.fooddiary.entity.ActivityLevel
import ru.kuzdikenov.fooddiary.entity.Gender
import ru.kuzdikenov.fooddiary.entity.GoalEntity
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.entity.UserProfileEntity
import ru.kuzdikenov.fooddiary.repository.GoalRepository
import ru.kuzdikenov.fooddiary.repository.UserProfileRepository
import ru.kuzdikenov.fooddiary.repository.UserRepository
import ru.kuzdikenov.fooddiary.service.command.ProfileUpsertCommand
import java.time.LocalDate
import java.util.Optional

class ProfileServiceTest {

    private val userProfileRepository = Mockito.mock(UserProfileRepository::class.java)
    private val userRepository = Mockito.mock(UserRepository::class.java)
    private val goalRepository = Mockito.mock(GoalRepository::class.java)
    private val service = ProfileService(userProfileRepository, userRepository, goalRepository)

    @Test
    fun `creates profile when user has none`() {
        val user = UserEntity(id = 7, email = "user@example.com", passwordHash = "hash")
        val goal = goal(id = 1, name = "Maintain")
        Mockito.`when`(userRepository.findById(7L)).thenReturn(Optional.of(user))
        Mockito.`when`(goalRepository.findById(1L)).thenReturn(Optional.of(goal))
        Mockito.`when`(userProfileRepository.findByUserId(7L)).thenReturn(null)
        Mockito.`when`(userProfileRepository.save(Mockito.any(UserProfileEntity::class.java))).thenAnswer { it.getArgument(0) }

        val profile = service.updateProfile(7, command(goalId = 1))

        assertEquals(user, profile.user)
        assertEquals(goal, profile.goal)
        assertEquals(Gender.MALE, profile.gender)
        assertEquals(180, profile.heightCm)
    }

    @Test
    fun `updates existing profile`() {
        val user = UserEntity(id = 7, email = "user@example.com", passwordHash = "hash")
        val oldGoal = goal(id = 1, name = "Maintain")
        val newGoal = goal(id = 2, name = "Cut")
        val existing = UserProfileEntity(
            user = user,
            gender = Gender.MALE,
            birthDate = LocalDate.of(2000, 1, 1),
            heightCm = 180,
            weightKg = 80.0,
            activityLevel = ActivityLevel.MODERATE,
            goal = oldGoal
        )
        Mockito.`when`(userRepository.findById(7L)).thenReturn(Optional.of(user))
        Mockito.`when`(goalRepository.findById(2L)).thenReturn(Optional.of(newGoal))
        Mockito.`when`(userProfileRepository.findByUserId(7L)).thenReturn(existing)
        Mockito.`when`(userProfileRepository.save(Mockito.any(UserProfileEntity::class.java))).thenAnswer { it.getArgument(0) }

        val updated = service.updateProfile(7, command(goalId = 2, weightKg = 75.0))

        assertEquals(existing, updated)
        assertEquals(newGoal, updated.goal)
        assertEquals(75.0, updated.weightKg)
    }

    private fun command(goalId: Long, weightKg: Double = 80.0): ProfileUpsertCommand {
        return ProfileUpsertCommand(
            gender = Gender.MALE,
            birthDate = LocalDate.of(2000, 1, 1),
            heightCm = 180,
            weightKg = weightKg,
            activityLevel = ActivityLevel.MODERATE,
            goalId = goalId
        )
    }

    private fun goal(id: Long, name: String): GoalEntity {
        return GoalEntity(
            id = id,
            name = name,
            caloriesModifier = 1.0,
            proteinsRatio = 0.25,
            fatsRatio = 0.25,
            carbohydratesRatio = 0.5
        )
    }
}
