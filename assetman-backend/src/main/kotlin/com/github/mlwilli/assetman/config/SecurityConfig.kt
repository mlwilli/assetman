package com.github.mlwilli.assetman.config

import com.github.mlwilli.assetman.common.security.JsonAccessDeniedHandler
import com.github.mlwilli.assetman.common.security.JsonAuthenticationEntryPoint
import com.github.mlwilli.assetman.common.security.JwtAuthenticationFilter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val authenticationEntryPoint: JsonAuthenticationEntryPoint,
    private val accessDeniedHandler: JsonAccessDeniedHandler
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder =
        PasswordEncoderFactories.createDelegatingPasswordEncoder()

    /**
     * Prevent Spring Boot from auto-creating an in-memory user + printing a generated password.
     * We do JWT-only auth; we do not load users via UserDetailsService.
     */
    @Bean
    fun userDetailsService(): UserDetailsService =
        UserDetailsService { throw UsernameNotFoundException("JWT-only auth; no UserDetailsService users") }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

            // Explicitly disable default auth mechanisms (JWT only)
            .httpBasic { it.disable() }
            .formLogin { it.disable() }

            // Return correct status codes for unauthenticated vs forbidden
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint(authenticationEntryPoint)
                ex.accessDeniedHandler(accessDeniedHandler)
            }

            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(
                        "/actuator/health",
                        "/api/ping",
                        "/api/auth/signup-tenant",
                        "/api/auth/login",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password",
                        "/api/auth/refresh"
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
