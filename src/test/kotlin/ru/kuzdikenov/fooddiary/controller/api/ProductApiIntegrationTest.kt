package ru.kuzdikenov.fooddiary.controller.api

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.kuzdikenov.fooddiary.config.SecurityConfig
import ru.kuzdikenov.fooddiary.entity.ProductEntity
import ru.kuzdikenov.fooddiary.entity.RoleEntity
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.exception.ApiExceptionHandler
import ru.kuzdikenov.fooddiary.exception.ProductUsedInFoodEntriesException
import ru.kuzdikenov.fooddiary.mapper.ProductMapper
import ru.kuzdikenov.fooddiary.repository.UserRepository
import ru.kuzdikenov.fooddiary.service.CurrentUserService
import ru.kuzdikenov.fooddiary.service.ProductService
import ru.kuzdikenov.fooddiary.service.command.ProductUpsertCommand

@WebMvcTest(controllers = [ProductController::class])
@Import(SecurityConfig::class, ApiExceptionHandler::class, ProductMapper::class)
class ProductApiIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @MockitoBean
    lateinit var productService: ProductService

    @MockitoBean
    lateinit var currentUserService: CurrentUserService

    @MockitoBean
    lateinit var userRepository: UserRepository

    @Test
    fun `anonymous api request returns json unauthorized`() {
        mockMvc.perform(get("/api/products"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.path").value("/api/products"))
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = ["USER"])
    fun `api write without csrf returns json forbidden`() {
        enabledUser()

        mockMvc.perform(
            post("/api/products")
                .contentType("application/json")
                .content("""{"name":"Apple","caloriesPer100g":52,"proteinsPer100g":0.3,"fatsPer100g":0.2,"carbohydratesPer100g":14}""")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error").value("FORBIDDEN"))
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = ["USER"])
    fun `api product create returns public flag`() {
        enabledUser()
        Mockito.`when`(currentUserService.getCurrentUserId()).thenReturn(7)
        Mockito.`when`(productService.createProduct(anyCommand(), Mockito.eq(7L)))
            .thenReturn(product(isPublic = false))

        mockMvc.perform(
            post("/api/products")
                .with(csrf())
                .contentType("application/json")
                .content("""{"name":"Apple","caloriesPer100g":52,"proteinsPer100g":0.3,"fatsPer100g":0.2,"carbohydratesPer100g":14}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Apple"))
            .andExpect(jsonPath("$.isPublic").value(false))
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = ["USER"])
    fun `api access denied from service returns json forbidden`() {
        enabledUser()
        Mockito.`when`(currentUserService.getCurrentUserId()).thenReturn(7)
        Mockito.`when`(productService.getProductById(99L, 7L)).thenThrow(AccessDeniedException("No access"))

        mockMvc.perform(get("/api/products/99"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error").value("FORBIDDEN"))
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = ["USER"])
    fun `api product conflict returns json conflict`() {
        enabledUser()
        Mockito.`when`(currentUserService.getCurrentUserId()).thenReturn(7)
        Mockito.doThrow(ProductUsedInFoodEntriesException())
            .`when`(productService).deleteProduct(1L, 7L)

        mockMvc.perform(delete("/api/products/1").with(csrf()))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").value("CONFLICT"))
    }

    private fun enabledUser() {
        Mockito.`when`(userRepository.findByEmail("user@example.com"))
            .thenReturn(
                UserEntity(
                    id = 7,
                    email = "user@example.com",
                    passwordHash = "hash",
                    enabled = true,
                    roles = mutableSetOf(RoleEntity(id = 1, name = "ROLE_USER"))
                )
            )
    }

    private fun anyCommand(): ProductUpsertCommand {
        return Mockito.any(ProductUpsertCommand::class.java) ?: ProductUpsertCommand(
            name = "Apple",
            caloriesPer100g = 52.0,
            proteinsPer100g = 0.3,
            fatsPer100g = 0.2,
            carbohydratesPer100g = 14.0
        )
    }

    private fun product(isPublic: Boolean): ProductEntity {
        return ProductEntity(
            id = 1,
            name = "Apple",
            caloriesPer100g = 52.0,
            proteinsPer100g = 0.3,
            fatsPer100g = 0.2,
            carbohydratesPer100g = 14.0,
            isPublic = isPublic,
            owner = UserEntity(id = 7, email = "user@example.com", passwordHash = "hash")
        )
    }
}
