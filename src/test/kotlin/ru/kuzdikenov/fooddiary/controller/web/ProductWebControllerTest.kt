package ru.kuzdikenov.fooddiary.controller.web

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.domain.PageImpl
import org.springframework.ui.ExtendedModelMap
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap
import ru.kuzdikenov.fooddiary.entity.ProductEntity
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.exception.ProductUsedInFoodEntriesException
import ru.kuzdikenov.fooddiary.form.ProductForm
import ru.kuzdikenov.fooddiary.service.CurrentUserService
import ru.kuzdikenov.fooddiary.service.ProductService

class ProductWebControllerTest {

    private val productService = Mockito.mock(ProductService::class.java)
    private val currentUserService = Mockito.mock(CurrentUserService::class.java)
    private val controller = ProductWebController(productService, currentUserService)

    @Test
    fun `index adds products to model`() {
        val model = ExtendedModelMap()
        Mockito.`when`(currentUserService.getCurrentUserId()).thenReturn(7)
        Mockito.`when`(productService.getCurrentUserProducts(7, null, 0, 20, "createdAt,desc"))
            .thenReturn(PageImpl(listOf(product())))

        assertEquals("products/index", controller.index(null, 0, 20, "createdAt,desc", model))
        assertEquals("", model["search"])
        assertEquals("createdAt,desc", model["sort"])
    }

    @Test
    fun `create redirects after successful save`() {
        val form = productForm()
        val redirectAttributes = RedirectAttributesModelMap()
        Mockito.`when`(currentUserService.getCurrentUserId()).thenReturn(7)

        val view = controller.create(form, BeanPropertyBindingResult(form, "form"), redirectAttributes)

        assertEquals("redirect:/products", view)
        assertEquals("Продукт создан", redirectAttributes.flashAttributes["successMessage"])
    }

    @Test
    fun `create returns form when validation fails`() {
        val form = ProductForm()
        val bindingResult = BeanPropertyBindingResult(form, "form")
        bindingResult.rejectValue("name", "required")

        assertEquals("products/new", controller.create(form, bindingResult, RedirectAttributesModelMap()))
    }

    @Test
    fun `delete puts flash error when product is used`() {
        val redirectAttributes = RedirectAttributesModelMap()
        Mockito.`when`(currentUserService.getCurrentUserId()).thenReturn(7)
        Mockito.doThrow(ProductUsedInFoodEntriesException()).`when`(productService).deleteProduct(1, 7)

        assertEquals("redirect:/products", controller.delete(1, redirectAttributes))
        assertEquals(
            "Продукт используется в записях дневника и не может быть удалён",
            redirectAttributes.flashAttributes["errorMessage"]
        )
    }

    private fun productForm(): ProductForm {
        return ProductForm(
            name = "Apple",
            caloriesPer100g = 52.0,
            proteinsPer100g = 0.3,
            fatsPer100g = 0.2,
            carbohydratesPer100g = 14.0
        )
    }

    private fun product(): ProductEntity {
        return ProductEntity(
            id = 1,
            name = "Apple",
            caloriesPer100g = 52.0,
            proteinsPer100g = 0.3,
            fatsPer100g = 0.2,
            carbohydratesPer100g = 14.0,
            owner = UserEntity(id = 7, email = "user@example.com", passwordHash = "hash")
        )
    }
}
