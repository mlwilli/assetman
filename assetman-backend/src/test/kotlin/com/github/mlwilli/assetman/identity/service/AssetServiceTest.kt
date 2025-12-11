package com.github.mlwilli.assetman.identity.service

import com.github.mlwilli.assetman.asset.domain.Asset
import com.github.mlwilli.assetman.asset.domain.AssetStatus
import com.github.mlwilli.assetman.asset.repo.AssetRepository
import com.github.mlwilli.assetman.asset.service.AssetService
import com.github.mlwilli.assetman.asset.web.CreateAssetRequest
import com.github.mlwilli.assetman.asset.web.UpdateAssetRequest
import com.github.mlwilli.assetman.common.error.NotFoundException
import com.github.mlwilli.assetman.common.security.AuthenticatedUser
import com.github.mlwilli.assetman.common.security.TenantContext
import com.github.mlwilli.assetman.location.domain.Location
import com.github.mlwilli.assetman.location.domain.LocationType
import com.github.mlwilli.assetman.location.repo.LocationRepository
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class AssetServiceTest {

    private lateinit var assetRepository: AssetRepository
    private lateinit var locationRepository: LocationRepository
    private lateinit var service: AssetService

    private val tenantId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        assetRepository = Mockito.mock(AssetRepository::class.java)
        locationRepository = Mockito.mock(LocationRepository::class.java)

        // AssetService(assetRepository, locationRepository)
        service = AssetService(assetRepository, locationRepository)

        TenantContext.set(
            AuthenticatedUser(
                userId = userId,
                tenantId = tenantId,
                email = "user@tenant.test",
                roles = setOf("ADMIN")
            )
        )
    }

    @AfterEach
    fun tearDown() {
        TenantContext.clear()
    }

    // ----------------------------------------------------------------------
    // createAsset
    // ----------------------------------------------------------------------

    @Test
    fun `createAsset persists asset with tenantId and returns mapped dto`() {
        val request = CreateAssetRequest(
            name = "Boiler Pump",
            status = AssetStatus.IN_SERVICE,
            category = "HVAC",
            code = "ASSET-001",
            assetTag = "TAG-001",
            manufacturer = "Acme",
            model = "BP-100",
            serialNumber = "SN-123",
            tags = listOf("mechanical", "boiler-room"),
            purchaseDate = LocalDate.now().minusDays(10),
            purchaseCost = BigDecimal("5000.00"),
            inServiceDate = LocalDate.now().minusDays(5),
            retiredDate = null,
            disposedDate = null,
            warrantyExpiryDate = LocalDate.now().plusYears(2),
            depreciationYears = 10,
            residualValue = BigDecimal("500.00"),
            locationId = UUID.randomUUID(),
            propertyId = null,
            unitId = null,
            assignedUserId = userId,
            externalRef = "ERP-123",
            customFieldsJson = """{"rpm":1800}"""
        )

        val saved = Asset(
            tenantId = tenantId,
            name = request.name,
            status = request.status,
            category = request.category,
            code = request.code,
            assetTag = request.assetTag,
            manufacturer = request.manufacturer,
            model = request.model,
            serialNumber = request.serialNumber,
            tags = request.tags?.joinToString(","),
            purchaseDate = request.purchaseDate,
            purchaseCost = request.purchaseCost,
            inServiceDate = request.inServiceDate,
            retiredDate = request.retiredDate,
            disposedDate = request.disposedDate,
            warrantyExpiryDate = request.warrantyExpiryDate,
            depreciationYears = request.depreciationYears,
            residualValue = request.residualValue,
            locationId = request.locationId,
            propertyId = request.propertyId,
            unitId = request.unitId,
            assignedUserId = request.assignedUserId,
            externalRef = request.externalRef,
            customFieldsJson = request.customFieldsJson
        )
        setBaseFields(saved, UUID.randomUUID())

        Mockito.`when`(
            assetRepository.save(Mockito.any(Asset::class.java))
        ).thenReturn(saved)

        val dto = service.createAsset(request)

        // verify entity persisted correctly
        Mockito.verify(assetRepository).save(Mockito.any(Asset::class.java))

        assertEquals(saved.id, dto.id)
        assertEquals(tenantId, dto.tenantId)
        assertEquals("Boiler Pump", dto.name)
        assertEquals(AssetStatus.IN_SERVICE, dto.status)
        assertEquals(listOf("mechanical", "boiler-room"), dto.tags)
    }

    // ----------------------------------------------------------------------
    // getAsset
    // ----------------------------------------------------------------------

    @Test
    fun `getAsset returns dto when asset exists for tenant`() {
        val id = UUID.randomUUID()
        val asset = Asset(
            tenantId = tenantId,
            name = "Core Switch",
            status = AssetStatus.IN_SERVICE
        ).apply {
            category = "IT"
            serialNumber = "SN-SW"
            setBaseFields(this, id)
        }

        Mockito.`when`(
            assetRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(asset)

        val dto = service.getAsset(id)

        assertEquals(id, dto.id)
        assertEquals(tenantId, dto.tenantId)
        assertEquals("Core Switch", dto.name)
        assertEquals(AssetStatus.IN_SERVICE, dto.status)
        assertEquals("IT", dto.category)
    }

    @Test
    fun `getAsset throws NotFoundException when asset missing for tenant`() {
        val id = UUID.randomUUID()

        Mockito.`when`(
            assetRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(null)

        assertThrows<NotFoundException> {
            service.getAsset(id)
        }
    }

    // ----------------------------------------------------------------------
    // listAssetsForCurrentTenant (simple)
    // ----------------------------------------------------------------------

    @Test
    fun `listAssetsForCurrentTenant calls repository search with filters and maps page`() {
        val pageable = PageRequest.of(0, 20)

        val asset = Asset(
            tenantId = tenantId,
            name = "Printer 1",
            status = AssetStatus.IN_SERVICE
        ).apply {
            category = "IT"
            setBaseFields(this, UUID.randomUUID())
        }

        val page = PageImpl(listOf(asset), pageable, 1)

        Mockito.`when`(
            assetRepository.search(
                tenantId = tenantId,
                status = AssetStatus.IN_SERVICE,
                category = "IT",
                locationIds = null,
                propertyId = null,
                unitId = null,
                assignedUserId = null,
                search = "printer",
                pageable = pageable
            )
        ).thenReturn(page)

        val result = service.listAssetsForCurrentTenant(
            status = AssetStatus.IN_SERVICE,
            category = "IT",
            locationId = null,
            propertyId = null,
            unitId = null,
            assignedUserId = null,
            search = "printer",
            pageable = pageable
        )

        assertEquals(1, result.totalElements)
        val dto = result.content.first()
        assertEquals(asset.id, dto.id)
        assertEquals("Printer 1", dto.name)
        assertEquals("IT", dto.category)
    }

    // ----------------------------------------------------------------------
    // listAssetsForCurrentTenant - location subtree
    // ----------------------------------------------------------------------

    @Test
    fun `listAssetsForCurrentTenant filters by location subtree using location path`() {
        val pageable = PageRequest.of(0, 10)

        val rootId = UUID.randomUUID()
        val childId = UUID.randomUUID()

        val rootLocation = Location(
            tenantId = tenantId,
            name = "HQ",
            type = LocationType.SITE
        ).apply {
            path = "/$rootId"
            setBaseFields(this, rootId)
        }

        val childLocation = Location(
            tenantId = tenantId,
            name = "Server Room",
            type = LocationType.ROOM,
            parentId = rootId,
            path = "/$rootId/$childId"
        ).apply {
            setBaseFields(this, childId)
        }

        // Service will call findByIdAndTenantId for the root filter location
        Mockito.`when`(
            locationRepository.findByIdAndTenantId(rootId, tenantId)
        ).thenReturn(rootLocation)

        // Service computes pathPrefix = root.path.trimEnd('/') + "/"
        val pathPrefix = rootLocation.path!!.trimEnd('/') + "/"

        Mockito.`when`(
            locationRepository.findAllByTenantIdAndPathStartingWith(
                tenantId = tenantId,
                pathPrefix = pathPrefix
            )
        ).thenReturn(listOf(childLocation))

        // Expected subtree IDs: children from repo + rootId
        val subtreeIds = listOf(childId, rootId)

        val asset = Asset(
            tenantId = tenantId,
            name = "Core Server",
            status = AssetStatus.IN_SERVICE
        ).apply {
            locationId = childId
            setBaseFields(this, UUID.randomUUID())
        }

        val page = PageImpl(listOf(asset), pageable, 1)

        Mockito.`when`(
            assetRepository.search(
                tenantId = tenantId,
                status = AssetStatus.IN_SERVICE,
                category = null,
                locationIds = subtreeIds,
                propertyId = null,
                unitId = null,
                assignedUserId = null,
                search = null,
                pageable = pageable
            )
        ).thenReturn(page)

        val result = service.listAssetsForCurrentTenant(
            status = AssetStatus.IN_SERVICE,
            category = null,
            locationId = rootId,
            propertyId = null,
            unitId = null,
            assignedUserId = null,
            search = null,
            pageable = pageable
        )

        assertEquals(1, result.totalElements)
        val dto = result.content.first()
        assertEquals("Core Server", dto.name)
        assertEquals(childId, dto.locationId)
    }

    // ----------------------------------------------------------------------
    // updateAsset
    // ----------------------------------------------------------------------

    @Test
    fun `updateAsset updates fields and returns updated dto`() {
        val id = UUID.randomUUID()

        val existing = Asset(
            tenantId = tenantId,
            name = "Old Name",
            status = AssetStatus.PLANNED
        ).apply {
            category = "OLD"
            code = "OLD"
            assetTag = "OLD-TAG"
            manufacturer = "OldCo"
            model = "O-1"
            serialNumber = "OLD-SN"
            tags = "old"
            purchaseDate = LocalDate.now().minusDays(100)
            purchaseCost = BigDecimal("100.00")
            inServiceDate = null
            retiredDate = null
            disposedDate = null
            warrantyExpiryDate = null
            depreciationYears = 3
            residualValue = BigDecimal("10.00")
            locationId = null
            propertyId = null
            unitId = null
            assignedUserId = null
            externalRef = "OLD-REF"
            customFieldsJson = """{"old":true}"""
            setBaseFields(this, id)
        }

        Mockito.`when`(
            assetRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(existing)

        Mockito.`when`(
            assetRepository.save(existing)
        ).thenReturn(existing)

        val request = UpdateAssetRequest(
            name = "New Name",
            status = AssetStatus.IN_SERVICE,
            category = "IT",
            code = "ASSET-002",
            assetTag = "TAG-002",
            manufacturer = "Dell",
            model = "R740",
            serialNumber = "SN-999",
            tags = listOf("server", "db"),
            purchaseDate = LocalDate.now().minusDays(10),
            purchaseCost = BigDecimal("7000.00"),
            inServiceDate = LocalDate.now().minusDays(5),
            retiredDate = null,
            disposedDate = null,
            warrantyExpiryDate = LocalDate.now().plusYears(4),
            depreciationYears = 5,
            residualValue = BigDecimal("700.00"),
            locationId = UUID.randomUUID(),
            propertyId = null,
            unitId = null,
            assignedUserId = userId,
            externalRef = "ERP-999",
            customFieldsJson = """{"rack":"R2"}"""
        )

        val dto = service.updateAsset(id, request)

        // entity mutated
        assertEquals("New Name", existing.name)
        assertEquals(AssetStatus.IN_SERVICE, existing.status)
        assertEquals("IT", existing.category)
        assertEquals("ASSET-002", existing.code)
        assertEquals("TAG-002", existing.assetTag)
        assertEquals("Dell", existing.manufacturer)
        assertEquals("R740", existing.model)
        assertEquals("SN-999", existing.serialNumber)
        assertEquals("server,db", existing.tags)
        assertEquals(request.purchaseDate, existing.purchaseDate)
        assertEquals(request.purchaseCost, existing.purchaseCost)
        assertEquals(request.inServiceDate, existing.inServiceDate)
        assertEquals(request.warrantyExpiryDate, existing.warrantyExpiryDate)
        assertEquals(request.depreciationYears, existing.depreciationYears)
        assertEquals(request.residualValue, existing.residualValue)
        assertEquals(request.locationId, existing.locationId)
        assertEquals(request.assignedUserId, existing.assignedUserId)
        assertEquals("ERP-999", existing.externalRef)
        assertEquals("""{"rack":"R2"}""", existing.customFieldsJson)

        // dto basics
        assertEquals("New Name", dto.name)
        assertEquals(AssetStatus.IN_SERVICE, dto.status)
        assertEquals(listOf("server", "db"), dto.tags)
    }

    @Test
    fun `updateAsset throws NotFoundException when asset is missing for tenant`() {
        val id = UUID.randomUUID()

        Mockito.`when`(
            assetRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(null)

        assertThrows<NotFoundException> {
            service.updateAsset(
                id,
                UpdateAssetRequest(
                    name = "Does not matter",
                    status = AssetStatus.PLANNED
                )
            )
        }
    }

    // ----------------------------------------------------------------------
    // deleteAsset
    // ----------------------------------------------------------------------

    @Test
    fun `deleteAsset deletes entity when found`() {
        val id = UUID.randomUUID()

        val asset = Asset(
            tenantId = tenantId,
            name = "To Delete",
            status = AssetStatus.RETIRED
        ).apply {
            setBaseFields(this, id)
        }

        Mockito.`when`(
            assetRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(asset)

        service.deleteAsset(id)

        Mockito.verify(assetRepository).delete(asset)
    }

    @Test
    fun `deleteAsset throws NotFoundException when asset missing`() {
        val id = UUID.randomUUID()

        Mockito.`when`(
            assetRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(null)

        assertThrows<NotFoundException> {
            service.deleteAsset(id)
        }
    }

    // ----------------------------------------------------------------------
    // getAssetByExternalRef
    // ----------------------------------------------------------------------

    @Test
    fun `getAssetByExternalRef returns dto when asset exists for tenant`() {
        val externalRef = "ERP-123"

        val asset = Asset(
            tenantId = tenantId,
            name = "Linked Asset",
            status = AssetStatus.IN_SERVICE
        ).apply {
            this.externalRef = externalRef
            setBaseFields(this, UUID.randomUUID())
        }

        Mockito.`when`(
            assetRepository.findByTenantIdAndExternalRef(tenantId, externalRef)
        ).thenReturn(asset)

        val dto = service.getAssetByExternalRef(externalRef)

        assertEquals(asset.id, dto.id)
        assertEquals(tenantId, dto.tenantId)
        assertEquals("Linked Asset", dto.name)
        assertEquals(externalRef, dto.externalRef)
    }

    @Test
    fun `getAssetByExternalRef throws NotFoundException when asset missing for tenant`() {
        val externalRef = "MISSING-REF"

        Mockito.`when`(
            assetRepository.findByTenantIdAndExternalRef(tenantId, externalRef)
        ).thenReturn(null)

        assertThrows<NotFoundException> {
            service.getAssetByExternalRef(externalRef)
        }
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    private fun setBaseFields(entity: Any, id: UUID) {
        var clazz: Class<*>? = entity.javaClass

        while (clazz != null) {
            try {
                val idField = clazz.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(entity, id)

                val createdField = clazz.getDeclaredField("createdAt")
                createdField.isAccessible = true
                createdField.set(entity, Instant.now())

                val updatedField = clazz.getDeclaredField("updatedAt")
                updatedField.isAccessible = true
                updatedField.set(entity, Instant.now())
                return
            } catch (_: NoSuchFieldException) {
                // walk up base classes
            }
            clazz = clazz.superclass
        }

        error("id / createdAt / updatedAt not found in class hierarchy of ${entity.javaClass}")
    }
}
