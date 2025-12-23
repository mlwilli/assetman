package com.github.mlwilli.assetman.property.service

import com.github.mlwilli.assetman.common.error.NotFoundException
import com.github.mlwilli.assetman.common.security.TenantContext
import com.github.mlwilli.assetman.common.security.currentTenantId
import com.github.mlwilli.assetman.common.web.PagingLimits
import com.github.mlwilli.assetman.common.web.firstPage
import org.springframework.data.domain.Sort
import com.github.mlwilli.assetman.property.domain.Unit
import com.github.mlwilli.assetman.property.domain.UnitStatus
import com.github.mlwilli.assetman.property.repo.PropertyRepository
import com.github.mlwilli.assetman.property.repo.UnitRepository
import com.github.mlwilli.assetman.property.web.CreateUnitRequest
import com.github.mlwilli.assetman.property.web.UnitDto
import com.github.mlwilli.assetman.property.web.UpdateUnitRequest
import com.github.mlwilli.assetman.property.web.toDto
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
    fun listUnits(propertyId: UUID?, status: UnitStatus?, search: String?, limit: Int): List<UnitDto> {
        val tenantId = currentTenantId()

        val pageable = firstPage(
            limit = limit,
            defaultLimit = PagingLimits.DEFAULT_LIST_LIMIT,
            maxLimit = PagingLimits.MAX_LIST_LIMIT,
            sort = Sort.by("name").ascending()
        )

        return unitRepository.searchPage(tenantId, propertyId, status, search, pageable)
            .content
            .map { it.toDto() }
    }


    @Transactional(readOnly = true)
    fun getUnit(id: UUID): UnitDto {
        val tenantId = currentTenantId()
        val unit = unitRepository.findByIdAndTenantId(id, tenantId)
            ?: throw NotFoundException("Unit not found")
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
            availableFrom = request.availableFrom,
            availableTo = request.availableTo,
            maxOccupancy = request.maxOccupancy,
            furnished = request.furnished,
            externalRef = request.externalRef,
            notes = request.notes,
            customFieldsJson = request.customFieldsJson
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
        unit.availableFrom = request.availableFrom
        unit.availableTo = request.availableTo
        unit.maxOccupancy = request.maxOccupancy
        unit.furnished = request.furnished
        unit.externalRef = request.externalRef
        unit.notes = request.notes
        unit.customFieldsJson = request.customFieldsJson

        val saved = unitRepository.save(unit)
        return saved.toDto()
    }

    @Transactional
    fun deleteUnit(id: UUID) {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")

        val existing = unitRepository.findByIdAndTenantId(id, ctx.tenantId)
            ?: return

        // later: enforce no active leases, etc.
        unitRepository.delete(existing)
    }
}
