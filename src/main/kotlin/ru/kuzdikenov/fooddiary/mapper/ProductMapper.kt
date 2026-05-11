package ru.kuzdikenov.fooddiary.mapper

import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import ru.kuzdikenov.api.dto.ProductCreateRequest
import ru.kuzdikenov.api.dto.ProductPageResponse
import ru.kuzdikenov.api.dto.ProductResponse
import ru.kuzdikenov.api.dto.ProductUpdateRequest
import ru.kuzdikenov.fooddiary.entity.ProductEntity
import ru.kuzdikenov.fooddiary.service.command.ProductUpsertCommand
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class ProductMapper {

    fun toResponse(entity: ProductEntity): ProductResponse {
        return ProductResponse(
            id = entity.id!!,
            name = entity.name,
            caloriesPer100g = entity.caloriesPer100g,
            proteinsPer100g = entity.proteinsPer100g,
            fatsPer100g = entity.fatsPer100g,
            carbohydratesPer100g = entity.carbohydratesPer100g,
            createdAt = entity.createdAt.toOffsetDateTimeUtc()
        )
    }

    fun toPageResponse(page: Page<ProductEntity>): ProductPageResponse {
        return ProductPageResponse(
            content = page.content.map(::toResponse),
            page = page.number,
            propertySize = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages
        )
    }

    fun toCommand(productCreateRequest: ProductCreateRequest): ProductUpsertCommand {
        return ProductUpsertCommand(
            name = productCreateRequest.name,
            caloriesPer100g = productCreateRequest.caloriesPer100g,
            proteinsPer100g = productCreateRequest.proteinsPer100g,
            fatsPer100g = productCreateRequest.fatsPer100g,
            carbohydratesPer100g = productCreateRequest.carbohydratesPer100g
        )
    }

    fun toCommand(productUpdateRequest: ProductUpdateRequest): ProductUpsertCommand {
        return ProductUpsertCommand(
            name = productUpdateRequest.name,
            caloriesPer100g = productUpdateRequest.caloriesPer100g,
            proteinsPer100g = productUpdateRequest.proteinsPer100g,
            fatsPer100g = productUpdateRequest.fatsPer100g,
            carbohydratesPer100g = productUpdateRequest.carbohydratesPer100g
        )
    }

    private fun Instant.toOffsetDateTimeUtc(): OffsetDateTime {
        return this.atOffset(ZoneOffset.UTC)
    }
}
