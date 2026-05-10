package ru.kuzdikenov.fooddiary.exception

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.resource.NoResourceFoundException


@ControllerAdvice
class GlobalWebExceptionHandler {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException() {
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(
        ex: Exception,
        request: HttpServletRequest,
        model: Model
    ): String {
        model.addAttribute("message", "Произошла внутренняя ошибка сервера")
        model.addAttribute("path", request.requestURI)
        return "error/500"
    }
}
