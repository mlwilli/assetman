package com.github.mlwilli.assetman.property.service

import com.github.mlwilli.assetman.property.domain.Unit
import com.github.mlwilli.assetman.property.domain.UnitStatus
import com.github.mlwilli.assetman.property.repo.PropertyRepository
import com.github.mlwilli.assetman.property.repo.UnitRepository
import com.github.mlwilli.assetman.property.web.CreateUnitRequest
import com.github.mlwilli.assetman.property.web.UnitDto
import com.github.mlwilli.assetman.property.web.UpdateUnitRequest
import com.github.mlwilli.assetman.shared.security.TenantContext
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UnitService(
    private val unitRepository: UnitRepository,
    private val propertyRepository: PropertyRepository
) {

    @Transactional(readOnly = true)
    fun listUnits(
        propertyId: UUID?,
        status: UnitStatus?,
        search: String?
    ): List<UnitDto> {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")
        return unitRepository.search(
            tenantId = ctx.tenantId,
            propertyId = propertyId,
            status = status,
            search = search
        ).map { it.toDto() }
    }

    @Transactional(readOnly = true)
    fun getUnit(id: UUID): UnitDto {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")
        val unit = unitRepository.findByIdAndTenantId(id, ctx.tenantId)
            ?: throw EntityNotFoundException("Unit not found")
        return unit.toDto()
    }

    @Transactional
    fun createUnit(request: CreateUnitRequest): UnitDto {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")

        // Ensure property belongs to the same tenant.
        val property = propertyRepository.findByIdAndTenantId(request.propertyId, ctx.tenantId)
            ?: throw EntityNotFoundException("Property not found")

        val entity = Unit(
            tenantId = ctx.tenantId,
            propertyId = property.id,
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
        val saved = unitRepository.save(entity)
        return saved.toDto()
    }

    @Transactional
    fun updateUnit(id: UUID, request: UpdateUnitRequest): UnitDto {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")
        val unit = unitRepository.findByIdAndTenantId(id, ctx.tenantId)
            ?: throw EntityNotFoundException("Unit not found")

        unit.name = request.name
        unit.floor = request.floor
        unit.status = request.status
        unit.bedrooms = request.bedrooms
        unit.bathrooms = request.bathrooms
        unit.areaSqFt = request.areaSqFt
        unit.monthlyRent = request.monthlyRent
        unit.currency = request.currency
        unit.notes = request.notes

        val saved = unitRepository.save(unit)
        return saved.toDto()
    }

    @Transactional
    fun deleteUnit(id: UUID) {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")
        val existing = unitRepository.findByIdAndTenantId(id, ctx.tenantId)
            ?: return
        unitRepository.delete(existing)
    }

    private fun Unit.toDto(): UnitDto =
        UnitDto(
            id = id,
            tenantId = tenantId,
            propertyId = propertyId,
            name = name,
            floor = floor,
            status = status,
            bedrooms = bedrooms,
            bathrooms = bathrooms,
            areaSqFt = areaSqFt,
            monthlyRent = monthlyRent,
            currency = currency,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}
