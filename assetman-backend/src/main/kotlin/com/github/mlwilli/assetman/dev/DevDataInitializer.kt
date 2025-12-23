package com.github.mlwilli.assetman.dev

import com.github.mlwilli.assetman.asset.domain.Asset
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
import com.github.mlwilli.assetman.asset.service.AssetService
import com.github.mlwilli.assetman.asset.domain.AssetStatus
import com.github.mlwilli.assetman.asset.repo.AssetRepository
import com.github.mlwilli.assetman.asset.web.CreateAssetRequest
import com.github.mlwilli.assetman.common.security.TenantContext
import java.math.BigDecimal

@Component
class DevDataInitializer(
    private val env: Environment,
    private val tenantRepository: TenantRepository,
    private val userRepository: UserRepository,
    private val locationRepository: LocationRepository,
    private val assetRepository: AssetRepository,
    private val passwordEncoder: PasswordEncoder
) : ApplicationRunner {



    private val log = LoggerFactory.getLogger(DevDataInitializer::class.java)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (!env.activeProfiles.contains("dev")) return

        val tenant = seedTenantAndAdmin()
        seedLocationsIfEmpty(tenant)

        val adminEmail = (System.getenv("ASSETMAN_DEV_ADMIN_EMAIL") ?: "admin@demo.com")
            .trim()
            .lowercase()

        val admin = userRepository.findByTenantIdAndEmailIgnoreCase(tenant.id, adminEmail)
            ?: run {
                log.warn("DEV admin user not found after seed; skipping asset seed")
                return
            }

        TenantContext.withTenantUser(
            tenantId = tenant.id,
            userId = admin.id,
            email = admin.email,
            roles = admin.roles.map { it.name }.toSet()
        ) {
            seedAssetsIfEmpty(tenant)
        }

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

    private fun seedAssetsIfEmpty(tenant: Tenant) {
        val existing = assetRepository.countByTenantId(tenant.id)
        // WARNING: count() is global. Prefer tenant-scoped if you have it.
        // If you add a tenant-scoped count later, switch to that.
        if (existing > 0) {
            log.info("DEV assets already exist (global count={})", existing)
            return
        }

        val locations = locationRepository.findAllByTenantIdOrderByNameAsc(tenant.id)
        val hq = locations.firstOrNull { it.code == "HQ" } ?: locations.firstOrNull()

        log.info("Seeding DEV assets for tenant='{}'", tenant.slug)

        val assets = listOf(
            Asset(
                tenantId = tenant.id,
                name = "Dell Server R740",
                status = AssetStatus.IN_SERVICE,
                category = "IT",
                code = "ASSET-0001",
                assetTag = "TAG-0001",
                manufacturer = "Dell",
                model = "PowerEdge R740",
                serialNumber = "SRV-R740-0001",
                tags = "server,critical",
                locationId = hq?.id,
                purchaseCost = BigDecimal("12500.00"),
                depreciationYears = 5,
                externalRef = "DEV-EXT-0001",
                customFieldsJson = """{"rack":"R12","u":"14","ipAddress":"10.0.0.5"}"""
            ),
            Asset(
                tenantId = tenant.id,
                name = "HVAC Unit - Building A",
                status = AssetStatus.UNDER_MAINTENANCE,
                category = "HVAC",
                code = "ASSET-0002",
                assetTag = "TAG-0002",
                manufacturer = "Trane",
                model = "RTU-500",
                serialNumber = "HVAC-0002",
                tags = "hvac,roof",
                locationId = locations.firstOrNull { it.code == "BLDG-A" }?.id ?: hq?.id,
                purchaseCost = java.math.BigDecimal("22000.00"),
                depreciationYears = 10,
                externalRef = "DEV-EXT-0002"
            ),
            Asset(
                tenantId = tenant.id,
                name = "Forklift - Warehouse",
                status = AssetStatus.IN_SERVICE,
                category = "VEHICLE",
                code = "ASSET-0003",
                assetTag = "TAG-0003",
                manufacturer = "Toyota",
                model = "8FGCU25",
                serialNumber = "FL-0003",
                tags = "warehouse,forklift",
                locationId = locations.firstOrNull { it.code == "WH" }?.id ?: hq?.id,
                purchaseCost = java.math.BigDecimal("18000.00"),
                depreciationYears = 7,
                externalRef = "DEV-EXT-0003"
            )
        )

        assetRepository.saveAll(assets)

        log.info("DEV asset seed complete (created={})", assets.size)
    }


}
