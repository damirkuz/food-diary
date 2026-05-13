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
import ru.kuzdikenov.fooddiary.form.ProductForm
import ru.kuzdikenov.fooddiary.service.CurrentUserService
import ru.kuzdikenov.fooddiary.service.ProductService

@Controller
@RequestMapping("/products")
class ProductWebController(
    private val productService: ProductService,
    private val currentUserService: CurrentUserService,
) {

    @GetMapping
    fun index(
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "createdAt,desc") sort: String,
        model: Model
    ): String {
        val ownerId = currentUserService.getCurrentUserId()
        val products = productService.getCurrentUserProducts(ownerId, search, page, size, sort)

        model.addAttribute("products", products)
        model.addAttribute("search", search ?: "")
        model.addAttribute("sort", sort)

        return "products/index"
    }

    @GetMapping("/new")
    fun newProduct(model: Model): String {
        model.addAttribute("form", ProductForm())
        return "products/new"
    }

    @GetMapping("/{id}")
    fun show(
        @PathVariable id: Long,
        model: Model
    ): String {
        val ownerId = currentUserService.getCurrentUserId()
        val product = productService.getProductById(id, ownerId)

        model.addAttribute("product", product)
        return "products/show"
    }

    @PostMapping
    fun create(
        @Valid @ModelAttribute("form") form: ProductForm,
        bindingResult: BindingResult,
        redirectAttributes: RedirectAttributes
    ): String {
        if (bindingResult.hasErrors()) {
            return "products/new"
        }

        val ownerId = currentUserService.getCurrentUserId()
        productService.createProduct(form.toCommand(), ownerId)
        redirectAttributes.addFlashAttribute("successMessage", "Продукт создан")

        return "redirect:/products"
    }

    @GetMapping("/{id}/edit")
    fun edit(
        @PathVariable id: Long,
        model: Model
    ): String {
        val ownerId = currentUserService.getCurrentUserId()
        val product = productService.getProductById(id, ownerId)

        model.addAttribute("productId", id)
        model.addAttribute("form", ProductForm.fromProduct(product))
        return "products/edit"
    }

    @PostMapping("/{id}/edit")
    fun update(
        @PathVariable id: Long,
        @Valid @ModelAttribute("form") form: ProductForm,
        bindingResult: BindingResult,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        if (bindingResult.hasErrors()) {
            model.addAttribute("productId", id)
            return "products/edit"
        }

        val ownerId = currentUserService.getCurrentUserId()
        productService.updateProduct(id, ownerId, form.toCommand())
        redirectAttributes.addFlashAttribute("successMessage", "Продукт обновлён")

        return "redirect:/products"
    }

    @PostMapping("/{id}/delete")
    fun delete(
        @PathVariable id: Long,
        redirectAttributes: RedirectAttributes
    ): String {
        val ownerId = currentUserService.getCurrentUserId()

        try {
            productService.deleteProduct(id, ownerId)
            redirectAttributes.addFlashAttribute("successMessage", "Продукт удалён")
        } catch (ex: ProductUsedInFoodEntriesException) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.message)
        }

        return "redirect:/products"
    }
}
