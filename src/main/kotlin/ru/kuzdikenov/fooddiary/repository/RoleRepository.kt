package ru.kuzdikenov.fooddiary.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.kuzdikenov.fooddiary.entity.RoleEntity

interface RoleRepository : JpaRepository<RoleEntity, Long> {

    fun findByName(name: String): RoleEntity?

}