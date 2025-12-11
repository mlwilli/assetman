package com.github.mlwilli.assetman.identity.service

import com.github.mlwilli.assetman.workorder.domain.WorkOrder
import com.github.mlwilli.assetman.workorder.domain.WorkOrderPriority
import com.github.mlwilli.assetman.workorder.domain.WorkOrderStatus
import com.github.mlwilli.assetman.workorder.domain.WorkOrderType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class WorkOrderTest {

    private val tenantId: UUID = UUID.randomUUID()
    private val userId: UUID = UUID.randomUUID()

    private fun newWorkOrder(
        status: WorkOrderStatus = WorkOrderStatus.OPEN,
        dueDate: LocalDate? = null,
        startedAt: Instant? = null,
        completedAt: Instant? = null
    ): WorkOrder {
        return WorkOrder(
            tenantId = tenantId,
            title = "Test WO",
            description = "desc",
            status = status,
            priority = WorkOrderPriority.MEDIUM,
            type = WorkOrderType.REPAIR,
            assetId = null,
            propertyId = null,
            unitId = null,
            locationId = null,
            assignedToUserId = null,
            createdByUserId = userId,
            dueDate = dueDate,
            scheduledStart = null,
            scheduledEnd = null,
            startedAt = startedAt,
            completedAt = completedAt,
            estimatedCost = BigDecimal("0.00"),
            actualCost = null,
            tags = null,
            externalTicketRef = null,
            notes = null
        )
    }

    // ---------------------------------------------------------------------
    // isOverdue
    // ---------------------------------------------------------------------

    @Test
    fun `isOverdue is false when no dueDate`() {
        val wo = newWorkOrder(dueDate = null, status = WorkOrderStatus.OPEN)

        assertFalse(wo.isOverdue(LocalDate.now()))
    }

    @Test
    fun `isOverdue is false when dueDate is today or in future`() {
        val today = LocalDate.now()
        val woToday = newWorkOrder(dueDate = today, status = WorkOrderStatus.OPEN)
        val woFuture = newWorkOrder(dueDate = today.plusDays(1), status = WorkOrderStatus.OPEN)

        assertFalse(woToday.isOverdue(today))
        assertFalse(woFuture.isOverdue(today))
    }

    @Test
    fun `isOverdue is true when dueDate is in past and status not terminal`() {
        val today = LocalDate.now()
        val wo = newWorkOrder(
            dueDate = today.minusDays(1),
            status = WorkOrderStatus.IN_PROGRESS
        )

        assertTrue(wo.isOverdue(today))
    }

    @Test
    fun `isOverdue is false when status is COMPLETED or CANCELLED`() {
        val today = LocalDate.now()
        val past = today.minusDays(2)

        val completed = newWorkOrder(dueDate = past, status = WorkOrderStatus.COMPLETED)
        val cancelled = newWorkOrder(dueDate = past, status = WorkOrderStatus.CANCELLED)

        assertFalse(completed.isOverdue(today))
        assertFalse(cancelled.isOverdue(today))
    }

    // ---------------------------------------------------------------------
    // start()
    // ---------------------------------------------------------------------

    @Test
    fun `start moves to IN_PROGRESS and sets startedAt when null`() {
        val wo = newWorkOrder(status = WorkOrderStatus.OPEN, startedAt = null)

        wo.start()

        assertEquals(WorkOrderStatus.IN_PROGRESS, wo.status)
        assertNotNull(wo.startedAt)
    }

    @Test
    fun `start keeps existing startedAt when already set`() {
        val existingStarted = Instant.now().minusSeconds(3600)
        val wo = newWorkOrder(
            status = WorkOrderStatus.OPEN,
            startedAt = existingStarted
        )

        wo.start()

        assertEquals(WorkOrderStatus.IN_PROGRESS, wo.status)
        assertEquals(existingStarted, wo.startedAt)
    }

    @Test
    fun `start throws when work order is COMPLETED or CANCELLED`() {
        val completed = newWorkOrder(status = WorkOrderStatus.COMPLETED)
        val cancelled = newWorkOrder(status = WorkOrderStatus.CANCELLED)

        assertThrows(IllegalStateException::class.java) {
            completed.start()
        }
        assertThrows(IllegalStateException::class.java) {
            cancelled.start()
        }
    }

    // ---------------------------------------------------------------------
    // complete()
    // ---------------------------------------------------------------------

    @Test
    fun `complete moves to COMPLETED and sets completedAt when null`() {
        val wo = newWorkOrder(
            status = WorkOrderStatus.IN_PROGRESS,
            startedAt = Instant.now(),
            completedAt = null
        )

        wo.complete()

        assertEquals(WorkOrderStatus.COMPLETED, wo.status)
        assertNotNull(wo.completedAt)
    }

    @Test
    fun `complete keeps existing completedAt when already set`() {
        val completedAt = Instant.now().minusSeconds(600)
        val wo = newWorkOrder(
            status = WorkOrderStatus.IN_PROGRESS,
            startedAt = Instant.now().minusSeconds(3600),
            completedAt = completedAt
        )

        wo.complete()

        assertEquals(WorkOrderStatus.COMPLETED, wo.status)
        assertEquals(completedAt, wo.completedAt)
    }

    @Test
    fun `complete throws when work order is CANCELLED`() {
        val wo = newWorkOrder(status = WorkOrderStatus.CANCELLED)

        assertThrows(IllegalStateException::class.java) {
            wo.complete()
        }
    }

    // ---------------------------------------------------------------------
    // hold()
    // ---------------------------------------------------------------------

    @Test
    fun `hold moves to ON_HOLD from OPEN or IN_PROGRESS`() {
        val open = newWorkOrder(status = WorkOrderStatus.OPEN)
        val inProgress = newWorkOrder(status = WorkOrderStatus.IN_PROGRESS)

        open.hold()
        inProgress.hold()

        assertEquals(WorkOrderStatus.ON_HOLD, open.status)
        assertEquals(WorkOrderStatus.ON_HOLD, inProgress.status)
    }

    @Test
    fun `hold throws when work order is COMPLETED or CANCELLED`() {
        val completed = newWorkOrder(status = WorkOrderStatus.COMPLETED)
        val cancelled = newWorkOrder(status = WorkOrderStatus.CANCELLED)

        assertThrows(IllegalStateException::class.java) {
            completed.hold()
        }
        assertThrows(IllegalStateException::class.java) {
            cancelled.hold()
        }
    }

    // ---------------------------------------------------------------------
    // cancel()
    // ---------------------------------------------------------------------

    @Test
    fun `cancel always sets status to CANCELLED`() {
        val wo = newWorkOrder(status = WorkOrderStatus.IN_PROGRESS)

        wo.cancel()

        assertEquals(WorkOrderStatus.CANCELLED, wo.status)
    }
}
