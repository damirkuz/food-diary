package ru.kuzdikenov.fooddiary.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.kuzdikenov.fooddiary.entity.ProductEntity
import ru.kuzdikenov.fooddiary.exception.UserNotFoundException
import ru.kuzdikenov.fooddiary.repository.ProductRepository
import ru.kuzdikenov.fooddiary.repository.UserRepository
import ru.kuzdikenov.fooddiary.service.command.ProductUpsertCommand

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val productDomainService: ProductDomainService,
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
        val pageable = PageRequest.of(page, size, ProductSorts.user(sort))
        val normalizedSearch = search?.trim()

        return if (normalizedSearch.isNullOrBlank()) {
            productRepository.findAllByOwnerIdOrIsPublicTrue(ownerId, pageable)
        } else {
            productRepository.findAllByOwnerIdAndNameContainingIgnoreCaseOrIsPublicTrueAndNameContainingIgnoreCase(
                ownerId,
                normalizedSearch,
                normalizedSearch,
                pageable
            )
        }
    }

    @Transactional(readOnly = true)
    fun getProductById(id: Long, ownerId: Long): ProductEntity {
        return productDomainService.getAccessibleProduct(id, ownerId)
    }

    @Transactional
    fun updateProduct(id: Long, ownerId: Long, command: ProductUpsertCommand): ProductEntity {
        val product = productDomainService.getOwnedProduct(id, ownerId)

        productDomainService.applyUpsert(product, command)

        return productRepository.save(product)
    }

    @Transactional
    fun deleteProduct(id: Long, ownerId: Long) {
        val product = productDomainService.getOwnedProduct(id, ownerId)

        productDomainService.requireNotUsed(product)

        productRepository.delete(product)
    }
}
