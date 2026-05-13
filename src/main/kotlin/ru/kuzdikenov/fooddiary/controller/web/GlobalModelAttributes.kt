package ru.kuzdikenov.fooddiary.controller.web

import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute
import ru.kuzdikenov.fooddiary.repository.UserRepository

@ControllerAdvice(basePackages = ["ru.kuzdikenov.fooddiary.controller.web"])
class GlobalModelAttributes(
    private val userRepository: UserRepository,
) {

    @ModelAttribute("isAuthenticated")
    fun isAuthenticated(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication ?: return false
        return authentication.isAuthenticated && authentication !is AnonymousAuthenticationToken
    }

    @ModelAttribute("currentUserEmail")
    fun currentUserEmail(): String? {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication == null || !isAuthenticated()) {
            return null
        }

        return authentication.name
    }

    @ModelAttribute("isAdmin")
    fun isAdmin(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication ?: return false
        return userRepository.findByEmail(authentication.name)
            ?.roles
            ?.any { it.name == "ROLE_ADMIN" }
            ?: false
    }
}
