package ru.kuzdikenov.fooddiary.service

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.exception.UserNotFoundException
import ru.kuzdikenov.fooddiary.repository.UserRepository

@Service
class CurrentUserService (
    private val userRepository: UserRepository,
) {

    fun getCurrentUser(): UserEntity {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw AuthenticationCredentialsNotFoundException("User is not authenticated")

        val email = authentication.name

        return userRepository.findByEmail(email) ?: throw UserNotFoundException()
    }

    fun getCurrentUserId(): Long {
        return getCurrentUser().id ?: throw UserNotFoundException()
    }

}
