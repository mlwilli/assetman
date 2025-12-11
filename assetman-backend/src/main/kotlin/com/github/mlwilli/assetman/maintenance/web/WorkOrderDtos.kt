package com.github.mlwilli.assetman.maintenance.web

import com.github.mlwilli.assetman.maintenance.domain.WorkOrderPriority
import com.github.mlwilli.assetman.maintenance.domain.WorkOrderStatus
import com.github.mlwilli.assetman.maintenance.domain.WorkOrderType
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
    val title: String,
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
    val tags: String? = null,
    val externalTicketRef: String? = null,
    val notes: String? = null
)

data class UpdateWorkOrderRequest(
    val title: String,
    val description: String? = null,
    val status: WorkOrderStatus,
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
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val estimatedCost: BigDecimal? = null,
    val actualCost: BigDecimal? = null,
    val tags: String? = null,
    val externalTicketRef: String? = null,
    val notes: String? = null
)

data class UpdateWorkOrderStatusRequest(
    val status: WorkOrderStatus
)
