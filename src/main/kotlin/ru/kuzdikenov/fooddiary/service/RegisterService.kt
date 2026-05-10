package ru.kuzdikenov.fooddiary.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.kuzdikenov.fooddiary.entity.UserEntity
import ru.kuzdikenov.fooddiary.exception.EmailAlreadyExistsException
import ru.kuzdikenov.fooddiary.form.RegisterForm
import ru.kuzdikenov.fooddiary.repository.RoleRepository
import ru.kuzdikenov.fooddiary.repository.UserRepository

@Service
class RegisterService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional
    fun register(form: RegisterForm) {

        val email = form.email.trim().lowercase()

        if (userRepository.existsByEmail(email)) {
            throw EmailAlreadyExistsException(email)
        }

        val userRole = roleRepository.findByName("ROLE_USER")
            ?: throw IllegalStateException("ROLE_USER not found")

        val user = UserEntity(
            email = email,
            passwordHash = requireNotNull(passwordEncoder.encode(form.password))
        )

        user.addRole(userRole)

        userRepository.save(user)
    }
}
