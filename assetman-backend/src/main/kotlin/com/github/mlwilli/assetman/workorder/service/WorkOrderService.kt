package com.github.mlwilli.assetman.workorder.service

import com.github.mlwilli.assetman.common.error.NotFoundException
import com.github.mlwilli.assetman.common.security.TenantContext
import com.github.mlwilli.assetman.common.security.currentTenantId
import com.github.mlwilli.assetman.workorder.domain.WorkOrder
import com.github.mlwilli.assetman.workorder.domain.WorkOrderPriority
import com.github.mlwilli.assetman.workorder.domain.WorkOrderStatus
import com.github.mlwilli.assetman.workorder.domain.WorkOrderType
import com.github.mlwilli.assetman.workorder.repo.WorkOrderRepository
import com.github.mlwilli.assetman.workorder.web.CreateWorkOrderRequest
import com.github.mlwilli.assetman.workorder.web.UpdateWorkOrderRequest
import com.github.mlwilli.assetman.workorder.web.UpdateWorkOrderStatusRequest
import com.github.mlwilli.assetman.workorder.web.WorkOrderDto
import com.github.mlwilli.assetman.workorder.web.toDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class WorkOrderService(
    private val workOrderRepository: WorkOrderRepository
) {

    @Transactional(readOnly = true)
    fun listWorkOrders(
        status: WorkOrderStatus?,
        priority: WorkOrderPriority?,
        type: WorkOrderType?,
        assetId: UUID?,
        propertyId: UUID?,
        unitId: UUID?,
        assignedToUserId: UUID?,
        search: String?
    ): List<WorkOrderDto> {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")

        return workOrderRepository.search(
            tenantId = ctx.tenantId,
            status = status,
            priority = priority,
            type = type,
            assetId = assetId,
            propertyId = propertyId,
            unitId = unitId,
            assignedToUserId = assignedToUserId,
            search = search
        ).map { it.toDto() }
    }

    @Transactional(readOnly = true)
    fun getWorkOrder(id: UUID): WorkOrderDto {
        val tenantId = currentTenantId()

        val workOrder = workOrderRepository.findByIdAndTenantId(id, tenantId)
            ?: throw NotFoundException("Work order not found")

        return workOrder.toDto()
    }

    @Transactional
    fun createWorkOrder(request: CreateWorkOrderRequest): WorkOrderDto {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")

        val entity = WorkOrder(
            tenantId = ctx.tenantId,
            title = request.title,
            description = request.description,
            // status defaults to OPEN
            priority = request.priority,
            type = request.type,
            assetId = request.assetId,
            propertyId = request.propertyId,
            unitId = request.unitId,
            locationId = request.locationId,
            assignedToUserId = request.assignedToUserId,
            createdByUserId = ctx.userId,
            dueDate = request.dueDate,
            scheduledStart = request.scheduledStart,
            scheduledEnd = request.scheduledEnd,
            estimatedCost = request.estimatedCost,
            actualCost = null,
            tags = request.tags,
            externalTicketRef = request.externalTicketRef,
            notes = request.notes
        )

        val saved = workOrderRepository.save(entity)
        return saved.toDto()
    }

    @Transactional
    fun updateWorkOrder(id: UUID, request: UpdateWorkOrderRequest): WorkOrderDto {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")

        val wo = workOrderRepository.findByIdAndTenantId(id, ctx.tenantId)
            ?: throw NotFoundException("Work order not found")

        wo.title = request.title
        wo.description = request.description

        wo.priority = request.priority
        wo.type = request.type

        wo.assetId = request.assetId
        wo.propertyId = request.propertyId
        wo.unitId = request.unitId
        wo.locationId = request.locationId
        wo.assignedToUserId = request.assignedToUserId

        wo.dueDate = request.dueDate
        wo.scheduledStart = request.scheduledStart
        wo.scheduledEnd = request.scheduledEnd

        wo.estimatedCost = request.estimatedCost
        wo.actualCost = request.actualCost

        wo.tags = request.tags
        wo.externalTicketRef = request.externalTicketRef
        wo.notes = request.notes

        val saved = workOrderRepository.save(wo)
        return saved.toDto()
    }

    @Transactional
    fun updateStatus(id: UUID, request: UpdateWorkOrderStatusRequest): WorkOrderDto {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")

        val wo = workOrderRepository.findByIdAndTenantId(id, ctx.tenantId)
            ?: throw NotFoundException("Work order not found")

        when (request.status) {
            WorkOrderStatus.IN_PROGRESS -> wo.start(Instant.now())
            WorkOrderStatus.COMPLETED -> wo.complete(Instant.now())
            WorkOrderStatus.ON_HOLD -> wo.hold()
            WorkOrderStatus.CANCELLED -> wo.cancel()
            WorkOrderStatus.OPEN -> wo.status = WorkOrderStatus.OPEN
        }

        val saved = workOrderRepository.save(wo)
        return saved.toDto()
    }

    @Transactional
    fun deleteWorkOrder(id: UUID) {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")

        val wo = workOrderRepository.findByIdAndTenantId(id, ctx.tenantId) ?: return
        workOrderRepository.delete(wo)
    }

    /**
     * SLA / Overdue support:
     *
     * Returns all work orders for the current tenant that are "overdue"
     * according to the domain rule in WorkOrder.isOverdue(today).
     */
    @Transactional(readOnly = true)
    fun listOverdueWorkOrders(today: LocalDate = LocalDate.now()): List<WorkOrderDto> {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")

        val candidates: List<WorkOrder> = workOrderRepository.findDueBefore(
            tenantId = ctx.tenantId,
            today = today
        )

        return candidates
            .filter { it.isOverdue(today) }
            .map { it.toDto() }
    }
}
