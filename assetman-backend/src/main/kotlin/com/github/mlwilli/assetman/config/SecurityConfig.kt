package com.github.mlwilli.assetman.config

import com.github.mlwilli.assetman.common.security.CompanySelectionRequiredFilter
import com.github.mlwilli.assetman.common.security.JsonAccessDeniedHandler
import com.github.mlwilli.assetman.common.security.JsonAuthenticationEntryPoint
import com.github.mlwilli.assetman.common.security.JwtAuthenticationFilter
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
import org.springframework.security.web.access.ExceptionTranslationFilter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val authenticationEntryPoint: JsonAuthenticationEntryPoint,
    private val accessDeniedHandler: JsonAccessDeniedHandler,
    private val companySelectionRequiredFilter: CompanySelectionRequiredFilter
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder =
        PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    fun userDetailsService(): UserDetailsService =
        UserDetailsService { throw UsernameNotFoundException("JWT-only auth; no UserDetailsService users") }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
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
            // 1) Parse JWT -> populate TenantContext + SecurityContext
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

            // IMPORTANT:
            // Put company gate *after* ExceptionTranslationFilter so AccessDeniedException
            // is converted into an HTTP response via AccessDeniedHandler.
            .addFilterAfter(companySelectionRequiredFilter, ExceptionTranslationFilter::class.java)

        return http.build()
    }

}
