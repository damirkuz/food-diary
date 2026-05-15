package ru.kuzdikenov.fooddiary.controller.web

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.kuzdikenov.fooddiary.config.SecurityConfig
import ru.kuzdikenov.fooddiary.entity.RoleEntity
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.repository.UserRepository
import ru.kuzdikenov.fooddiary.security.CustomUserDetailsService
import ru.kuzdikenov.fooddiary.service.RegisterService
import ru.kuzdikenov.fooddiary.validator.RegisterFormValidator

@WebMvcTest(controllers = [AuthController::class])
@Import(SecurityConfig::class, CustomUserDetailsService::class)
class AuthSecurityIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @MockitoBean
    lateinit var registerService: RegisterService

    @MockitoBean
    lateinit var registerFormValidator: RegisterFormValidator

    @MockitoBean
    lateinit var userRepository: UserRepository

    @Test
    fun `login page contains csrf token`() {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")))
    }

    @Test
    fun `login with csrf redirects to profile`() {
        val passwordHash = requireNotNull(BCryptPasswordEncoder().encode("secret123"))
        Mockito.`when`(userRepository.findByEmail("user@example.com"))
            .thenReturn(user("user@example.com", passwordHash, enabled = true, roles = setOf("ROLE_USER")))

        mockMvc.perform(
            post("/login")
                .with(csrf())
                .param("email", "user@example.com")
                .param("password", "secret123")
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/profile"))
    }

    private fun user(email: String, passwordHash: String, enabled: Boolean, roles: Set<String>): UserEntity {
        return UserEntity(
            id = 1,
            email = email,
            passwordHash = passwordHash,
            enabled = enabled,
            roles = roles.mapIndexed { index, role -> RoleEntity(id = index.toLong() + 1, name = role) }.toMutableSet()
        )
    }
}
