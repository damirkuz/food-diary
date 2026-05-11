package ru.kuzdikenov.fooddiary.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.kuzdikenov.api.dto.ProductCreateRequest
import ru.kuzdikenov.api.dto.ProductUpdateRequest
import ru.kuzdikenov.fooddiary.entity.ProductEntity
import ru.kuzdikenov.fooddiary.exception.ProductUsedInFoodEntriesException
import ru.kuzdikenov.fooddiary.exception.ProductNotFoundException
import ru.kuzdikenov.fooddiary.exception.ProductSortException
import ru.kuzdikenov.fooddiary.exception.UserNotFoundException
import ru.kuzdikenov.fooddiary.repository.ProductRepository
import ru.kuzdikenov.fooddiary.repository.UserRepository
import java.math.BigDecimal

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
) {

    @Transactional
    fun createProduct(productCreateRequest: ProductCreateRequest, ownerId: Long): ProductEntity {
        val owner = userRepository.findById(ownerId).orElseThrow { UserNotFoundException() }

        val product = ProductEntity(
            name = productCreateRequest.name,
            caloriesPer100g = BigDecimal.valueOf(productCreateRequest.caloriesPer100g),
            proteinsPer100g = BigDecimal.valueOf(productCreateRequest.proteinsPer100g),
            fatsPer100g = BigDecimal.valueOf(productCreateRequest.fatsPer100g),
            carbohydratesPer100g = BigDecimal.valueOf(productCreateRequest.carbohydratesPer100g),
            owner = owner
        )

        return productRepository.save(product)
    }

    @Transactional(readOnly = true)
    fun getCurrentUserProducts(
        ownerId: Long,
        search: String?,
        page: Int,
        size: Int,
        sort: String
    ): Page<ProductEntity> {
        val pageable = PageRequest.of(page, size, toSort(sort))
        val normalizedSearch = search?.trim()

        return if (normalizedSearch.isNullOrBlank()) {
            productRepository.findAllByOwnerId(ownerId, pageable)
        } else {
            productRepository.findAllByOwnerIdAndNameContainingIgnoreCase(ownerId, normalizedSearch, pageable)
        }
    }

    @Transactional(readOnly = true)
    fun getProductById(id: Long, ownerId: Long): ProductEntity {
        return getProductForOwner(id, ownerId)
    }

    @Transactional
    fun updateProduct(id: Long, ownerId: Long, productUpdateRequest: ProductUpdateRequest): ProductEntity {
        val product = getProductForOwner(id, ownerId)

        product.name = productUpdateRequest.name
        product.caloriesPer100g = BigDecimal.valueOf(productUpdateRequest.caloriesPer100g)
        product.proteinsPer100g = BigDecimal.valueOf(productUpdateRequest.proteinsPer100g)
        product.fatsPer100g = BigDecimal.valueOf(productUpdateRequest.fatsPer100g)
        product.carbohydratesPer100g = BigDecimal.valueOf(productUpdateRequest.carbohydratesPer100g)

        return productRepository.save(product)
    }

    @Transactional
    fun deleteProduct(id: Long, ownerId: Long) {
        val product = getProductForOwner(id, ownerId)

        if (product.foodEntries.isNotEmpty()) {
            throw ProductUsedInFoodEntriesException()
        }

        productRepository.delete(product)
    }

    private fun getProductForOwner(id: Long, ownerId: Long): ProductEntity {
        val product = productRepository.findById(id).orElseThrow { ProductNotFoundException() }
        val productOwnerId = product.owner.id ?: throw UserNotFoundException()

        if (productOwnerId != ownerId) {
            throw AccessDeniedException("User has no access to this product")
        }

        return product
    }

    private fun toSort(sort: String): Sort {
        return when (sort) {
            "createdAt,desc" -> Sort.by(Sort.Direction.DESC, "createdAt")
            "createdAt,asc" -> Sort.by(Sort.Direction.ASC, "createdAt")
            "name,asc" -> Sort.by(Sort.Direction.ASC, "name")
            "name,desc" -> Sort.by(Sort.Direction.DESC, "name")
            else -> throw ProductSortException(sort)
        }
    }
}
