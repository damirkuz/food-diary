package ru.kuzdikenov.fooddiary.controller.web

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.ui.ExtendedModelMap
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.exception.ProductUsedInFoodEntriesException
import ru.kuzdikenov.fooddiary.service.AdminService
import ru.kuzdikenov.fooddiary.service.CurrentUserService
import java.time.Instant

class AdminWebControllerTest {

    private val adminService = Mockito.mock(AdminService::class.java)
    private val currentUserService = Mockito.mock(CurrentUserService::class.java)
    private val controller = AdminWebController(adminService, currentUserService)

    @Test
    fun `users page maps formatted date`() {
        val model = ExtendedModelMap()
        val user = UserEntity(id = 2, email = "user@example.com", passwordHash = "hash")
        user.createdAt = Instant.parse("2026-05-13T20:36:00Z")
        Mockito.`when`(adminService.getUsers(null, 0, 20)).thenReturn(PageImpl(listOf(user)))
        Mockito.`when`(currentUserService.getCurrentUserId()).thenReturn(1)

        assertEquals("admin/users", controller.users(null, 0, 20, model))
        val users = model["users"] as Page<*>
        val view = users.content.first() as AdminWebController.AdminUserView
        assertEquals("13.05.2026 23:36", view.createdAt)
    }

    @Test
    fun `role update redirects with flash`() {
        val redirectAttributes = RedirectAttributesModelMap()

        assertEquals("redirect:/admin/users", controller.updateRole(2, true, redirectAttributes))
        assertEquals("Пользователю выдана роль администратора", redirectAttributes.flashAttributes["successMessage"])
    }

    @Test
    fun `product delete conflict redirects with flash error`() {
        val redirectAttributes = RedirectAttributesModelMap()
        Mockito.doThrow(ProductUsedInFoodEntriesException()).`when`(adminService).deleteProduct(5)

        assertEquals("redirect:/admin/products", controller.deleteProduct(5, redirectAttributes))
        assertEquals(
            "Продукт используется в записях дневника и не может быть удалён",
            redirectAttributes.flashAttributes["errorMessage"]
        )
    }
}
