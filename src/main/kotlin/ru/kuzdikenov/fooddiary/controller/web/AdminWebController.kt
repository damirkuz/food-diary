package ru.kuzdikenov.fooddiary.controller.web

import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import ru.kuzdikenov.fooddiary.exception.ProductUsedInFoodEntriesException
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.form.ProductForm
import ru.kuzdikenov.fooddiary.service.AdminService
import ru.kuzdikenov.fooddiary.service.CurrentUserService
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Controller
@RequestMapping("/admin")
class AdminWebController(
    private val adminService: AdminService,
    private val currentUserService: CurrentUserService,
) {

    @GetMapping
    fun dashboard(): String {
        return "redirect:/admin/users"
    }

    @GetMapping("/users")
    fun users(
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        model: Model
    ): String {
        val users = adminService.getUsers(search, page, size).map(::toView)

        model.addAttribute("users", users)
        model.addAttribute("search", search ?: "")
        model.addAttribute("currentUserId", currentUserService.getCurrentUserId())

        return "admin/users"
    }

    @PostMapping("/users/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @RequestParam enabled: Boolean,
        redirectAttributes: RedirectAttributes
    ): String {
        adminService.setUserEnabled(id, enabled, currentUserService.getCurrentUserId())
        redirectAttributes.addFlashAttribute(
            "successMessage",
            if (enabled) "Пользователь разблокирован" else "Пользователь заблокирован"
        )

        return "redirect:/admin/users"
    }

    @PostMapping("/users/{id}/role")
    fun updateRole(
        @PathVariable id: Long,
        @RequestParam admin: Boolean,
        redirectAttributes: RedirectAttributes
    ): String {
        adminService.setAdminRole(id, admin, currentUserService.getCurrentUserId())
        redirectAttributes.addFlashAttribute(
            "successMessage",
            if (admin) "Пользователю выдана роль администратора" else "Роль администратора снята"
        )

        return "redirect:/admin/users"
    }

    @GetMapping("/products")
    fun products(
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt,desc") sort: String,
        model: Model
    ): String {
        val products = adminService.getProducts(search, page, size, sort)

        model.addAttribute("products", products)
        model.addAttribute("search", search ?: "")
        model.addAttribute("sort", sort)

        return "admin/products"
    }

    @GetMapping("/products/{id}/edit")
    fun editProduct(
        @PathVariable id: Long,
        model: Model
    ): String {
        val product = adminService.getProduct(id)

        model.addAttribute("productId", id)
        model.addAttribute("ownerEmail", product.owner.email)
        model.addAttribute("form", ProductForm.fromProduct(product))

        return "admin/product-edit"
    }

    @PostMapping("/products/{id}/edit")
    fun updateProduct(
        @PathVariable id: Long,
        @Valid @ModelAttribute("form") form: ProductForm,
        bindingResult: BindingResult,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        if (bindingResult.hasErrors()) {
            model.addAttribute("productId", id)
            model.addAttribute("ownerEmail", adminService.getProduct(id).owner.email)
            return "admin/product-edit"
        }

        adminService.updateProduct(id, form.toCommand())
        redirectAttributes.addFlashAttribute("successMessage", "Продукт обновлён")

        return "redirect:/admin/products"
    }

    @PostMapping("/products/{id}/delete")
    fun deleteProduct(
        @PathVariable id: Long,
        redirectAttributes: RedirectAttributes
    ): String {
        try {
            adminService.deleteProduct(id)
            redirectAttributes.addFlashAttribute("successMessage", "Продукт удалён")
        } catch (ex: ProductUsedInFoodEntriesException) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.message)
        }

        return "redirect:/admin/products"
    }

    private fun toView(user: UserEntity): AdminUserView {
        return AdminUserView(
            id = requireNotNull(user.id),
            email = user.email,
            roleNames = user.roles.map { it.name }.sorted(),
            enabled = user.enabled,
            createdAt = USER_DATE_FORMATTER.format(user.createdAt)
        )
    }

    data class AdminUserView(
        val id: Long,
        val email: String,
        val roleNames: List<String>,
        val enabled: Boolean,
        val createdAt: String,
    )

    companion object {
        private val USER_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Moscow"))
    }
}
