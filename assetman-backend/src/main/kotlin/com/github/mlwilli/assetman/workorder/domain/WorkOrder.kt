package com.github.mlwilli.assetman.workorder.domain

import com.github.mlwilli.assetman.common.domain.BaseEntity
import com.github.mlwilli.assetman.common.domain.TenantScoped
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(
    name = "work_orders",
    indexes = [
        Index(name = "idx_work_orders_tenant", columnList = "tenant_id"),
        Index(name = "idx_work_orders_status", columnList = "status"),
        Index(name = "idx_work_orders_priority", columnList = "priority"),
        Index(name = "idx_work_orders_asset", columnList = "asset_id"),
        Index(name = "idx_work_orders_property", columnList = "property_id"),
        Index(name = "idx_work_orders_unit", columnList = "unit_id"),
        Index(name = "idx_work_orders_assigned", columnList = "assigned_to_user_id")
    ]
)
class WorkOrder(
    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(nullable = false, length = 255)
    var title: String,

    @Column(nullable = true, length = 8000)
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: WorkOrderStatus = WorkOrderStatus.OPEN,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var priority: WorkOrderPriority = WorkOrderPriority.MEDIUM,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var type: WorkOrderType = WorkOrderType.CORRECTIVE,

    // Optional links to other domain objects
    @Column(name = "asset_id", nullable = true)
    var assetId: UUID? = null,

    @Column(name = "property_id", nullable = true)
    var propertyId: UUID? = null,

    @Column(name = "unit_id", nullable = true)
    var unitId: UUID? = null,

    @Column(name = "location_id", nullable = true)
    var locationId: UUID? = null,

    // Assignment / ownership
    @Column(name = "assigned_to_user_id", nullable = true)
    var assignedToUserId: UUID? = null,

    @Column(name = "created_by_user_id", nullable = false)
    var createdByUserId: UUID,

    // Dates / scheduling
    @Column(name = "due_date", nullable = true)
    var dueDate: LocalDate? = null,

    @Column(name = "scheduled_start", nullable = true)
    var scheduledStart: Instant? = null,

    @Column(name = "scheduled_end", nullable = true)
    var scheduledEnd: Instant? = null,

    @Column(name = "started_at", nullable = true)
    var startedAt: Instant? = null,

    @Column(name = "completed_at", nullable = true)
    var completedAt: Instant? = null,

    // Cost tracking
    @Column(name = "estimated_cost", nullable = true, precision = 14, scale = 2)
    var estimatedCost: BigDecimal? = null,

    @Column(name = "actual_cost", nullable = true, precision = 14, scale = 2)
    var actualCost: BigDecimal? = null,

    // Simple tags and external references
    @Column(name = "tags", nullable = true, length = 1024)
    var tags: String? = null, // comma-separated tags for now

    @Column(name = "external_ticket_ref", nullable = true, length = 255)
    var externalTicketRef: String? = null,

    @Column(name = "notes", nullable = true, length = 4000)
    var notes: String? = null
) : BaseEntity(), TenantScoped
