package ru.kuzdikenov.fooddiary.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.kuzdikenov.fooddiary.entity.ProductEntity

interface ProductRepository : JpaRepository<ProductEntity, Long> {

    fun findAllByOwnerIdOrIsPublicTrue(ownerId: Long, pageable: Pageable): Page<ProductEntity>

    fun findAllByOwnerIdAndNameContainingIgnoreCaseOrIsPublicTrueAndNameContainingIgnoreCase(
        ownerId: Long,
        name: String,
        publicName: String,
        pageable: Pageable
    ): Page<ProductEntity>

    @EntityGraph(attributePaths = ["owner"])
    @Query("select p from ProductEntity p")
    fun findAllForAdmin(pageable: Pageable): Page<ProductEntity>

    @EntityGraph(attributePaths = ["owner"])
    fun findAllByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<ProductEntity>
}
