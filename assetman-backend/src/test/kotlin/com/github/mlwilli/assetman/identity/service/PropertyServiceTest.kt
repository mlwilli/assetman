package com.github.mlwilli.assetman.property.service

import com.github.mlwilli.assetman.common.error.NotFoundException
import com.github.mlwilli.assetman.common.security.AuthenticatedUser
import com.github.mlwilli.assetman.common.security.TenantContext
import com.github.mlwilli.assetman.property.domain.Property
import com.github.mlwilli.assetman.property.domain.PropertyType
import com.github.mlwilli.assetman.property.repo.PropertyRepository
import com.github.mlwilli.assetman.property.repo.UnitRepository
import com.github.mlwilli.assetman.property.web.CreatePropertyRequest
import com.github.mlwilli.assetman.property.web.UpdatePropertyRequest
import com.github.mlwilli.assetman.testsupport.pageOf
import com.github.mlwilli.assetman.testsupport.setBaseEntityFields
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class PropertyServiceTest {

    private lateinit var propertyRepository: PropertyRepository
    private lateinit var unitRepository: UnitRepository
    private lateinit var service: PropertyService

    private val tenantId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        propertyRepository = mock()
        unitRepository = mock()
        service = PropertyService(propertyRepository, unitRepository)

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
    // listProperties
    // ----------------------------------------------------------------------

    @Test
    fun `listProperties returns mapped DTOs`() {
        val property = Property(
            tenantId = tenantId,
            name = "HQ",
            type = PropertyType.COMMERCIAL,
            code = "HQ-001",
            locationId = null
        )
        setBaseEntityFields(property, UUID.randomUUID())

        // IMPORTANT: stub searchPage (the method PropertyService actually calls)
        Mockito.`when`(
            propertyRepository.searchPage(
                eq(tenantId),
                eq(PropertyType.COMMERCIAL),
                eq("hq"),
                any<Pageable>()
            )
        ).thenReturn(pageOf(property))

        val result = service.listProperties(
            type = PropertyType.COMMERCIAL,
            search = "hq",
            limit = 20
        )

        assertEquals(1, result.size)
        assertEquals("HQ", result[0].name)
        assertEquals(PropertyType.COMMERCIAL, result[0].type)
        assertEquals("HQ-001", result[0].code)

        // Capture pageable on verify (reliable)
        val pageableCaptor = argumentCaptor<Pageable>()
        verify(propertyRepository).searchPage(
            eq(tenantId),
            eq(PropertyType.COMMERCIAL),
            eq("hq"),
            pageableCaptor.capture()
        )

        val pageable = pageableCaptor.firstValue as PageRequest
        assertEquals(0, pageable.pageNumber)
        assertEquals(20, pageable.pageSize)
    }

    // ----------------------------------------------------------------------
    // getProperty
    // ----------------------------------------------------------------------

    @Test
    fun `getProperty returns DTO for existing tenant property`() {
        val id = UUID.randomUUID()
        val property = Property(
            tenantId = tenantId,
            name = "Warehouse",
            type = PropertyType.INDUSTRIAL
        )
        setBaseEntityFields(property, id)

        whenever(propertyRepository.findByIdAndTenantId(eq(id), eq(tenantId)))
            .thenReturn(property)

        val dto = service.getProperty(id)

        assertEquals(id, dto.id)
        assertEquals("Warehouse", dto.name)
        assertEquals(PropertyType.INDUSTRIAL, dto.type)
    }

    @Test
    fun `getProperty throws NotFoundException when missing`() {
        val id = UUID.randomUUID()

        whenever(propertyRepository.findByIdAndTenantId(eq(id), eq(tenantId)))
            .thenReturn(null)

        assertThrows<NotFoundException> { service.getProperty(id) }
    }

    // ----------------------------------------------------------------------
    // createProperty
    // ----------------------------------------------------------------------

    @Test
    fun `createProperty saves and returns DTO`() {
        val request = CreatePropertyRequest(
            name = "East Office",
            type = PropertyType.COMMERCIAL,
            code = "E-001",
            locationId = UUID.randomUUID(),
            addressLine1 = "123 Main St",
            city = "Boise",
            state = "ID",
            postalCode = "83702",
            notes = "Main location"
        )

        val saved = Property(
            tenantId = tenantId,
            name = request.name,
            type = request.type,
            code = request.code,
            locationId = request.locationId,
            addressLine1 = request.addressLine1,
            city = request.city,
            state = request.state,
            postalCode = request.postalCode,
            notes = request.notes
        )
        setBaseEntityFields(saved, UUID.randomUUID())

        whenever(propertyRepository.save(any())).thenReturn(saved)

        val dto = service.createProperty(request)

        val captor = argumentCaptor<Property>()
        verify(propertyRepository).save(captor.capture())
        val persisted = captor.firstValue

        assertEquals(request.name, persisted.name)
        assertEquals(request.type, persisted.type)
        assertEquals(request.code, persisted.code)

        assertEquals(saved.id, dto.id)
        assertEquals("East Office", dto.name)
    }

    // ----------------------------------------------------------------------
    // updateProperty
    // ----------------------------------------------------------------------

    @Test
    fun `updateProperty updates entity and returns DTO`() {
        val id = UUID.randomUUID()

        val existing = Property(
            tenantId = tenantId,
            name = "Old Name",
            type = PropertyType.COMMERCIAL
        )
        setBaseEntityFields(existing, id)

        whenever(propertyRepository.findByIdAndTenantId(eq(id), eq(tenantId)))
            .thenReturn(existing)

        whenever(propertyRepository.save(eq(existing)))
            .thenReturn(existing)

        val request = UpdatePropertyRequest(
            name = "Updated Name",
            type = PropertyType.RESIDENTIAL,
            code = "NEW-CODE",
            locationId = UUID.randomUUID(),
            addressLine1 = "456 Updated Rd",
            city = "Nampa",
            state = "ID",
            postalCode = "83686",
            notes = "Updated"
        )

        val dto = service.updateProperty(id, request)

        assertEquals("Updated Name", existing.name)
        assertEquals(PropertyType.RESIDENTIAL, existing.type)
        assertEquals("Updated Name", dto.name)
    }

    @Test
    fun `updateProperty throws EntityNotFoundException when missing`() {
        val id = UUID.randomUUID()

        whenever(propertyRepository.findByIdAndTenantId(eq(id), eq(tenantId)))
            .thenReturn(null)

        assertThrows(EntityNotFoundException::class.java) {
            service.updateProperty(
                id,
                UpdatePropertyRequest(
                    name = "X",
                    type = PropertyType.OTHER
                )
            )
        }
    }

    // ----------------------------------------------------------------------
    // deleteProperty
    // ----------------------------------------------------------------------

    @Test
    fun `deleteProperty deletes when found and no units exist`() {
        val id = UUID.randomUUID()
        val property = Property(
            tenantId = tenantId,
            name = "To Delete",
            type = PropertyType.LAND
        )
        setBaseEntityFields(property, id)

        whenever(propertyRepository.findByIdAndTenantId(eq(id), eq(tenantId)))
            .thenReturn(property)

        whenever(unitRepository.existsByTenantIdAndPropertyId(eq(tenantId), eq(id)))
            .thenReturn(false)

        service.deleteProperty(id)

        verify(propertyRepository).delete(eq(property))
    }

    @Test
    fun `deleteProperty is no-op when not found`() {
        val id = UUID.randomUUID()

        whenever(propertyRepository.findByIdAndTenantId(eq(id), eq(tenantId)))
            .thenReturn(null)

        service.deleteProperty(id)

        verify(propertyRepository, never()).delete(any())
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------




}
