package ru.kuzdikenov.fooddiary.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "products")
class ProductEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(name = "calories_per_100g", nullable = false, precision = 10, scale = 2)
    var caloriesPer100g: BigDecimal,

    @Column(name = "proteins_per_100g", nullable = false, precision = 10, scale = 2)
    var proteinsPer100g: BigDecimal,

    @Column(name = "fats_per_100g", nullable = false, precision = 10, scale = 2)
    var fatsPer100g: BigDecimal,

    @Column(name = "carbohydrates_per_100g", nullable = false, precision = 10, scale = 2)
    var carbohydratesPer100g: BigDecimal,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: UserEntity,

    @OneToMany(
        mappedBy = "product",
        fetch = FetchType.LAZY
    )
    var foodEntries: MutableList<FoodEntryEntity> = mutableListOf()
) : BaseAuditableEntity()
