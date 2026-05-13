package ru.kuzdikenov.fooddiary.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.kuzdikenov.fooddiary.entity.ProductEntity
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.exception.UserNotFoundException
import ru.kuzdikenov.fooddiary.repository.ProductRepository
import ru.kuzdikenov.fooddiary.repository.RoleRepository
import ru.kuzdikenov.fooddiary.repository.UserRepository
import ru.kuzdikenov.fooddiary.service.command.ProductUpsertCommand

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val productRepository: ProductRepository,
    private val productDomainService: ProductDomainService,
) {

    @Transactional(readOnly = true)
    fun getUsers(search: String?, page: Int, size: Int): Page<UserEntity> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100), Sort.by("email").ascending())
        val normalizedSearch = search?.trim()

        return if (normalizedSearch.isNullOrBlank()) {
            userRepository.findAll(pageable)
        } else {
            userRepository.findAllByEmailContainingIgnoreCase(normalizedSearch, pageable)
        }
    }

    @Transactional
    fun setUserEnabled(userId: Long, enabled: Boolean, currentAdminId: Long) {
        if (userId == currentAdminId && !enabled) {
            throw AccessDeniedException("Администратор не может заблокировать свой аккаунт")
        }

        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        user.enabled = enabled
        userRepository.save(user)
    }

    @Transactional
    fun setAdminRole(userId: Long, admin: Boolean, currentAdminId: Long) {
        if (userId == currentAdminId && !admin) {
            throw AccessDeniedException("Администратор не может снять роль администратора с себя")
        }

        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        val adminRole = roleRepository.findByName(ROLE_ADMIN)
            ?: throw IllegalStateException("$ROLE_ADMIN not found")

        if (admin) {
            user.addRole(adminRole)
        } else {
            user.removeRole(adminRole)
        }

        userRepository.save(user)
    }

    @Transactional(readOnly = true)
    fun getProducts(search: String?, page: Int, size: Int, sort: String): Page<ProductEntity> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100), ProductSorts.admin(sort))
        val normalizedSearch = search?.trim()

        return if (normalizedSearch.isNullOrBlank()) {
            productRepository.findAllForAdmin(pageable)
        } else {
            productRepository.findAllByNameContainingIgnoreCase(normalizedSearch, pageable)
        }
    }

    @Transactional(readOnly = true)
    fun getProduct(id: Long): ProductEntity {
        return productDomainService.getProduct(id)
    }

    @Transactional
    fun updateProduct(id: Long, command: ProductUpsertCommand): ProductEntity {
        val product = getProduct(id)

        productDomainService.applyUpsert(product, command)

        return productRepository.save(product)
    }

    @Transactional
    fun deleteProduct(id: Long) {
        val product = getProduct(id)

        productDomainService.requireNotUsed(product)

        productRepository.delete(product)
    }

    companion object {
        private const val ROLE_ADMIN = "ROLE_ADMIN"
    }
}
