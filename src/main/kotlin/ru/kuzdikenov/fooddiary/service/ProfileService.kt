package ru.kuzdikenov.fooddiary.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.kuzdikenov.fooddiary.entity.UserProfileEntity
import ru.kuzdikenov.fooddiary.exception.UserNotFoundException
import ru.kuzdikenov.fooddiary.repository.GoalRepository
import ru.kuzdikenov.fooddiary.repository.UserProfileRepository
import ru.kuzdikenov.fooddiary.repository.UserRepository
import ru.kuzdikenov.fooddiary.service.command.ProfileUpsertCommand

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
}
