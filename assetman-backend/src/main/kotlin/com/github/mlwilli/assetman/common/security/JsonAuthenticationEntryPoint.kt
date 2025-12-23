package com.github.mlwilli.assetman.common.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mlwilli.assetman.common.web.ApiErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class JsonAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: org.springframework.security.core.AuthenticationException
    ) {
        writeError(response, request, HttpStatus.UNAUTHORIZED, "Unauthorized")
    }

    private fun writeError(
        response: HttpServletResponse,
        request: HttpServletRequest,
        status: HttpStatus,
        message: String
    ) {
        response.status = status.value()
        response.contentType = "application/json"
        val body = ApiErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = request.requestURI
        )
        response.writer.write(objectMapper.writeValueAsString(body))
    }
}

@Component
class JsonAccessDeniedHandler(
    private val objectMapper: ObjectMapper
) : AccessDeniedHandler {

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        val (status, message) =
            if (accessDeniedException is CompanySelectionRequiredException) {
                HttpStatus.CONFLICT to accessDeniedException.message
            } else {
                HttpStatus.FORBIDDEN to "Forbidden"
            }

        response.status = status.value()
        response.contentType = "application/json"

        val body = ApiErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = request.requestURI
        )

        response.writer.write(objectMapper.writeValueAsString(body))
    }
}

