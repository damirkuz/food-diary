package ru.kuzdikenov.fooddiary.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import ru.kuzdikenov.fooddiary.entity.ProductEntity
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.repository.ProductRepository
import ru.kuzdikenov.fooddiary.repository.UserRepository
import ru.kuzdikenov.fooddiary.service.command.ProductUpsertCommand
import java.util.Optional

class ProductServiceTest {

    private val productRepository = Mockito.mock(ProductRepository::class.java)
    private val userRepository = Mockito.mock(UserRepository::class.java)
    private val productDomainService = ProductDomainService(productRepository)
    private val service = ProductService(productRepository, userRepository, productDomainService)

    @Test
    fun `creates product for owner`() {
        val owner = UserEntity(id = 7, email = "user@example.com", passwordHash = "hash")
        Mockito.`when`(userRepository.findById(7L)).thenReturn(Optional.of(owner))
        Mockito.`when`(productRepository.save(anyProduct())).thenAnswer { invocation ->
            invocation.getArgument<ProductEntity>(0).apply { id = 15 }
        }

        val product = service.createProduct(command(name = "Apple"), ownerId = 7)

        assertEquals(15, product.id)
        assertEquals("Apple", product.name)
        assertEquals(owner, product.owner)
    }

    @Test
    fun `lists current user and public products`() {
        Mockito.`when`(
            productRepository.findAllByOwnerIdOrIsPublicTrue(Mockito.eq(7L), anyPageable())
        ).thenReturn(PageImpl(listOf(product(ownerId = 7), product(ownerId = 1, isPublic = true))))

        val page = service.getCurrentUserProducts(ownerId = 7, search = null, page = 0, size = 20, sort = "createdAt,desc")

        assertEquals(2, page.content.size)
    }

    @Test
    fun `searches current user and public products`() {
        Mockito.`when`(
            productRepository.findAllByOwnerIdAndNameContainingIgnoreCaseOrIsPublicTrueAndNameContainingIgnoreCase(
                Mockito.eq(7L),
                eqString("apple"),
                eqString("apple"),
                anyPageable()
            )
        ).thenReturn(PageImpl(listOf(product(ownerId = 7))))

        val page = service.getCurrentUserProducts(ownerId = 7, search = " apple ", page = 0, size = 20, sort = "name,asc")

        assertEquals(1, page.content.size)
    }

    @Test
    fun `updates only owned product`() {
        Mockito.`when`(productRepository.findById(1L)).thenReturn(Optional.of(product(ownerId = 7)))
        Mockito.`when`(productRepository.save(anyProduct())).thenAnswer { it.getArgument(0) }

        val updated = service.updateProduct(1L, ownerId = 7, command = command(name = "Updated"))

        assertEquals("Updated", updated.name)
    }

    @Test
    fun `does not update public product owned by another user`() {
        Mockito.`when`(productRepository.findById(1L)).thenReturn(Optional.of(product(ownerId = 99, isPublic = true)))

        assertThrows(AccessDeniedException::class.java) {
            service.updateProduct(1L, ownerId = 7, command = command(name = "Updated"))
        }
    }

    @Test
    fun `deletes owned unused product`() {
        val product = product(ownerId = 7)
        Mockito.`when`(productRepository.findById(1L)).thenReturn(Optional.of(product))

        service.deleteProduct(1L, ownerId = 7)

        val captor = ArgumentCaptor.forClass(ProductEntity::class.java)
        Mockito.verify(productRepository).delete(captor.capture())
        assertEquals(product, captor.value)
    }

    private fun command(name: String): ProductUpsertCommand {
        return ProductUpsertCommand(
            name = name,
            caloriesPer100g = 52.0,
            proteinsPer100g = 0.3,
            fatsPer100g = 0.2,
            carbohydratesPer100g = 14.0
        )
    }

    private fun anyPageable(): Pageable {
        return Mockito.any(Pageable::class.java) ?: PageRequest.of(0, 20)
    }

    private fun anyProduct(): ProductEntity {
        return Mockito.any(ProductEntity::class.java) ?: product(ownerId = 7)
    }

    private fun eqString(value: String): String {
        return Mockito.eq(value) ?: value
    }

    private fun product(ownerId: Long, isPublic: Boolean = false): ProductEntity {
        return ProductEntity(
            id = 1,
            name = "Apple",
            caloriesPer100g = 52.0,
            proteinsPer100g = 0.3,
            fatsPer100g = 0.2,
            carbohydratesPer100g = 14.0,
            isPublic = isPublic,
            owner = UserEntity(id = ownerId, email = "owner$ownerId@example.com", passwordHash = "hash")
        )
    }
}
