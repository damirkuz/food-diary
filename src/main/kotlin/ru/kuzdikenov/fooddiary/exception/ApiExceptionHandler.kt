package ru.kuzdikenov.fooddiary.exception

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.kuzdikenov.api.dto.ErrorResponse
import ru.kuzdikenov.api.dto.FieldError
import ru.kuzdikenov.api.dto.ValidationErrorResponse
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = ["ru.kuzdikenov.fooddiary.controller.api"])
class ApiExceptionHandler {

    @ExceptionHandler(ProductNotFoundException::class, FoodEntryNotFoundException::class)
    fun handleNotFound(
        ex: RuntimeException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.message ?: "Ресурс не найден", request)
    }

    @ExceptionHandler(ProductUsedInFoodEntriesException::class)
    fun handleProductCantDeleted(
        ex: ProductUsedInFoodEntriesException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return error(HttpStatus.CONFLICT, "CONFLICT", ex.message ?: "Продукт нельзя удалить", request)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        ex: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.message ?: "У вас нет доступа к этому ресурсу", request)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ValidationErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.map {
            FieldError(
                field = it.field,
                message = it.defaultMessage ?: "Некорректное значение"
            )
        }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ValidationErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "VALIDATION_ERROR",
                    message = "Некорректные данные запроса",
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                    path = request.requestURI,
                    fieldErrors = fieldErrors
                )
            )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ValidationErrorResponse> {
        val fieldErrors = ex.constraintViolations.map {
            FieldError(
                field = it.propertyPath.toString(),
                message = it.message
            )
        }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ValidationErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "VALIDATION_ERROR",
                    message = "Некорректные данные запроса",
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                    path = request.requestURI,
                    fieldErrors = fieldErrors
                )
            )
    }

    @ExceptionHandler(ProductSortException::class)
    fun handleProductSort(
        ex: ProductSortException,
        request: HttpServletRequest
    ): ResponseEntity<ValidationErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ValidationErrorResponse(
                    status = HttpStatus.BAD_REQUEST.value(),
                    error = "VALIDATION_ERROR",
                    message = "Некорректные данные запроса",
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                    path = request.requestURI,
                    fieldErrors = listOf(
                        FieldError(
                            field = "sort",
                            message = ex.message ?: "Недопустимое значение sort"
                        )
                    )
                )
            )
    }

    private fun error(
        status: HttpStatus,
        error: String,
        message: String,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(status)
            .body(
                ErrorResponse(
                    status = status.value(),
                    error = error,
                    message = message,
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                    path = request.requestURI
                )
            )
    }
}
