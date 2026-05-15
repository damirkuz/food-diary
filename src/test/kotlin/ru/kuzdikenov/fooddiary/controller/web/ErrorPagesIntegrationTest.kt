package ru.kuzdikenov.fooddiary.controller.web

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.kuzdikenov.fooddiary.config.SecurityConfig
import ru.kuzdikenov.fooddiary.entity.RoleEntity
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.exception.GlobalWebExceptionHandler
import ru.kuzdikenov.fooddiary.exception.ProductNotFoundException
import ru.kuzdikenov.fooddiary.repository.UserRepository
import ru.kuzdikenov.fooddiary.service.CurrentUserService
import ru.kuzdikenov.fooddiary.service.ProductService

@WebMvcTest(controllers = [ProductWebController::class])
@Import(SecurityConfig::class, GlobalWebExceptionHandler::class)
class ErrorPagesIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @MockitoBean
    lateinit var userRepository: UserRepository

    @MockitoBean
    lateinit var productService: ProductService

    @MockitoBean
    lateinit var currentUserService: CurrentUserService

    @Test
    @WithMockUser(username = "user@example.com", roles = ["USER"])
    fun `missing html page returns custom 404 with authenticated header`() {
        Mockito.`when`(userRepository.findByEmail("user@example.com"))
            .thenReturn(
                UserEntity(
                    id = 1,
                    email = "user@example.com",
                    passwordHash = "hash",
                    enabled = true,
                    roles = mutableSetOf(RoleEntity(id = 1, name = "ROLE_USER"))
                )
            )
        Mockito.`when`(currentUserService.getCurrentUserId()).thenReturn(1)
        Mockito.`when`(productService.getProductById(99L, 1L)).thenThrow(ProductNotFoundException())

        mockMvc.perform(get("/products/99").header("Accept", "text/html"))
            .andExpect(status().isNotFound)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Страница не найдена")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("user@example.com")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Whitelabel"))))
    }
}
