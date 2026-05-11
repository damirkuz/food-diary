package ru.kuzdikenov.fooddiary.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import ru.kuzdikenov.fooddiary.entity.ProductEntity

interface ProductRepository : JpaRepository<ProductEntity, Long> {

    fun findAllByOwnerId(ownerId: Long, pageable: Pageable): Page<ProductEntity>

    fun findAllByOwnerIdAndNameContainingIgnoreCase(
        ownerId: Long,
        name: String,
        pageable: Pageable
    ): Page<ProductEntity>
}
