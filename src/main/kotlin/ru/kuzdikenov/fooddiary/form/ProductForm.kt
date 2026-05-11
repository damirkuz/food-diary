package ru.kuzdikenov.fooddiary.form

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import ru.kuzdikenov.api.dto.ProductCreateRequest
import ru.kuzdikenov.api.dto.ProductUpdateRequest
import ru.kuzdikenov.fooddiary.entity.ProductEntity

class ProductForm(
    @field:NotBlank(message = "Название продукта обязательно")
    @field:Size(max = 255, message = "Название продукта должно быть не длиннее 255 символов")
    var name: String = "",

    @field:NotNull(message = "Калорийность обязательна")
    @field:DecimalMin(value = "0", message = "Калорийность не может быть отрицательной")
    var caloriesPer100g: Double? = null,

    @field:NotNull(message = "Белки обязательны")
    @field:DecimalMin(value = "0", message = "Белки не могут быть отрицательными")
    var proteinsPer100g: Double? = null,

    @field:NotNull(message = "Жиры обязательны")
    @field:DecimalMin(value = "0", message = "Жиры не могут быть отрицательными")
    var fatsPer100g: Double? = null,

    @field:NotNull(message = "Углеводы обязательны")
    @field:DecimalMin(value = "0", message = "Углеводы не могут быть отрицательными")
    var carbohydratesPer100g: Double? = null,
) {

    fun toCreateRequest(): ProductCreateRequest {
        return ProductCreateRequest(
            name = name,
            caloriesPer100g = caloriesPer100g!!,
            proteinsPer100g = proteinsPer100g!!,
            fatsPer100g = fatsPer100g!!,
            carbohydratesPer100g = carbohydratesPer100g!!
        )
    }

    fun toUpdateRequest(): ProductUpdateRequest {
        return ProductUpdateRequest(
            name = name,
            caloriesPer100g = caloriesPer100g!!,
            proteinsPer100g = proteinsPer100g!!,
            fatsPer100g = fatsPer100g!!,
            carbohydratesPer100g = carbohydratesPer100g!!
        )
    }

    companion object {
        fun fromProduct(product: ProductEntity): ProductForm {
            return ProductForm(
                name = product.name,
                caloriesPer100g = product.caloriesPer100g.toDouble(),
                proteinsPer100g = product.proteinsPer100g.toDouble(),
                fatsPer100g = product.fatsPer100g.toDouble(),
                carbohydratesPer100g = product.carbohydratesPer100g.toDouble()
            )
        }
    }
}
