package ru.kuzdikenov.fooddiary.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.kuzdikenov.fooddiary.entity.GoalEntity


interface GoalRepository : JpaRepository<GoalEntity, Long> {
}