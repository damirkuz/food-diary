package ru.kuzdikenov.fooddiary.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import ru.kuzdikenov.fooddiary.entity.UserEntity

interface UserRepository : JpaRepository<UserEntity, Long> {

    fun findByEmail(email: String): UserEntity?

    fun existsByEmail(email: String): Boolean

    fun findAllByEmailContainingIgnoreCase(email: String, pageable: Pageable): Page<UserEntity>

}
