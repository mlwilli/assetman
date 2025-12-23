package com.github.mlwilli.assetman.common.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mlwilli.assetman.common.web.ApiErrorResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class CompanySelectionRequiredFilter(
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    private fun isAllowlisted(request: HttpServletRequest): Boolean {
        val path = request.requestURI

        if (path.startsWith("/api/auth")) return true
        if (path.startsWith("/api/companies")) return true
        if (path == "/api/me") return true

        if (path == "/actuator/health") return true
        if (path == "/api/ping") return true

        return false
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        if (!request.requestURI.startsWith("/api/")) {
            chain.doFilter(request, response)
            return
        }

        val principal = TenantContext.get()
        if (principal == null) {
            chain.doFilter(request, response)
            return
        }

        if (principal.companyId == null && !isAllowlisted(request)) {
            // Return a response instead of throwing (prevents test/runtime bubbling)
            val status = HttpStatus.CONFLICT // or FORBIDDEN if you prefer 403
            response.status = status.value()
            response.contentType = "application/json"

            val body = ApiErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                message = "Company selection required. Select a company to continue.",
                path = request.requestURI
            )

            response.writer.write(objectMapper.writeValueAsString(body))
            return
        }

        chain.doFilter(request, response)
    }
}
