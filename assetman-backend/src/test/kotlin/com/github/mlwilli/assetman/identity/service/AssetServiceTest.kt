package com.github.mlwilli.assetman.identity.service

import com.github.mlwilli.assetman.asset.domain.Asset
import com.github.mlwilli.assetman.asset.domain.AssetStatus
import com.github.mlwilli.assetman.asset.repo.AssetRepository
import com.github.mlwilli.assetman.asset.service.AssetService
import com.github.mlwilli.assetman.asset.web.CreateAssetRequest
import com.github.mlwilli.assetman.common.security.AuthenticatedUser
import com.github.mlwilli.assetman.common.security.TenantContext
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class AssetServiceTest {

    private lateinit var assetRepository: AssetRepository
    private lateinit var assetService: AssetService

    private val tenantId: UUID = UUID.randomUUID()
    private val userId: UUID = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        assetRepository = Mockito.mock(AssetRepository::class.java)
        assetService = AssetService(assetRepository)

        TenantContext.set(
            AuthenticatedUser(
                userId = userId,
                tenantId = tenantId,
                email = "user@tenant.test",
                roles = setOf("USER")
            )
        )
    }

    @AfterEach
    fun cleanup() {
        TenantContext.clear()
    }

    @Test
    fun `createAsset saves asset for current tenant and returns dto`() {
        val request = CreateAssetRequest(
            name = "Boiler Unit 1",
            status = AssetStatus.IN_SERVICE,
            category = "HVAC",
            serialNumber = "SN-123",
            tags = listOf("boiler", "hvac"),
            purchaseDate = LocalDate.of(2020, 1, 1),
            purchaseCost = BigDecimal("5000.00"),
            locationId = UUID.randomUUID(),
            assignedUserId = userId,
            warrantyExpiryDate = LocalDate.of(2026, 1, 1),
            customFieldsJson = """{"model":"X100"}"""
        )

        val savedAsset = Asset(
            tenantId = tenantId,
            name = request.name,
            status = request.status!!,
            tags = request.tags!!.joinToString(","),
            category = request.category,
            serialNumber = request.serialNumber,
            purchaseDate = request.purchaseDate,
            purchaseCost = request.purchaseCost,
            locationId = request.locationId,
            assignedUserId = request.assignedUserId,
            warrantyExpiryDate = request.warrantyExpiryDate,
            customFieldsJson = request.customFieldsJson
        )

        whenever(assetRepository.save(any())).thenReturn(savedAsset)

        val dto = assetService.createAsset(request)

        assertEquals(savedAsset.name, dto.name)
        assertEquals(savedAsset.status, dto.status)
        assertEquals(listOf("boiler", "hvac"), dto.tags)
        assertEquals(tenantId, dto.tenantId)
    }

    @Test
    fun `listAssetsForCurrentTenant returns mapped dtos`() {
        val pageable = PageRequest.of(0, 10)

        val asset = Asset(
            tenantId = tenantId,
            name = "Pump 1",
            status = AssetStatus.IN_SERVICE,
            tags = "pump,water",
            category = "PLUMBING",
            serialNumber = "P-123",
            purchaseDate = LocalDate.of(2021, 5, 10),
            purchaseCost = BigDecimal("1500.00"),
            locationId = UUID.randomUUID(),
            assignedUserId = userId,
            warrantyExpiryDate = null,
            customFieldsJson = null
        )

        val page: Page<Asset> = PageImpl(listOf(asset), pageable, 1)

        whenever(
            assetRepository.search(
                eq(tenantId),
                eq(AssetStatus.IN_SERVICE),
                eq("pump"),
                eq(pageable)
            )
        ).thenReturn(page)

        val resultPage = assetService.listAssetsForCurrentTenant(
            status = AssetStatus.IN_SERVICE,
            search = "pump",
            pageable = pageable
        )

        assertEquals(1, resultPage.totalElements)
        val dto = resultPage.content.first()
        assertEquals(asset.name, dto.name)
        assertEquals(listOf("pump", "water"), dto.tags)
        assertEquals(tenantId, dto.tenantId)
    }
}
