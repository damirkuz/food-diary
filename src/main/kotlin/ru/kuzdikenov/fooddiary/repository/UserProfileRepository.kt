package ru.kuzdikenov.fooddiary.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.kuzdikenov.fooddiary.entity.UserProfileEntity

interface UserProfileRepository : JpaRepository<UserProfileEntity, Long> {
    fun findByUserId(userId: Long): UserProfileEntity?
}