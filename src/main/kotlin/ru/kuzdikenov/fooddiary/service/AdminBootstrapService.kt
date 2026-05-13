package ru.kuzdikenov.fooddiary.service

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import ru.kuzdikenov.fooddiary.config.properties.AdminProperties
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.repository.RoleRepository
import ru.kuzdikenov.fooddiary.repository.UserRepository

@Component
class AdminBootstrapService(
    private val adminProperties: AdminProperties,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
) : CommandLineRunner {

    override fun run(vararg args: String) {
        createOrUpdateAdmin()
    }

    @Transactional
    fun createOrUpdateAdmin() {
        val email = adminProperties.email.trim().lowercase()
        val password = adminProperties.password

        if (email.isBlank() || password.isBlank()) {
            logger.info("Admin bootstrap skipped: APP_ADMIN_EMAIL or APP_ADMIN_PASSWORD is not set")
            return
        }

        val userRole = roleRepository.findByName(ROLE_USER)
            ?: throw IllegalStateException("$ROLE_USER not found")
        val adminRole = roleRepository.findByName(ROLE_ADMIN)
            ?: throw IllegalStateException("$ROLE_ADMIN not found")

        val user = userRepository.findByEmail(email)
            ?: UserEntity(
                email = email,
                passwordHash = requireNotNull(passwordEncoder.encode(password)),
                enabled = true
            )

        user.enabled = true
        user.addRole(userRole)
        user.addRole(adminRole)

        userRepository.save(user)
        logger.info("Admin account is ready: {}", email)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AdminBootstrapService::class.java)
        private const val ROLE_USER = "ROLE_USER"
        private const val ROLE_ADMIN = "ROLE_ADMIN"
    }
}
