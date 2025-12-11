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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Assertions.assertThrows

import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.util.UUID
import kotlin.jvm.java

class PropertyServiceTest {

    private lateinit var propertyRepository: PropertyRepository
    private lateinit var unitRepository: UnitRepository
    private lateinit var service: PropertyService

    private val tenantId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        propertyRepository = Mockito.mock(PropertyRepository::class.java)
        unitRepository = Mockito.mock(UnitRepository::class.java)

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

        setEntityId(property, UUID.randomUUID())

        Mockito.`when`(
            propertyRepository.search(
                tenantId = tenantId,
                type = PropertyType.COMMERCIAL,
                search = "hq"
            )
        ).thenReturn(listOf(property))

        val result = service.listProperties(
            type = PropertyType.COMMERCIAL,
            search = "hq"
        )

        assertEquals(1, result.size)
        assertEquals("HQ", result[0].name)
        assertEquals(PropertyType.COMMERCIAL, result[0].type)
        assertEquals("HQ-001", result[0].code)
    }

    // ----------------------------------------------------------------------
    // getProperty
    // ----------------------------------------------------------------------

    @Test
    fun `getProperty returns DTO for existing tenant-scoped property`() {
        val id = UUID.randomUUID()
        val property = Property(
            tenantId = tenantId,
            name = "Warehouse",
            type = PropertyType.INDUSTRIAL
        )

        setEntityId(property, id)

        Mockito.`when`(
            propertyRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(property)

        val dto = service.getProperty(id)

        assertEquals(id, dto.id)
        assertEquals("Warehouse", dto.name)
        assertEquals(PropertyType.INDUSTRIAL, dto.type)
    }

    @Test
    fun `getProperty throws NotFoundException for missing property`() {
        val id = UUID.randomUUID()

        Mockito.`when`(
            propertyRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(null)

        // ✅ use Kotlin JUnit extension generic form
        assertThrows<NotFoundException> {
            service.getProperty(id)
        }
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

        setEntityId(saved, UUID.randomUUID())

        Mockito.`when`(
            propertyRepository.save(Mockito.any(Property::class.java))
        ).thenReturn(saved)

        val dto = service.createProperty(request)

        val captor = ArgumentCaptor.forClass(Property::class.java)
        Mockito.verify(propertyRepository).save(captor.capture())
        val persisted = captor.value

        // Validate fields passed to repo
        assertEquals(request.name, persisted.name)
        assertEquals(request.type, persisted.type)
        assertEquals(request.code, persisted.code)
        assertEquals(request.locationId, persisted.locationId)
        assertEquals("123 Main St", persisted.addressLine1)

        // Validate DTO output
        assertEquals(saved.id, dto.id)
        assertEquals("East Office", dto.name)
    }

    // ----------------------------------------------------------------------
    // updateProperty
    // ----------------------------------------------------------------------

    @Test
    fun `updateProperty updates existing entity and returns DTO`() {
        val id = UUID.randomUUID()

        val existing = Property(
            tenantId = tenantId,
            name = "Old Name",
            type = PropertyType.COMMERCIAL
        )
        setEntityId(existing, id)

        Mockito.`when`(
            propertyRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(existing)

        val request = UpdatePropertyRequest(
            name = "Updated Name",
            type = PropertyType.RESIDENTIAL,
            code = "NEW-CODE",
            locationId = UUID.randomUUID(),
            addressLine1 = "456 Updated Rd",
            city = "Nampa",
            state = "ID",
            postalCode = "83686",
            notes = "Updated property"
        )

        Mockito.`when`(
            propertyRepository.save(existing)
        ).thenReturn(existing)

        val dto = service.updateProperty(id, request)

        assertEquals("Updated Name", existing.name)
        assertEquals(PropertyType.RESIDENTIAL, existing.type)
        assertEquals("NEW-CODE", existing.code)
        assertEquals("456 Updated Rd", existing.addressLine1)
        assertEquals("Updated Name", dto.name)
    }

    @Test
    fun `updateProperty throws EntityNotFoundException if property missing`() {
        val id = UUID.randomUUID()

        Mockito.`when`(
            propertyRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(null)

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
    fun `deleteProperty deletes entity when found`() {
        val id = UUID.randomUUID()

        val property = Property(
            tenantId = tenantId,
            name = "To Delete",
            type = PropertyType.LAND
        )
        setEntityId(property, id)

        Mockito.`when`(
            propertyRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(property)

        service.deleteProperty(id)

        Mockito.verify(propertyRepository).delete(property)
    }

    @Test
    fun `deleteProperty is silent when entity does not exist`() {
        val id = UUID.randomUUID()

        Mockito.`when`(
            propertyRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(null)

        service.deleteProperty(id)

        // ✅ give Mockito a concrete type for any()
        Mockito.verify(propertyRepository, Mockito.never())
            .delete(Mockito.any(Property::class.java))
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    private fun setEntityId(entity: Any, id: UUID) {
        val field = entity.javaClass.superclass.getDeclaredField("id")
        field.isAccessible = true
        field.set(entity, id)
    }
}
