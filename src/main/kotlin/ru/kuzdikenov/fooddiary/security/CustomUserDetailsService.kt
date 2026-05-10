package ru.kuzdikenov.fooddiary.security

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import ru.kuzdikenov.fooddiary.repository.UserRepository

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {
    override fun loadUserByUsername(email: String): UserDetails {
        val user = userRepository.findByEmail(email) ?: throw  IllegalArgumentException("User with email $email not found")

        return User.builder()
            .username(user.email)
            .password(user.passwordHash)
            .disabled(!user.enabled)
            .authorities(user.roles.map { SimpleGrantedAuthority(it.name) })
            .build()
    }

}