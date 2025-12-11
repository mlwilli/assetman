package com.github.mlwilli.assetman.maintenance.web

import com.github.mlwilli.assetman.maintenance.domain.WorkOrderPriority
import com.github.mlwilli.assetman.maintenance.domain.WorkOrderStatus
import com.github.mlwilli.assetman.maintenance.domain.WorkOrderType
import com.github.mlwilli.assetman.maintenance.service.WorkOrderService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/work-orders")
class WorkOrderController(
    private val workOrderService: WorkOrderService
) {

    @GetMapping
    fun listWorkOrders(
        @RequestParam(required = false) status: WorkOrderStatus?,
        @RequestParam(required = false) priority: WorkOrderPriority?,
        @RequestParam(required = false) type: WorkOrderType?,
        @RequestParam(required = false) assetId: UUID?,
        @RequestParam(required = false) propertyId: UUID?,
        @RequestParam(required = false) unitId: UUID?,
        @RequestParam(required = false) assignedToUserId: UUID?,
        @RequestParam(required = false) search: String?
    ): List<WorkOrderDto> =
        workOrderService.listWorkOrders(
            status = status,
            priority = priority,
            type = type,
            assetId = assetId,
            propertyId = propertyId,
            unitId = unitId,
            assignedToUserId = assignedToUserId,
            search = search
        )

    @GetMapping("/{id}")
    fun getWorkOrder(@PathVariable id: UUID): WorkOrderDto =
        workOrderService.getWorkOrder(id)

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','TECHNICIAN')")
    fun createWorkOrder(
        @RequestBody request: CreateWorkOrderRequest
    ): ResponseEntity<WorkOrderDto> {
        val created = workOrderService.createWorkOrder(request)
        return ResponseEntity.ok(created)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','TECHNICIAN')")
    fun updateWorkOrder(
        @PathVariable id: UUID,
        @RequestBody request: UpdateWorkOrderRequest
    ): ResponseEntity<WorkOrderDto> {
        val updated = workOrderService.updateWorkOrder(id, request)
        return ResponseEntity.ok(updated)
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','TECHNICIAN')")
    fun updateStatus(
        @PathVariable id: UUID,
        @RequestBody request: UpdateWorkOrderStatusRequest
    ): ResponseEntity<WorkOrderDto> {
        val updated = workOrderService.updateStatus(id, request)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
    fun deleteWorkOrder(@PathVariable id: UUID): ResponseEntity<Void> {
        workOrderService.deleteWorkOrder(id)
        return ResponseEntity.noContent().build()
    }
}
