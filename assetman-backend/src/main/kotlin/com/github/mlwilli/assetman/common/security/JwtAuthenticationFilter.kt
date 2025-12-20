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

                val user = jwtTokenProvider.parseToken(token)

                // Invalid token -> treat as anonymous; do NOT short-circuit response
                if (user == null) {
                    TenantContext.clear()
                    SecurityContextHolder.clearContext()
                    chain.doFilter(request, response)
                    return
                }


                // Populate TenantContext for domain services
                TenantContext.set(user)

                // Populate Spring Security context for @PreAuthorize, etc.
                val auth = UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    user.roles.map { SimpleGrantedAuthority("ROLE_$it") }
                )
                SecurityContextHolder.getContext().authentication = auth
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
