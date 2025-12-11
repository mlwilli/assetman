package com.github.mlwilli.assetman.workorder.repo

import com.github.mlwilli.assetman.workorder.domain.WorkOrder
import com.github.mlwilli.assetman.workorder.domain.WorkOrderPriority
import com.github.mlwilli.assetman.workorder.domain.WorkOrderStatus
import com.github.mlwilli.assetman.workorder.domain.WorkOrderType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID

interface WorkOrderRepository : JpaRepository<WorkOrder, UUID> {

    fun findByIdAndTenantId(id: UUID, tenantId: UUID): WorkOrder?

    @Query(
        """
        SELECT w
        FROM WorkOrder w
        WHERE w.tenantId = :tenantId
          AND (:status IS NULL OR w.status = :status)
          AND (:priority IS NULL OR w.priority = :priority)
          AND (:type IS NULL OR w.type = :type)
          AND (:assetId IS NULL OR w.assetId = :assetId)
          AND (:propertyId IS NULL OR w.propertyId = :propertyId)
          AND (:unitId IS NULL OR w.unitId = :unitId)
          AND (:assignedToUserId IS NULL OR w.assignedToUserId = :assignedToUserId)
          AND (
                :search IS NULL 
                OR lower(w.title) LIKE lower(concat('%', :search, '%'))
                OR lower(coalesce(w.description, '')) LIKE lower(concat('%', :search, '%'))
          )
        ORDER BY w.priority DESC, w.dueDate NULLS LAST, w.createdAt DESC
        """
    )
    fun search(
        @Param("tenantId") tenantId: UUID,
        @Param("status") status: WorkOrderStatus?,
        @Param("priority") priority: WorkOrderPriority?,
        @Param("type") type: WorkOrderType?,
        @Param("assetId") assetId: UUID?,
        @Param("propertyId") propertyId: UUID?,
        @Param("unitId") unitId: UUID?,
        @Param("assignedToUserId") assignedToUserId: UUID?,
        @Param("search") search: String?
    ): List<WorkOrder>

    /**
     * Returns all work orders for a tenant whose dueDate is before the given day.
     * Status filtering (terminal vs non-terminal) is done in the domain/service layer via isOverdue().
     */
    @Query(
        """
        SELECT w
        FROM WorkOrder w
        WHERE w.tenantId = :tenantId
          AND w.dueDate IS NOT NULL
          AND w.dueDate < :today
        """
    )
    fun findDueBefore(
        @Param("tenantId") tenantId: UUID,
        @Param("today") today: LocalDate
    ): List<WorkOrder>
}
