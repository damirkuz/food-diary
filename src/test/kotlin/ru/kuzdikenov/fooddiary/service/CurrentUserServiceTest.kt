package ru.kuzdikenov.fooddiary.service

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.exception.UserNotFoundException
import ru.kuzdikenov.fooddiary.repository.UserRepository

class CurrentUserServiceTest {

    private val userRepository = Mockito.mock(UserRepository::class.java)
    private val service = CurrentUserService(userRepository)

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `returns current authenticated user id`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("user@example.com", "password")
        Mockito.`when`(userRepository.findByEmail("user@example.com"))
            .thenReturn(UserEntity(id = 7, email = "user@example.com", passwordHash = "hash"))

        assertEquals(7, service.getCurrentUserId())
    }

    @Test
    fun `throws when authentication is missing`() {
        assertThrows(AuthenticationCredentialsNotFoundException::class.java) {
            service.getCurrentUser()
        }
    }

    @Test
    fun `throws when authenticated user is missing in database`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("missing@example.com", "password")
        Mockito.`when`(userRepository.findByEmail("missing@example.com")).thenReturn(null)

        assertThrows(UserNotFoundException::class.java) {
            service.getCurrentUser()
        }
    }
}
