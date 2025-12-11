package com.github.mlwilli.assetman.dev

import com.github.mlwilli.assetman.asset.domain.Asset
import com.github.mlwilli.assetman.asset.domain.AssetStatus
import com.github.mlwilli.assetman.asset.repo.AssetRepository
import com.github.mlwilli.assetman.identity.domain.Role
import com.github.mlwilli.assetman.identity.domain.Tenant
import com.github.mlwilli.assetman.identity.domain.User
import com.github.mlwilli.assetman.identity.repo.TenantRepository
import com.github.mlwilli.assetman.identity.repo.UserRepository
import com.github.mlwilli.assetman.location.domain.Location
import com.github.mlwilli.assetman.location.domain.LocationType
import com.github.mlwilli.assetman.location.repo.LocationRepository
import com.github.mlwilli.assetman.property.domain.Property
import com.github.mlwilli.assetman.property.domain.PropertyType
import com.github.mlwilli.assetman.property.domain.Unit
import com.github.mlwilli.assetman.property.domain.UnitStatus
import com.github.mlwilli.assetman.property.repo.PropertyRepository
import com.github.mlwilli.assetman.property.repo.UnitRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.security.crypto.password.PasswordEncoder
import java.math.BigDecimal
import java.time.LocalDate

@Profile("dev")
@Component
class DevDataInitializer(
    private val tenantRepository: TenantRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val locationRepository: LocationRepository,
    private val propertyRepository: PropertyRepository,
    private val unitRepository: UnitRepository,
    private val assetRepository: AssetRepository
) : CommandLineRunner {

    // Test Values. Obviously fake data

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String?) {
        // Avoid reseeding if demo tenant already exists
        val existing = tenantRepository.findBySlug("demo")
        if (existing != null) {
            log.info("DevDataInitializer: demo tenant already exists, skipping seeding.")
            return
        }

        log.info("DevDataInitializer: seeding demo data...")

        // --- Tenant ---
        val tenant = tenantRepository.save(
            Tenant(
                name = "Demo Organization",
                slug = "demo"
            )
        )

        // --- Users ---
        val ownerAdminUser = userRepository.save(
            User(
                tenantId = tenant.id,
                fullName = "Demo Owner",
                email = "owner@demo.test",
                passwordHash = passwordEncoder.encode("Password123!"),
                displayName = "Demo Owner",
                roles = setOf(Role.OWNER, Role.ADMIN) as MutableSet<Role>,
                active = true
            )
        )

        // --- Locations ---
        val hqLocation = locationRepository.save(
            Location(
                tenantId = tenant.id,
                name = "HQ - Main Campus",
                type = LocationType.SITE,
                parentId = null,
                path = null
            )
        )

        val buildingALocation = locationRepository.save(
            Location(
                tenantId = tenant.id,
                name = "Building A - Floor 1",
                type = LocationType.FLOOR,
                parentId = hqLocation.id,
                path = null
            )
        )

        // --- Properties ---
        val demoProperty = propertyRepository.save(
            Property(
                tenantId = tenant.id,
                name = "Demo Property",
                type = PropertyType.RESIDENTIAL,
                notes = "Sample property for dev/testing",
                code = "PROP-DEMO-001",
                addressLine1 = "123 Demo Street",
                addressLine2 = null,
                city = "Demo City",
                state = "DE",
                postalCode = "12345",
                country = "US",
                locationId = buildingALocation.id
            )
        )

        // --- Units ---
        val unit101 = unitRepository.save(
            Unit(
                tenantId = tenant.id,
                propertyId = demoProperty.id,
                name = "Unit 101",
                notes = "Sample unit 101",
                status = UnitStatus.OCCUPIED,
                bedrooms = 2,
                bathrooms = 1,
                floor = "1",
                areaSqFt = BigDecimal("850"),
                currency = "USD",
                monthlyRent = BigDecimal("1500.00")
            )
        )

        // --- Assets ---
        val boilerAsset = assetRepository.save(
            Asset(
                tenantId = tenant.id,
                name = "Boiler - Unit 101",
                status = AssetStatus.IN_SERVICE,
                tags = "boiler, hvac",
                warrantyExpiryDate = LocalDate.now().plusYears(1),
                purchaseCost = BigDecimal("5000.00"),
                customFieldsJson = """{"model":"B-1000","manufacturer":"Acme Heating"}""",
                category = "HVAC",
                purchaseDate = LocalDate.now().minusYears(2),
                serialNumber = "HVAC-BOILER-001",
                locationId = buildingALocation.id,
                assignedUserId = ownerAdminUser.id
            )
        )

        log.info("DevDataInitializer: demo data seeded.")
        log.info("Dev tenant slug: demo")
        log.info("Dev owner/admin email: owner@demo.test")
        log.info("Dev owner/admin password: Password123!")
        log.info("Sample asset id: {}", boilerAsset.id)
    }
}
