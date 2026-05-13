package ru.kuzdikenov.fooddiary.exception

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.resource.NoResourceFoundException


@ControllerAdvice(basePackages = ["ru.kuzdikenov.fooddiary.controller.web"])
class GlobalWebExceptionHandler {

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(
        ex: NoResourceFoundException,
        request: HttpServletRequest,
        response: HttpServletResponse,
        model: Model
    ): String {
        logger.warn("Resource not found: {}", request.requestURI)
        response.status = HttpStatus.NOT_FOUND.value()
        model.addAttribute("message", "Страница не найдена")
        model.addAttribute("path", request.requestURI)
        return "error/404"
    }

    @ExceptionHandler(
        ProductNotFoundException::class,
        FoodEntryNotFoundException::class,
        UserNotFoundException::class,
        NutritionLookupNotFoundException::class
    )
    fun handleNotFound(
        ex: RuntimeException,
        request: HttpServletRequest,
        response: HttpServletResponse,
        model: Model
    ): String {
        logger.warn("Entity not found at {}: {}", request.requestURI, ex.message)
        response.status = HttpStatus.NOT_FOUND.value()
        model.addAttribute("message", ex.message ?: "Ресурс не найден")
        model.addAttribute("path", request.requestURI)
        return "error/404"
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        ex: AccessDeniedException,
        request: HttpServletRequest,
        response: HttpServletResponse,
        model: Model
    ): String {
        logger.warn("Access denied at {}: {}", request.requestURI, ex.message)
        response.status = HttpStatus.FORBIDDEN.value()
        model.addAttribute("message", ex.message ?: "У вас нет доступа к этому действию")
        model.addAttribute("path", request.requestURI)
        return "error/403"
    }

    @ExceptionHandler(ProductSortException::class, IllegalArgumentException::class)
    fun handleBadRequest(
        ex: RuntimeException,
        request: HttpServletRequest,
        response: HttpServletResponse,
        model: Model
    ): String {
        logger.warn("Bad request at {}: {}", request.requestURI, ex.message)
        response.status = HttpStatus.BAD_REQUEST.value()
        model.addAttribute("message", ex.message ?: "Некорректный запрос")
        model.addAttribute("path", request.requestURI)
        return "error/400"
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(
        ex: Exception,
        request: HttpServletRequest,
        response: HttpServletResponse,
        model: Model
    ): String {
        logger.error("Unexpected web exception at ${request.requestURI}", ex)
        response.status = HttpStatus.INTERNAL_SERVER_ERROR.value()
        model.addAttribute("message", "Произошла внутренняя ошибка сервера")
        model.addAttribute("path", request.requestURI)
        return "error/500"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GlobalWebExceptionHandler::class.java)
    }
}
