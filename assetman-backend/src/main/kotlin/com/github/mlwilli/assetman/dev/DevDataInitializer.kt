package com.github.mlwilli.assetman.dev

import com.github.mlwilli.assetman.identity.domain.Role
import com.github.mlwilli.assetman.identity.domain.Tenant
import com.github.mlwilli.assetman.identity.domain.User
import com.github.mlwilli.assetman.identity.repo.TenantRepository
import com.github.mlwilli.assetman.identity.repo.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.boot.context.event.ApplicationReadyEvent

@Component
class DevDataInitializer(
    private val env: Environment,
    private val tenantRepository: TenantRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    private val log = LoggerFactory.getLogger(DevDataInitializer::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun seed() {
        if (!env.activeProfiles.contains("dev")) return
        //todo: Demo auth removal
        val tenantSlug = System.getenv("ASSETMAN_DEV_TENANT_SLUG") ?: "demo"
        val tenantName = System.getenv("ASSETMAN_DEV_TENANT_NAME") ?: "Demo Tenant"
        val adminEmail = System.getenv("ASSETMAN_DEV_ADMIN_EMAIL") ?: "admin@demo.com"
        val adminName = System.getenv("ASSETMAN_DEV_ADMIN_NAME") ?: "Demo Admin"
        val adminPassword = System.getenv("ASSETMAN_DEV_ADMIN_PASSWORD") ?: "DemoPass!"

        val tenant = tenantRepository.findBySlug(tenantSlug)
            ?: tenantRepository.save(Tenant(name = tenantName, slug = tenantSlug))

        val existingAdmin = userRepository.findByTenantIdAndEmailIgnoreCase(tenant.id, adminEmail)

        if (existingAdmin == null) {
            val user = User(
                tenantId = tenant.id,
                email = adminEmail,
                fullName = adminName,
                passwordHash = passwordEncoder.encode(adminPassword),
                roles = setOf(Role.OWNER, Role.ADMIN)
            )
            userRepository.save(user)
            log.info("DEV seed created: tenantSlug='{}', adminEmail='{}'", tenantSlug, adminEmail)
        } else {
            log.info("DEV seed exists: tenantSlug='{}', adminEmail='{}'", tenantSlug, adminEmail)
        }
    }
}
