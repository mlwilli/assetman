package com.github.mlwilli.assetman.property.service

import com.github.mlwilli.assetman.property.domain.Property
import com.github.mlwilli.assetman.property.domain.PropertyType
import com.github.mlwilli.assetman.property.repo.PropertyRepository
import com.github.mlwilli.assetman.property.web.CreatePropertyRequest
import com.github.mlwilli.assetman.property.web.PropertyDto
import com.github.mlwilli.assetman.property.web.UpdatePropertyRequest
import com.github.mlwilli.assetman.common.security.TenantContext
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PropertyService(
    private val propertyRepository: PropertyRepository
) {

    @Transactional(readOnly = true)
    fun listProperties(
        type: PropertyType?,
        search: String?
    ): List<PropertyDto> {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")
        return propertyRepository.search(
            tenantId = ctx.tenantId,
            type = type,
            search = search
        ).map { it.toDto() }
    }

    @Transactional(readOnly = true)
    fun getProperty(id: UUID): PropertyDto {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")
        val prop = propertyRepository.findByIdAndTenantId(id, ctx.tenantId)
            ?: throw EntityNotFoundException("Property not found")
        return prop.toDto()
    }

    @Transactional
    fun createProperty(request: CreatePropertyRequest): PropertyDto {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")
        val entity = Property(
            tenantId = ctx.tenantId,
            name = request.name,
            type = request.type,
            code = request.code,
            locationId = request.locationId,
            addressLine1 = request.addressLine1,
            addressLine2 = request.addressLine2,
            city = request.city,
            state = request.state,
            postalCode = request.postalCode,
            country = request.country,
            notes = request.notes
        )
        val saved = propertyRepository.save(entity)
        return saved.toDto()
    }

    @Transactional
    fun updateProperty(id: UUID, request: UpdatePropertyRequest): PropertyDto {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")
        val existing = propertyRepository.findByIdAndTenantId(id, ctx.tenantId)
            ?: throw EntityNotFoundException("Property not found")

        existing.name = request.name
        existing.type = request.type
        existing.code = request.code
        existing.locationId = request.locationId
        existing.addressLine1 = request.addressLine1
        existing.addressLine2 = request.addressLine2
        existing.city = request.city
        existing.state = request.state
        existing.postalCode = request.postalCode
        existing.country = request.country
        existing.notes = request.notes

        val saved = propertyRepository.save(existing)
        return saved.toDto()
    }

    @Transactional
    fun deleteProperty(id: UUID) {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")
        val existing = propertyRepository.findByIdAndTenantId(id, ctx.tenantId)
            ?: return
        // TODO: enforce that no units/leases exist before deleting (later)
        propertyRepository.delete(existing)
    }

    private fun Property.toDto(): PropertyDto =
        PropertyDto(
            id = id,
            tenantId = tenantId,
            name = name,
            type = type,
            code = code,
            locationId = locationId,
            addressLine1 = addressLine1,
            addressLine2 = addressLine2,
            city = city,
            state = state,
            postalCode = postalCode,
            country = country,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}
