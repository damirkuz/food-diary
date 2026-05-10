package ru.kuzdikenov.fooddiary.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(name = "users")
class UserEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 254)
    var email: String,

    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = [
            JoinColumn(name = "user_id", nullable = false)
        ],
        inverseJoinColumns = [
            JoinColumn(name = "role_id", nullable = false)
        ],
        uniqueConstraints = [
            UniqueConstraint(
                name = "uk_user_roles_user_id_role_id",
                columnNames = ["user_id", "role_id"]
            )
        ]
    )
    var roles: MutableSet<RoleEntity> = mutableSetOf(),

    @OneToOne(
        mappedBy = "user",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    var profile: UserProfileEntity? = null,

    @OneToMany(
        mappedBy = "owner",
        fetch = FetchType.LAZY
    )
    var products: MutableList<ProductEntity> = mutableListOf(),

    @OneToMany(
        mappedBy = "user",
        fetch = FetchType.LAZY
    )
    var foodEntries: MutableList<FoodEntryEntity> = mutableListOf()
) : BaseAuditableEntity() {

    fun addRole(role: RoleEntity) {
        roles.add(role)
    }

    fun removeRole(role: RoleEntity) {
        roles.remove(role)
    }
}
