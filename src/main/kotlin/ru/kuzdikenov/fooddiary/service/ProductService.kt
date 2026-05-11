package ru.kuzdikenov.fooddiary.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.kuzdikenov.fooddiary.entity.ProductEntity
import ru.kuzdikenov.fooddiary.exception.ProductUsedInFoodEntriesException
import ru.kuzdikenov.fooddiary.exception.ProductNotFoundException
import ru.kuzdikenov.fooddiary.exception.ProductSortException
import ru.kuzdikenov.fooddiary.exception.UserNotFoundException
import ru.kuzdikenov.fooddiary.repository.ProductRepository
import ru.kuzdikenov.fooddiary.repository.UserRepository
import ru.kuzdikenov.fooddiary.service.command.ProductUpsertCommand

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
) {

    @Transactional
    fun createProduct(command: ProductUpsertCommand, ownerId: Long): ProductEntity {
        val owner = userRepository.findById(ownerId).orElseThrow { UserNotFoundException() }

        val product = ProductEntity(
            name = command.name,
            caloriesPer100g = command.caloriesPer100g,
            proteinsPer100g = command.proteinsPer100g,
            fatsPer100g = command.fatsPer100g,
            carbohydratesPer100g = command.carbohydratesPer100g,
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
    fun updateProduct(id: Long, ownerId: Long, command: ProductUpsertCommand): ProductEntity {
        val product = getProductForOwner(id, ownerId)

        product.name = command.name
        product.caloriesPer100g = command.caloriesPer100g
        product.proteinsPer100g = command.proteinsPer100g
        product.fatsPer100g = command.fatsPer100g
        product.carbohydratesPer100g = command.carbohydratesPer100g

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
