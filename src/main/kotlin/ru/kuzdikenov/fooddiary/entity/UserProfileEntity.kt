package ru.kuzdikenov.fooddiary.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "user_profiles")
class UserProfileEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    var user: UserEntity,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var gender: Gender,

    @Column(name = "birth_date", nullable = false)
    var birthDate: LocalDate,

    @Column(name = "height_cm", nullable = false)
    var heightCm: Int,

    @Column(name = "weight_kg", nullable = false)
    var weightKg: Double,

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", nullable = false, length = 32)
    var activityLevel: ActivityLevel,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    var goal: GoalEntity
)
