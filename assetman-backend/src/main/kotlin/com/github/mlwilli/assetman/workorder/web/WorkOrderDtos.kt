package com.github.mlwilli.assetman.workorder.web

import com.github.mlwilli.assetman.workorder.domain.WorkOrder
import com.github.mlwilli.assetman.workorder.domain.WorkOrderPriority
import com.github.mlwilli.assetman.workorder.domain.WorkOrderStatus
import com.github.mlwilli.assetman.workorder.domain.WorkOrderType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class WorkOrderDto(
    val id: UUID,
    val tenantId: UUID,
    val title: String,
    val description: String?,
    val status: WorkOrderStatus,
    val priority: WorkOrderPriority,
    val type: WorkOrderType,
    val assetId: UUID?,
    val propertyId: UUID?,
    val unitId: UUID?,
    val locationId: UUID?,
    val assignedToUserId: UUID?,
    val createdByUserId: UUID,
    val dueDate: LocalDate?,
    val scheduledStart: Instant?,
    val scheduledEnd: Instant?,
    val startedAt: Instant?,
    val completedAt: Instant?,
    val estimatedCost: BigDecimal?,
    val actualCost: BigDecimal?,
    val tags: String?,
    val externalTicketRef: String?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreateWorkOrderRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val title: String,

    @field:Size(max = 8000)
    val description: String? = null,

    val priority: WorkOrderPriority = WorkOrderPriority.MEDIUM,
    val type: WorkOrderType = WorkOrderType.CORRECTIVE,

    val assetId: UUID? = null,
    val propertyId: UUID? = null,
    val unitId: UUID? = null,
    val locationId: UUID? = null,
    val assignedToUserId: UUID? = null,

    val dueDate: LocalDate? = null,
    val scheduledStart: Instant? = null,
    val scheduledEnd: Instant? = null,

    val estimatedCost: BigDecimal? = null,

    @field:Size(max = 1024)
    val tags: String? = null,

    @field:Size(max = 255)
    val externalTicketRef: String? = null,

    @field:Size(max = 4000)
    val notes: String? = null
)

/**
 * Update command for non-lifecycle fields.
 *
 * Status and lifecycle timestamps (startedAt/completedAt) are controlled by
 * the dedicated status transition endpoint (updateStatus), not here.
 */
data class UpdateWorkOrderRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val title: String,

    @field:Size(max = 8000)
    val description: String? = null,

    val priority: WorkOrderPriority,
    val type: WorkOrderType,

    val assetId: UUID? = null,
    val propertyId: UUID? = null,
    val unitId: UUID? = null,
    val locationId: UUID? = null,
    val assignedToUserId: UUID? = null,

    val dueDate: LocalDate? = null,
    val scheduledStart: Instant? = null,
    val scheduledEnd: Instant? = null,

    val estimatedCost: BigDecimal? = null,
    val actualCost: BigDecimal? = null,

    @field:Size(max = 1024)
    val tags: String? = null,

    @field:Size(max = 255)
    val externalTicketRef: String? = null,

    @field:Size(max = 4000)
    val notes: String? = null
)

data class UpdateWorkOrderStatusRequest(
    val status: WorkOrderStatus
)

/**
 * Canonical entity → DTO mapper for WorkOrder.
 */
fun WorkOrder.toDto(): WorkOrderDto =
    WorkOrderDto(
        id = this.id,
        tenantId = this.tenantId,
        title = this.title,
        description = this.description,
        status = this.status,
        priority = this.priority,
        type = this.type,
        assetId = this.assetId,
        propertyId = this.propertyId,
        unitId = this.unitId,
        locationId = this.locationId,
        assignedToUserId = this.assignedToUserId,
        createdByUserId = this.createdByUserId,
        dueDate = this.dueDate,
        scheduledStart = this.scheduledStart,
        scheduledEnd = this.scheduledEnd,
        startedAt = this.startedAt,
        completedAt = this.completedAt,
        estimatedCost = this.estimatedCost,
        actualCost = this.actualCost,
        tags = this.tags,
        externalTicketRef = this.externalTicketRef,
        notes = this.notes,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
