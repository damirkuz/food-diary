package ru.kuzdikenov.fooddiary.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.kuzdikenov.fooddiary.entity.UserProfileEntity
import ru.kuzdikenov.fooddiary.exception.UserNotFoundException
import ru.kuzdikenov.fooddiary.repository.GoalRepository
import ru.kuzdikenov.fooddiary.repository.UserProfileRepository
import ru.kuzdikenov.fooddiary.repository.UserRepository
import ru.kuzdikenov.fooddiary.service.command.ProfileUpsertCommand
import java.time.LocalDate
import java.time.Period

@Service
class ProfileService(
    private val userProfileRepository: UserProfileRepository,
    private val userRepository: UserRepository,
    private val goalRepository: GoalRepository,
) {

    @Transactional(readOnly = true)
    fun findByUserId(userId: Long): UserProfileEntity? {
        return userProfileRepository.findByUserId(userId)
    }

    @Transactional
    fun updateProfile(userId: Long, command: ProfileUpsertCommand): UserProfileEntity {
        validate(command)

        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        val goal = goalRepository.findById(command.goalId)
            .orElseThrow { IllegalArgumentException("Goal not found") }

        val profile = userProfileRepository.findByUserId(userId)
            ?: UserProfileEntity(
                user = user,
                gender = command.gender,
                birthDate = command.birthDate,
                heightCm = command.heightCm,
                weightKg = command.weightKg,
                activityLevel = command.activityLevel,
                goal = goal
            )

        profile.gender = command.gender
        profile.birthDate = command.birthDate
        profile.heightCm = command.heightCm
        profile.weightKg = command.weightKg
        profile.activityLevel = command.activityLevel
        profile.goal = goal

        return userProfileRepository.save(profile)
    }

    private fun validate(command: ProfileUpsertCommand) {
        val age = Period.between(command.birthDate, LocalDate.now()).years
        require(age in MIN_AGE..MAX_AGE) { "Возраст должен быть от $MIN_AGE до $MAX_AGE лет" }
        require(command.heightCm in MIN_HEIGHT_CM..MAX_HEIGHT_CM) {
            "Рост должен быть от $MIN_HEIGHT_CM до $MAX_HEIGHT_CM см"
        }
        require(command.weightKg in MIN_WEIGHT_KG..MAX_WEIGHT_KG) {
            "Вес должен быть от $MIN_WEIGHT_KG до $MAX_WEIGHT_KG кг"
        }
    }

    companion object {
        private const val MIN_AGE = 1
        private const val MAX_AGE = 120
        private const val MIN_HEIGHT_CM = 50
        private const val MAX_HEIGHT_CM = 250
        private const val MIN_WEIGHT_KG = 2.0
        private const val MAX_WEIGHT_KG = 500.0
    }
}
