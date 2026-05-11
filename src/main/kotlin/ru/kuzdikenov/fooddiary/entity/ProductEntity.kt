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

@Entity
@Table(name = "products")
class ProductEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(name = "calories_per_100g", nullable = false)
    var caloriesPer100g: Double,

    @Column(name = "proteins_per_100g", nullable = false)
    var proteinsPer100g: Double,

    @Column(name = "fats_per_100g", nullable = false)
    var fatsPer100g: Double,

    @Column(name = "carbohydrates_per_100g", nullable = false)
    var carbohydratesPer100g: Double,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: UserEntity,

    @OneToMany(
        mappedBy = "product",
        fetch = FetchType.LAZY
    )
    var foodEntries: MutableList<FoodEntryEntity> = mutableListOf()
) : BaseAuditableEntity()
