package com.github.mlwilli.assetman.common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        try {
            val header = request.getHeader("Authorization")

            if (header != null && header.startsWith("Bearer ")) {
                val token = header.substring(7)

                val user = try {
                    jwtTokenProvider.parseToken(token)
                } catch (ex: Exception) {
                    // Any parse/validation failure must be treated as "unauthenticated"
                    // so clients can refresh tokens correctly.
                    SecurityContextHolder.clearContext()
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token")
                    return
                }

                if (user != null) {
                    // Populate TenantContext for domain services
                    TenantContext.set(user)

                    // Populate Spring Security context for @PreAuthorize, etc.
                    val auth = UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.roles.map { SimpleGrantedAuthority("ROLE_$it") }
                    )
                    SecurityContextHolder.getContext().authentication = auth
                } else {
                    // Token present but did not resolve to a user => unauthenticated
                    SecurityContextHolder.clearContext()
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token")
                    return
                }
            }

            chain.doFilter(request, response)
        } finally {
            // Per-request cleanup
            // TenantContext is ours to manage per request.
            TenantContext.clear()

            // IMPORTANT:
            // Do NOT clear SecurityContextHolder here. Spring Security manages it.
            // Clearing it here can interfere with downstream authorization/exception handling.
        }
    }

    // No shouldNotFilter – we let the filter run for all endpoints.
}
