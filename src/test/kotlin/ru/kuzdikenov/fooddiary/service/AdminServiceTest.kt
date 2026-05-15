package ru.kuzdikenov.fooddiary.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.access.AccessDeniedException
import ru.kuzdikenov.fooddiary.entity.FoodEntryEntity
import ru.kuzdikenov.fooddiary.entity.ProductEntity
import ru.kuzdikenov.fooddiary.entity.RoleEntity
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.exception.ProductUsedInFoodEntriesException
import ru.kuzdikenov.fooddiary.repository.ProductRepository
import ru.kuzdikenov.fooddiary.repository.RoleRepository
import ru.kuzdikenov.fooddiary.repository.UserRepository
import ru.kuzdikenov.fooddiary.service.command.ProductUpsertCommand
import java.util.Optional

class AdminServiceTest {

    private val userRepository = Mockito.mock(UserRepository::class.java)
    private val roleRepository = Mockito.mock(RoleRepository::class.java)
    private val productRepository = Mockito.mock(ProductRepository::class.java)
    private val productDomainService = ProductDomainService(productRepository)
    private val service = AdminService(userRepository, roleRepository, productRepository, productDomainService)

    @Test
    fun `does not allow admin to block own account`() {
        assertThrows(AccessDeniedException::class.java) {
            service.setUserEnabled(userId = 1, enabled = false, currentAdminId = 1)
        }
    }

    @Test
    fun `does not allow admin to remove own admin role`() {
        assertThrows(AccessDeniedException::class.java) {
            service.setAdminRole(userId = 1, admin = false, currentAdminId = 1)
        }
    }

    @Test
    fun `grants admin role`() {
        val user = UserEntity(id = 2, email = "user@example.com", passwordHash = "hash")
        val adminRole = RoleEntity(id = 1, name = "ROLE_ADMIN")
        Mockito.`when`(userRepository.findById(2L)).thenReturn(Optional.of(user))
        Mockito.`when`(roleRepository.findByName("ROLE_ADMIN")).thenReturn(adminRole)

        service.setAdminRole(userId = 2, admin = true, currentAdminId = 1)

        assertTrue(user.roles.contains(adminRole))
        Mockito.verify(userRepository).save(user)
    }

    @Test
    fun `updates product through shared product rules`() {
        val product = product()
        Mockito.`when`(productRepository.findById(5L)).thenReturn(Optional.of(product))
        Mockito.`when`(productRepository.save(Mockito.any(ProductEntity::class.java))).thenAnswer { it.getArgument(0) }

        val updated = service.updateProduct(
            id = 5,
            command = ProductUpsertCommand(
                name = "Buckwheat",
                caloriesPer100g = 110.0,
                proteinsPer100g = 3.6,
                fatsPer100g = 1.1,
                carbohydratesPer100g = 21.3
            )
        )

        assertEquals("Buckwheat", updated.name)
        assertEquals(110.0, updated.caloriesPer100g)
    }

    @Test
    fun `does not delete product used in diary`() {
        val product = product()
        product.foodEntries.add(Mockito.mock(FoodEntryEntity::class.java))
        Mockito.`when`(productRepository.findById(5L)).thenReturn(Optional.of(product))

        assertThrows(ProductUsedInFoodEntriesException::class.java) {
            service.deleteProduct(5L)
        }
    }

    private fun product(): ProductEntity {
        return ProductEntity(
            id = 5,
            name = "Rice",
            caloriesPer100g = 130.0,
            proteinsPer100g = 2.7,
            fatsPer100g = 0.3,
            carbohydratesPer100g = 28.2,
            owner = UserEntity(id = 7, email = "owner@example.com", passwordHash = "hash")
        )
    }
}
