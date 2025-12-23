package com.github.mlwilli.assetman.identity.web

import com.github.mlwilli.assetman.common.security.AuthenticatedUser
import com.github.mlwilli.assetman.common.security.CompanySelectionRequiredFilter
import com.github.mlwilli.assetman.common.security.JwtAuthenticationFilter
import com.github.mlwilli.assetman.common.security.JwtTokenProvider
import com.github.mlwilli.assetman.common.security.JsonAccessDeniedHandler
import com.github.mlwilli.assetman.common.security.JsonAuthenticationEntryPoint
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID

@WebMvcTest(controllers = [UserDirectoryController::class])
@Import(
    UserDirectoryControllerSecurityTest.TestSecurityConfig::class,
    JwtAuthenticationFilter::class,
    CompanySelectionRequiredFilter::class,
    JsonAuthenticationEntryPoint::class,
    JsonAccessDeniedHandler::class
)
class UserDirectoryControllerSecurityTest {

    @TestConfiguration
    @EnableMethodSecurity
    class TestSecurityConfig {
        @Bean
        fun testSecurityFilterChain(
            http: HttpSecurity,
            jwtAuthenticationFilter: JwtAuthenticationFilter,
            companySelectionRequiredFilter: CompanySelectionRequiredFilter,
            authenticationEntryPoint: JsonAuthenticationEntryPoint,
            accessDeniedHandler: JsonAccessDeniedHandler
        ): SecurityFilterChain {
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
                        .requestMatchers("/actuator/health", "/api/ping").permitAll()
                        // keep it simple for this controller test:
                        .anyRequest().authenticated()
                }
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
                .addFilterAfter(companySelectionRequiredFilter, JwtAuthenticationFilter::class.java)

            return http.build()
        }
    }

    @Autowired lateinit var mockMvc: MockMvc

    @MockBean lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean lateinit var service: com.github.mlwilli.assetman.identity.service.UserDirectoryService


    private fun principal(
        roles: Set<String>,
        companyId: UUID? = UUID.randomUUID()
    ) = AuthenticatedUser(
        userId = UUID.randomUUID(),
        tenantId = UUID.randomUUID(),
        email = "tester@tenant.test",
        roles = roles,
        companyId = companyId
    )

    @Test
    fun `GET directory - unauthenticated returns 401`() {
        mockMvc.get("/api/users/directory")
            .andExpect { status { isUnauthorized() } }

        verify(service, never()).listDirectory(
            org.mockito.kotlin.anyOrNull(),
            org.mockito.kotlin.any(),
            org.mockito.kotlin.any()
        )
    }

    @Test
    fun `GET directory - MANAGER activeOnly=true allowed`() {
        val token = "t1"
        given(jwtTokenProvider.parseToken(token)).willReturn(principal(setOf("MANAGER")))
        given(service.listDirectory(null, 20, true)).willReturn(emptyList())

        mockMvc.get("/api/users/directory") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            param("limit", "20")
            param("activeOnly", "true")
        }.andExpect { status { isOk() } }

        verify(service).listDirectory(null, 20, true)
    }

    @Test
    fun `GET directory - MANAGER activeOnly=false forbidden by PreAuthorize`() {
        val token = "t2"
        given(jwtTokenProvider.parseToken(token)).willReturn(principal(setOf("MANAGER")))

        mockMvc.get("/api/users/directory") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            param("limit", "20")
            param("activeOnly", "false")
        }.andExpect { status { isForbidden() } }

        verify(service, never()).listDirectory(
            org.mockito.kotlin.anyOrNull(),
            org.mockito.kotlin.any(),
            org.mockito.kotlin.any()
        )
    }

    @Test
    fun `GET user - TECHNICIAN allowed`() {
        val token = "t3"
        given(jwtTokenProvider.parseToken(token)).willReturn(principal(setOf("TECHNICIAN")))

        val id = UUID.randomUUID()
        given(service.getUser(id)).willReturn(
            UserDirectoryDto(
                id = id,
                email = "u@t.test",
                fullName = "User",
                displayName = null,
                active = true
            )
        )

        mockMvc.get("/api/users/$id") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.andExpect { status { isOk() } }

        verify(service).getUser(id)
    }

    @Test
    fun `GET directory - authenticated but companyId missing is blocked by company selection filter`() {
        val token = "t4"
        given(jwtTokenProvider.parseToken(token)).willReturn(principal(setOf("ADMIN"), companyId = null))

        mockMvc.get("/api/users/directory") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.andExpect { status { isConflict() } }   // <- was isForbidden()

        verify(service, never()).listDirectory(
            org.mockito.kotlin.anyOrNull(),
            org.mockito.kotlin.any(),
            org.mockito.kotlin.any()
        )
    }

}
