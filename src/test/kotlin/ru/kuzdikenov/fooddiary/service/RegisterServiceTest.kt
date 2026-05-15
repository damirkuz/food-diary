package ru.kuzdikenov.fooddiary.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.security.crypto.password.PasswordEncoder
import ru.kuzdikenov.fooddiary.entity.RoleEntity
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.exception.EmailAlreadyExistsException
import ru.kuzdikenov.fooddiary.form.RegisterForm
import ru.kuzdikenov.fooddiary.repository.RoleRepository
import ru.kuzdikenov.fooddiary.repository.UserRepository

class RegisterServiceTest {

    private val userRepository = Mockito.mock(UserRepository::class.java)
    private val roleRepository = Mockito.mock(RoleRepository::class.java)
    private val passwordEncoder = Mockito.mock(PasswordEncoder::class.java)
    private val service = RegisterService(userRepository, roleRepository, passwordEncoder)

    @Test
    fun `registers normalized user with role and encoded password`() {
        val role = RoleEntity(id = 1, name = "ROLE_USER")
        Mockito.`when`(userRepository.existsByEmail("user@example.com")).thenReturn(false)
        Mockito.`when`(roleRepository.findByName("ROLE_USER")).thenReturn(role)
        Mockito.`when`(passwordEncoder.encode("secret123")).thenReturn("encoded-password")

        service.register(RegisterForm(email = " User@Example.COM ", password = "secret123"))

        val captor = ArgumentCaptor.forClass(UserEntity::class.java)
        Mockito.verify(userRepository).save(captor.capture())
        assertEquals("user@example.com", captor.value.email)
        assertEquals("encoded-password", captor.value.passwordHash)
        assertTrue(captor.value.roles.contains(role))
    }

    @Test
    fun `rejects duplicate email`() {
        Mockito.`when`(userRepository.existsByEmail("user@example.com")).thenReturn(true)

        assertThrows(EmailAlreadyExistsException::class.java) {
            service.register(RegisterForm(email = "user@example.com", password = "secret123"))
        }

        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any(UserEntity::class.java))
    }
}
