package com.github.mlwilli.assetman.property.service

import com.github.mlwilli.assetman.common.error.ConflictException
import com.github.mlwilli.assetman.common.error.NotFoundException
import com.github.mlwilli.assetman.common.security.TenantContext
import com.github.mlwilli.assetman.common.security.currentTenantId
import com.github.mlwilli.assetman.property.domain.Property
import com.github.mlwilli.assetman.property.domain.PropertyType
import com.github.mlwilli.assetman.property.repo.PropertyRepository
import com.github.mlwilli.assetman.property.repo.UnitRepository
import com.github.mlwilli.assetman.property.web.CreatePropertyRequest
import com.github.mlwilli.assetman.property.web.PropertyDto
import com.github.mlwilli.assetman.property.web.UpdatePropertyRequest
import com.github.mlwilli.assetman.property.web.toDto
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PropertyService(
    private val propertyRepository: PropertyRepository,
    private val unitRepository: UnitRepository
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
        val tenantId = currentTenantId()
        val property = propertyRepository.findByIdAndTenantId(id, tenantId)
            ?: throw NotFoundException("Property not found")
        return property.toDto()
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
            notes = request.notes,
            active = request.active,
            yearBuilt = request.yearBuilt,
            totalUnits = request.totalUnits,
            externalRef = request.externalRef,
            customFieldsJson = request.customFieldsJson
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
        existing.active = request.active
        existing.yearBuilt = request.yearBuilt
        existing.totalUnits = request.totalUnits
        existing.externalRef = request.externalRef
        existing.customFieldsJson = request.customFieldsJson

        val saved = propertyRepository.save(existing)
        return saved.toDto()
    }

    @Transactional
    fun deleteProperty(id: UUID) {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")

        val existing = propertyRepository.findByIdAndTenantId(id, ctx.tenantId)
            ?: return

        if (unitRepository.existsByTenantIdAndPropertyId(ctx.tenantId, existing.id)) {
            throw ConflictException("Cannot delete property with existing units")
        }

        propertyRepository.delete(existing)
    }
}
