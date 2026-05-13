package ru.kuzdikenov.fooddiary.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import ru.kuzdikenov.fooddiary.repository.UserRepository
import java.util.function.Supplier

@Configuration
class SecurityConfig(
    private val userRepository: UserRepository,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/",
                        "/login",
                        "/register",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/error"
                    ).permitAll()
                    .requestMatchers("/admin/**").access { authentication, context ->
                        adminAccess(authentication, context)
                    }
                    .requestMatchers("/api/**").access { authentication, context ->
                        enabledUserAccess(authentication, context)
                    }
                    .anyRequest().access { authentication, context ->
                        enabledUserAccess(authentication, context)
                    }
            }
            .formLogin { form ->
                form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .usernameParameter("email")
                    .passwordParameter("password")
                    .defaultSuccessUrl("/profile", true)
                    .failureUrl("/login?error")
                    .permitAll()
            }
            .logout { logout ->
                logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout")
                    .permitAll()
            }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    private fun adminAccess(
        authentication: Supplier<out Authentication?>,
        context: RequestAuthorizationContext
    ): AuthorizationDecision {
        return AuthorizationDecision(isEnabledUser(authentication.get()) && hasRoleInDatabase(authentication.get(), "ROLE_ADMIN"))
    }

    private fun enabledUserAccess(
        authentication: Supplier<out Authentication?>,
        context: RequestAuthorizationContext
    ): AuthorizationDecision {
        return AuthorizationDecision(isEnabledUser(authentication.get()))
    }

    private fun isEnabledUser(authentication: Authentication?): Boolean {
        if (authentication == null || !authentication.isAuthenticated || authentication.name == "anonymousUser") {
            return false
        }

        return userRepository.findByEmail(authentication.name)?.enabled == true
    }

    private fun hasRoleInDatabase(authentication: Authentication?, roleName: String): Boolean {
        if (authentication == null || !authentication.isAuthenticated || authentication.name == "anonymousUser") {
            return false
        }

        return userRepository.findByEmail(authentication.name)
            ?.roles
            ?.any { it.name == roleName }
            ?: false
    }
}
