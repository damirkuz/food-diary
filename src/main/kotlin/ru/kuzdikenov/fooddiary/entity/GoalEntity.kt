package ru.kuzdikenov.fooddiary.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "goals")
class GoalEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 50)
    var name: String,

    @Column(name = "calories_modifier", nullable = false)
    var caloriesModifier: Double,

    @Column(name = "proteins_ratio", nullable = false)
    var proteinsRatio: Double,

    @Column(name = "fats_ratio", nullable = false)
    var fatsRatio: Double,

    @Column(name = "carbohydrates_ratio", nullable = false)
    var carbohydratesRatio: Double,

    @OneToMany(
        mappedBy = "goal",
        fetch = FetchType.LAZY
    )
    var userProfiles: MutableList<UserProfileEntity> = mutableListOf()
)
