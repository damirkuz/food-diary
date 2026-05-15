package ru.kuzdikenov.fooddiary.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.security.web.util.matcher.RequestMatcher
import ru.kuzdikenov.fooddiary.repository.UserRepository
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.function.Supplier

@Configuration
class SecurityConfig(
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val apiRequestMatcher = RequestMatcher { request ->
            request.servletPath.startsWith("/api/")
        }

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
            .exceptionHandling { exceptions ->
                exceptions
                    .defaultAuthenticationEntryPointFor(
                        { request, response, _ ->
                            writeApiError(
                                request = request,
                                response = response,
                                status = HttpStatus.UNAUTHORIZED,
                                error = "UNAUTHORIZED",
                                message = "Для выполнения запроса необходимо войти в систему"
                            )
                        },
                        apiRequestMatcher
                    )
                    .defaultAccessDeniedHandlerFor(
                        { request, response, _ ->
                            writeApiError(
                                request = request,
                                response = response,
                                status = HttpStatus.FORBIDDEN,
                                error = "FORBIDDEN",
                                message = "У вас нет доступа к этому ресурсу"
                            )
                        },
                        apiRequestMatcher
                    )
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

    private fun writeApiError(
        request: HttpServletRequest,
        response: HttpServletResponse,
        status: HttpStatus,
        error: String,
        message: String,
    ) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()

        objectMapper.writeValue(
            response.outputStream,
            mapOf(
                "status" to status.value(),
                "error" to error,
                "message" to message,
                "timestamp" to Instant.now().toString(),
                "path" to request.requestURI
            )
        )
    }
}
