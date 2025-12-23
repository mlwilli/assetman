package com.github.mlwilli.assetman.property.service

import com.github.mlwilli.assetman.common.security.AuthenticatedUser
import com.github.mlwilli.assetman.common.security.TenantContext
import com.github.mlwilli.assetman.property.domain.Unit
import com.github.mlwilli.assetman.property.domain.UnitStatus
import com.github.mlwilli.assetman.property.repo.PropertyRepository
import com.github.mlwilli.assetman.property.repo.UnitRepository
import com.github.mlwilli.assetman.property.web.UnitDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import com.github.mlwilli.assetman.testsupport.pageOf
import com.github.mlwilli.assetman.testsupport.setBaseEntityFields
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Pageable



class UnitServiceTest {

    private lateinit var unitRepository: UnitRepository
    private lateinit var propertyRepository: PropertyRepository
    private lateinit var service: UnitService

    private val tenantId: UUID = UUID.randomUUID()
    private val userId: UUID = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        unitRepository = mock()
        propertyRepository = mock()
        service = UnitService(unitRepository, propertyRepository)

        TenantContext.set(
            AuthenticatedUser(
                userId = userId,
                tenantId = tenantId,
                email = "tester@example.com",
                roles = setOf("ADMIN")
            )
        )
    }

    @AfterEach
    fun tearDown() {
        TenantContext.clear()
    }

    @Test
    fun `listUnits returns mapped DTOs for tenant`() {
        val propertyId = UUID.randomUUID()

        val unit = Unit(
            tenantId = tenantId,
            propertyId = propertyId,
            name = "Suite 101",
            floor = "1",
            status = UnitStatus.OCCUPIED,
            bedrooms = 2,
            bathrooms = 1,
            areaSqFt = BigDecimal("850.0"),
            monthlyRent = BigDecimal("1200.00"),
            currency = "USD",
            notes = "Corner unit"
        )
        setBaseEntityFields(unit, UUID.randomUUID())

        val pageableCaptor = argumentCaptor<Pageable>()

        whenever(
            unitRepository.searchPage(
                eq(tenantId),
                eq(propertyId),
                eq(UnitStatus.OCCUPIED),
                eq("suite"),
                pageableCaptor.capture()
            )
        ).thenReturn(pageOf(unit))

        val result: List<UnitDto> = service.listUnits(
            propertyId = propertyId,
            status = UnitStatus.OCCUPIED,
            search = "suite",
            limit = 50
        )

        assertEquals(1, result.size)
        val dto = result[0]
        assertEquals(unit.id, dto.id)
        assertEquals(tenantId, dto.tenantId)
        assertEquals(propertyId, dto.propertyId)
        assertEquals("Suite 101", dto.name)
        assertEquals(UnitStatus.OCCUPIED, dto.status)

        // optional sanity check
        assertEquals(0, pageableCaptor.firstValue.pageNumber)
        assertEquals(50, pageableCaptor.firstValue.pageSize)

    }


}
