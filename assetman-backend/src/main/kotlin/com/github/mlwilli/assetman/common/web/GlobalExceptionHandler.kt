package com.github.mlwilli.assetman.common.web

import com.github.mlwilli.assetman.common.error.BadRequestException
import com.github.mlwilli.assetman.common.error.ConflictException
import com.github.mlwilli.assetman.common.error.ForbiddenException
import com.github.mlwilli.assetman.common.error.NotFoundException
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map {
            ApiValidationError(
                field = it.field,
                message = it.defaultMessage ?: "Invalid value",
                rejectedValue = it.rejectedValue
            )
        }

        val status = HttpStatus.BAD_REQUEST
        val body = ApiErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = "Validation failed",
            path = request.requestURI,
            validationErrors = errors
        )
        return ResponseEntity(body, HttpHeaders(), status)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val errors = ex.constraintViolations.map { v ->
            ApiValidationError(
                field = v.propertyPath.toString(),
                message = v.message,
                rejectedValue = v.invalidValue
            )
        }

        val status = HttpStatus.BAD_REQUEST
        val body = ApiErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = "Validation failed",
            path = request.requestURI,
            validationErrors = errors
        )
        return ResponseEntity(body, HttpHeaders(), status)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val status = HttpStatus.BAD_REQUEST
        val body = ApiErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = "Malformed JSON request",
            path = request.requestURI
        )
        return ResponseEntity(body, HttpHeaders(), status)
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(
        ex: NotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val status = HttpStatus.NOT_FOUND
        val body = ApiErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = ex.message,
            path = request.requestURI
        )
        return ResponseEntity(body, HttpHeaders(), status)
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(
        ex: BadRequestException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val status = HttpStatus.BAD_REQUEST
        val body = ApiErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = ex.message,
            path = request.requestURI
        )
        return ResponseEntity(body, HttpHeaders(), status)
    }

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(
        ex: ConflictException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val status = HttpStatus.CONFLICT
        val body = ApiErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = ex.message,
            path = request.requestURI
        )
        return ResponseEntity(body, HttpHeaders(), status)
    }

    @ExceptionHandler(ForbiddenException::class, AccessDeniedException::class)
    fun handleForbidden(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val status = HttpStatus.FORBIDDEN
        val body = ApiErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = ex.message ?: "Access denied",
            path = request.requestURI
        )
        return ResponseEntity(body, HttpHeaders(), status)
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(
        ex: AuthenticationException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val status = HttpStatus.UNAUTHORIZED
        val body = ApiErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = ex.message ?: "Authentication failed",
            path = request.requestURI
        )
        return ResponseEntity(body, HttpHeaders(), status)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        val body = ApiErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = "Unexpected error",
            path = request.requestURI
        )
        // TODO: add logging here
        return ResponseEntity(body, HttpHeaders(), status)
    }
}
