package com.github.mlwilli.assetman.shared.security

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

                if (user != null) {
                    TenantContext.set(user)

                    val auth = UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.roles.map { SimpleGrantedAuthority("ROLE_$it") }
                    )

                    SecurityContextHolder.getContext().authentication = auth
                }
            }

            chain.doFilter(request, response)

        } finally {
            TenantContext.clear()
            SecurityContextHolder.clearContext()
        }
    }

    override fun shouldNotFilter(req: HttpServletRequest): Boolean {
        // Public endpoints
        return req.requestURI.startsWith("/api/auth") ||
               req.requestURI.startsWith("/actuator/health")
    }
}
