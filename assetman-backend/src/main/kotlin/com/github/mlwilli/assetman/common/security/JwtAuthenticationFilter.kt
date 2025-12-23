package com.github.mlwilli.assetman.common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
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
            val header = request.getHeader(HttpHeaders.AUTHORIZATION)

            // No token => anonymous (SecurityConfig decides what is public)
            if (header.isNullOrBlank() || !header.startsWith("Bearer ")) {
                chain.doFilter(request, response)
                return
            }

            val token = header.substring(7).trim()
            val user = jwtTokenProvider.parseToken(token)

            // Invalid token => treat as anonymous; do NOT short-circuit response
            if (user == null) {
                TenantContext.clear()
                SecurityContextHolder.clearContext()
                chain.doFilter(request, response)
                return
            }

            // Populate TenantContext for services
            TenantContext.set(user)

            // Populate Spring Security context for @PreAuthorize
            val auth = UsernamePasswordAuthenticationToken(
                user,
                null,
                user.roles.map { SimpleGrantedAuthority("ROLE_$it") }
            )
            SecurityContextHolder.getContext().authentication = auth

            chain.doFilter(request, response)
        } finally {
            // Per-request cleanup (TenantContext is ours to manage)
            TenantContext.clear()
            // Do NOT clear SecurityContextHolder here; Spring Security manages it.
        }
    }
}
