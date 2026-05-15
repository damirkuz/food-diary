package ru.kuzdikenov.fooddiary.controller.web

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.kuzdikenov.fooddiary.config.SecurityConfig
import ru.kuzdikenov.fooddiary.entity.RoleEntity
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.repository.UserRepository
import ru.kuzdikenov.fooddiary.service.AdminService
import ru.kuzdikenov.fooddiary.service.CurrentUserService

@WebMvcTest(controllers = [AdminWebController::class])
@Import(SecurityConfig::class)
class AdminSecurityIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @MockitoBean
    lateinit var adminService: AdminService

    @MockitoBean
    lateinit var currentUserService: CurrentUserService

    @MockitoBean
    lateinit var userRepository: UserRepository

    @Test
    @WithMockUser(username = "admin@example.com", roles = ["USER"])
    fun `database admin role grants access without relogin`() {
        Mockito.`when`(userRepository.findByEmail("admin@example.com"))
            .thenReturn(user(enabled = true, roles = setOf("ROLE_USER", "ROLE_ADMIN")))
        Mockito.`when`(currentUserService.getCurrentUserId()).thenReturn(1)
        Mockito.`when`(adminService.getUsers(null, 0, 20)).thenReturn(PageImpl(emptyList()))

        mockMvc.perform(get("/admin/users"))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Пользователи")))
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = ["ADMIN"])
    fun `blocked admin cannot access admin pages`() {
        Mockito.`when`(userRepository.findByEmail("admin@example.com"))
            .thenReturn(user(enabled = false, roles = setOf("ROLE_ADMIN")))

        mockMvc.perform(get("/admin/users"))
            .andExpect(status().isForbidden)
    }

    private fun user(enabled: Boolean, roles: Set<String>): UserEntity {
        return UserEntity(
            id = 1,
            email = "admin@example.com",
            passwordHash = "hash",
            enabled = enabled,
            roles = roles.mapIndexed { index, role -> RoleEntity(id = index.toLong() + 1, name = role) }.toMutableSet()
        )
    }
}
