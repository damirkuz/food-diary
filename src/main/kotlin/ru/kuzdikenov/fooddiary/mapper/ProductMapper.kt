package ru.kuzdikenov.fooddiary.mapper

import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import ru.kuzdikenov.api.dto.ProductPageResponse
import ru.kuzdikenov.api.dto.ProductResponse
import ru.kuzdikenov.fooddiary.entity.ProductEntity
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class ProductMapper {

    fun toResponse(entity: ProductEntity): ProductResponse {
        return ProductResponse(
            id = entity.id!!,
            name = entity.name,
            caloriesPer100g = entity.caloriesPer100g.toDouble(),
            proteinsPer100g = entity.proteinsPer100g.toDouble(),
            fatsPer100g = entity.fatsPer100g.toDouble(),
            carbohydratesPer100g = entity.carbohydratesPer100g.toDouble(),
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

    private fun Instant.toOffsetDateTimeUtc(): OffsetDateTime {
        return this.atOffset(ZoneOffset.UTC)
    }
}
