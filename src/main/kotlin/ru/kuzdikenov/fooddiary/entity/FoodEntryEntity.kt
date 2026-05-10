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
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "food_entries")
class FoodEntryEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "entry_date", nullable = false)
    var entryDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 32)
    var mealType: MealType,

    @Column(nullable = false, precision = 10, scale = 2)
    var grams: BigDecimal,

    @Column(nullable = false, precision = 10, scale = 2)
    var calories: BigDecimal,

    @Column(nullable = false, precision = 10, scale = 2)
    var proteins: BigDecimal,

    @Column(nullable = false, precision = 10, scale = 2)
    var fats: BigDecimal,

    @Column(nullable = false, precision = 10, scale = 2)
    var carbohydrates: BigDecimal,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    var product: ProductEntity
) : BaseAuditableEntity()
