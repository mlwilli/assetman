package com.github.mlwilli.assetman.property.service

import com.github.mlwilli.assetman.common.error.NotFoundException
import com.github.mlwilli.assetman.common.security.AuthenticatedUser
import com.github.mlwilli.assetman.common.security.TenantContext
import com.github.mlwilli.assetman.property.domain.Property
import com.github.mlwilli.assetman.property.domain.PropertyType
import com.github.mlwilli.assetman.property.domain.Unit
import com.github.mlwilli.assetman.property.domain.UnitStatus
import com.github.mlwilli.assetman.property.repo.PropertyRepository
import com.github.mlwilli.assetman.property.repo.UnitRepository
import com.github.mlwilli.assetman.property.web.CreateUnitRequest
import com.github.mlwilli.assetman.property.web.UpdateUnitRequest
import com.github.mlwilli.assetman.property.web.UnitDto
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class UnitServiceTest {

    private lateinit var unitRepository: UnitRepository
    private lateinit var propertyRepository: PropertyRepository
    private lateinit var service: UnitService

    private val tenantId: UUID = UUID.randomUUID()
    private val userId: UUID = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        unitRepository = Mockito.mock(UnitRepository::class.java)
        propertyRepository = Mockito.mock(PropertyRepository::class.java)
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

    // ----------------------------------------------------------------------
    // listUnits
    // ----------------------------------------------------------------------

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
        setEntityBaseFields(unit, UUID.randomUUID())

        Mockito.`when`(
            unitRepository.search(
                tenantId = tenantId,
                propertyId = propertyId,
                status = UnitStatus.OCCUPIED,
                search = "suite"
            )
        ).thenReturn(listOf(unit))

        val result: List<UnitDto> = service.listUnits(
            propertyId = propertyId,
            status = UnitStatus.OCCUPIED,
            search = "suite"
        )

        assertEquals(1, result.size)
        val dto = result[0]
        assertEquals(unit.id, dto.id)
        assertEquals(tenantId, dto.tenantId)
        assertEquals(propertyId, dto.propertyId)
        assertEquals("Suite 101", dto.name)
        assertEquals(UnitStatus.OCCUPIED, dto.status)
    }

    // ----------------------------------------------------------------------
    // getUnit
    // ----------------------------------------------------------------------

    @Test
    fun `getUnit returns DTO when entity found for tenant`() {
        val id = UUID.randomUUID()
        val propertyId = UUID.randomUUID()

        val unit = Unit(
            tenantId = tenantId,
            propertyId = propertyId,
            name = "Unit A",
            floor = "2",
            status = UnitStatus.VACANT,
            bedrooms = 1,
            bathrooms = 1,
            areaSqFt = BigDecimal("600.0"),
            monthlyRent = BigDecimal("900.00"),
            currency = "USD",
            notes = "Test unit"
        )
        setEntityBaseFields(unit, id)

        Mockito.`when`(
            unitRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(unit)

        val dto = service.getUnit(id)

        assertEquals(id, dto.id)
        assertEquals(tenantId, dto.tenantId)
        assertEquals(propertyId, dto.propertyId)
        assertEquals("Unit A", dto.name)
        assertEquals(UnitStatus.VACANT, dto.status)
    }

    @Test
    fun `getUnit throws NotFoundException when unit does not exist in tenant`() {
        val id = UUID.randomUUID()

        Mockito.`when`(
            unitRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            service.getUnit(id)
        }
    }

    // ----------------------------------------------------------------------
    // createUnit
    // ----------------------------------------------------------------------

    @Test
    fun `createUnit validates property tenant and saves unit`() {
        val propertyId = UUID.randomUUID()

        val property = Property(
            tenantId = tenantId,
            name = "Test Property",
            type = PropertyType.RESIDENTIAL
        )
        setEntityBaseFields(property, propertyId)

        Mockito.`when`(
            propertyRepository.findByIdAndTenantId(propertyId, tenantId)
        ).thenReturn(property)

        val request = CreateUnitRequest(
            propertyId = propertyId,
            name = "Suite 202",
            floor = "2",
            status = UnitStatus.RESERVED,
            bedrooms = 3,
            bathrooms = 2,
            areaSqFt = BigDecimal("1100.00"),
            monthlyRent = BigDecimal("1800.00"),
            currency = "USD",
            notes = "Nice view"
        )

        val saved = Unit(
            tenantId = tenantId,
            propertyId = propertyId,
            name = request.name,
            floor = request.floor,
            status = request.status,
            bedrooms = request.bedrooms,
            bathrooms = request.bathrooms,
            areaSqFt = request.areaSqFt,
            monthlyRent = request.monthlyRent,
            currency = request.currency,
            notes = request.notes
        )
        setEntityBaseFields(saved, UUID.randomUUID())

        Mockito.`when`(
            unitRepository.save(any(Unit::class.java))
        ).thenReturn(saved)

        val dto = service.createUnit(request)

        val captor = ArgumentCaptor.forClass(Unit::class.java)
        Mockito.verify(unitRepository).save(captor.capture())
        val persisted = captor.value

        // Check what we persisted
        assertEquals(tenantId, persisted.tenantId)
        assertEquals(propertyId, persisted.propertyId)
        assertEquals("Suite 202", persisted.name)
        assertEquals(UnitStatus.RESERVED, persisted.status)

        // Check DTO
        assertEquals(saved.id, dto.id)
        assertEquals("Suite 202", dto.name)
    }

    @Test
    fun `createUnit throws EntityNotFoundException when property not in tenant`() {
        val propertyId = UUID.randomUUID()

        Mockito.`when`(
            propertyRepository.findByIdAndTenantId(propertyId, tenantId)
        ).thenReturn(null)

        val request = CreateUnitRequest(
            propertyId = propertyId,
            name = "Suite 999",
            status = UnitStatus.VACANT
        )

        assertThrows(EntityNotFoundException::class.java) {
            service.createUnit(request)
        }
    }

    // ----------------------------------------------------------------------
    // updateUnit
    // ----------------------------------------------------------------------

    @Test
    fun `updateUnit updates existing unit and returns DTO`() {
        val id = UUID.randomUUID()
        val propertyId = UUID.randomUUID()

        val existing = Unit(
            tenantId = tenantId,
            propertyId = propertyId,
            name = "Old Name",
            floor = "1",
            status = UnitStatus.VACANT,
            bedrooms = 1,
            bathrooms = 1,
            areaSqFt = BigDecimal("500.00"),
            monthlyRent = BigDecimal("700.00"),
            currency = "USD",
            notes = "Old notes"
        )
        setEntityBaseFields(existing, id)

        Mockito.`when`(
            unitRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(existing)

        val request = UpdateUnitRequest(
            name = "Updated Name",
            floor = "3",
            status = UnitStatus.OCCUPIED,
            bedrooms = 2,
            bathrooms = 2,
            areaSqFt = BigDecimal("950.00"),
            monthlyRent = BigDecimal("1500.00"),
            currency = "USD",
            notes = "Updated notes"
        )

        Mockito.`when`(
            unitRepository.save(existing)
        ).thenReturn(existing)

        val dto = service.updateUnit(id, request)

        // Verify entity mutated
        assertEquals("Updated Name", existing.name)
        assertEquals("3", existing.floor)
        assertEquals(UnitStatus.OCCUPIED, existing.status)
        assertEquals(BigDecimal("950.00"), existing.areaSqFt)

        // Verify DTO
        assertEquals("Updated Name", dto.name)
        assertEquals(UnitStatus.OCCUPIED, dto.status)
    }

    @Test
    fun `updateUnit throws EntityNotFoundException when unit missing`() {
        val id = UUID.randomUUID()

        Mockito.`when`(
            unitRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(null)

        val request = UpdateUnitRequest(
            name = "Does Not Matter",
            floor = null,
            status = UnitStatus.VACANT,
            bedrooms = null,
            bathrooms = null,
            areaSqFt = null,
            monthlyRent = null,
            currency = null,
            notes = null
        )

        assertThrows(EntityNotFoundException::class.java) {
            service.updateUnit(id, request)
        }
    }

    // ----------------------------------------------------------------------
    // deleteUnit
    // ----------------------------------------------------------------------

    @Test
    fun `deleteUnit deletes when found`() {
        val id = UUID.randomUUID()
        val propertyId = UUID.randomUUID()

        val unit = Unit(
            tenantId = tenantId,
            propertyId = propertyId,
            name = "To Delete",
            floor = "1",
            status = UnitStatus.VACANT
        )
        setEntityBaseFields(unit, id)

        Mockito.`when`(
            unitRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(unit)

        service.deleteUnit(id)

        Mockito.verify(unitRepository).delete(unit)
    }

    @Test
    fun `deleteUnit is no-op when unit not found`() {
        val id = UUID.randomUUID()

        Mockito.`when`(
            unitRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(null)

        service.deleteUnit(id)

        Mockito.verify(unitRepository, Mockito.never()).delete(any(Unit::class.java))
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    private fun setEntityBaseFields(entity: Any, id: UUID) {
        val superclass = entity.javaClass.superclass  // BaseEntity
        val idField = superclass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)

        val createdAtField = superclass.getDeclaredField("createdAt")
        createdAtField.isAccessible = true
        createdAtField.set(entity, Instant.now())

        val updatedAtField = superclass.getDeclaredField("updatedAt")
        updatedAtField.isAccessible = true
        updatedAtField.set(entity, Instant.now())
    }
}
