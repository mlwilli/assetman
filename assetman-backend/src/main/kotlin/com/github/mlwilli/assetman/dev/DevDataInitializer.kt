package com.github.mlwilli.assetman.dev

import com.github.mlwilli.assetman.identity.domain.Role
import com.github.mlwilli.assetman.identity.domain.Tenant
import com.github.mlwilli.assetman.identity.domain.User
import com.github.mlwilli.assetman.identity.repo.TenantRepository
import com.github.mlwilli.assetman.identity.repo.UserRepository
import com.github.mlwilli.assetman.location.domain.Location
import com.github.mlwilli.assetman.location.domain.LocationType
import com.github.mlwilli.assetman.location.repo.LocationRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.env.Environment
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class DevDataInitializer(
    private val env: Environment,
    private val tenantRepository: TenantRepository,
    private val userRepository: UserRepository,
    private val locationRepository: LocationRepository,
    private val passwordEncoder: PasswordEncoder
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(DevDataInitializer::class.java)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!env.activeProfiles.contains("dev")) return

        val tenant = seedTenantAndAdmin()
        seedLocationsIfEmpty(tenant)
    }

    private fun seedTenantAndAdmin(): Tenant {
        val tenantSlug = System.getenv("ASSETMAN_DEV_TENANT_SLUG") ?: "demo"
        val tenantName = System.getenv("ASSETMAN_DEV_TENANT_NAME") ?: "Demo Tenant"
        val adminEmail = System.getenv("ASSETMAN_DEV_ADMIN_EMAIL") ?: "admin@demo.com"
        val adminName = System.getenv("ASSETMAN_DEV_ADMIN_NAME") ?: "Demo Admin"
        val adminPassword = System.getenv("ASSETMAN_DEV_ADMIN_PASSWORD") ?: "DemoPass!"

        val slug = tenantSlug.trim().lowercase()
        val email = adminEmail.trim().lowercase()

        val tenant = tenantRepository.findBySlug(slug)
            ?: tenantRepository.save(Tenant(name = tenantName.trim(), slug = slug))

        if (userRepository.findByTenantIdAndEmailIgnoreCase(tenant.id, email) == null) {
            userRepository.save(
                User(
                    tenantId = tenant.id,
                    email = email,
                    fullName = adminName.trim(),
                    passwordHash = passwordEncoder.encode(adminPassword),
                    roles = setOf(Role.OWNER, Role.ADMIN)
                )
            )
            log.info("DEV seed created: tenant='{}', admin='{}'", slug, email)
        }

        return tenant
    }

    private fun seedLocationsIfEmpty(tenant: Tenant) {
        if (locationRepository.countByTenantId(tenant.id) > 0) {
            log.info("DEV locations already exist for tenant='{}'", tenant.slug)
            return
        }

        log.info("Seeding DEV locations for tenant='{}'", tenant.slug)

        // Root: HQ Site
        val hq = saveWithPath(
            Location(
                tenantId = tenant.id,
                name = "HQ",
                type = LocationType.SITE,
                code = "HQ",
                sortOrder = 1,
                description = "Demo headquarters site"
            )
        )

        // Building A
        val buildingA = saveWithPath(
            Location(
                tenantId = tenant.id,
                name = "Building A",
                type = LocationType.BUILDING,
                code = "BLDG-A",
                parentId = hq.id,
                sortOrder = 1
            ),
            parent = hq
        )

        // Floors
        val floor1 = saveWithPath(
            Location(
                tenantId = tenant.id,
                name = "Floor 1",
                type = LocationType.FLOOR,
                code = "F1",
                parentId = buildingA.id,
                sortOrder = 1
            ),
            parent = buildingA
        )

        val floor2 = saveWithPath(
            Location(
                tenantId = tenant.id,
                name = "Floor 2",
                type = LocationType.FLOOR,
                code = "F2",
                parentId = buildingA.id,
                sortOrder = 2
            ),
            parent = buildingA
        )

        // Rooms
        saveWithPath(
            Location(
                tenantId = tenant.id,
                name = "Room 101",
                type = LocationType.ROOM,
                code = "101",
                parentId = floor1.id,
                sortOrder = 1
            ),
            parent = floor1
        )

        saveWithPath(
            Location(
                tenantId = tenant.id,
                name = "Room 102",
                type = LocationType.ROOM,
                code = "102",
                parentId = floor1.id,
                sortOrder = 2
            ),
            parent = floor1
        )

        // Secondary building
        saveWithPath(
            Location(
                tenantId = tenant.id,
                name = "Warehouse",
                type = LocationType.BUILDING,
                code = "WH",
                parentId = hq.id,
                sortOrder = 2
            ),
            parent = hq
        )

        log.info("DEV location seed complete")
    }

    /**
     * Two-phase save to safely compute materialized path.
     */
    private fun saveWithPath(
        location: Location,
        parent: Location? = null
    ): Location {
        val saved = locationRepository.save(location)

        val parentPath = parent?.path ?: parent?.id?.let { "/$it" }
        saved.path =
            if (parentPath.isNullOrBlank()) "/${saved.id}"
            else parentPath.trimEnd('/') + "/${saved.id}"

        return locationRepository.save(saved)
    }
}
