package com.github.mlwilli.assetman.identity.service

import com.github.mlwilli.assetman.common.error.NotFoundException
import com.github.mlwilli.assetman.common.security.AuthenticatedUser
import com.github.mlwilli.assetman.common.security.TenantContext
import com.github.mlwilli.assetman.testsupport.setBaseEntityFields
import com.github.mlwilli.assetman.workorder.domain.*
import com.github.mlwilli.assetman.workorder.repo.WorkOrderRepository
import com.github.mlwilli.assetman.workorder.service.WorkOrderService
import com.github.mlwilli.assetman.workorder.web.CreateWorkOrderRequest
import com.github.mlwilli.assetman.workorder.web.UpdateWorkOrderRequest
import com.github.mlwilli.assetman.workorder.web.UpdateWorkOrderStatusRequest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class WorkOrderServiceTest {

    private lateinit var workOrderRepository: WorkOrderRepository
    private lateinit var service: WorkOrderService

    private val tenantId: UUID = UUID.randomUUID()
    private val userId: UUID = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        workOrderRepository = Mockito.mock(WorkOrderRepository::class.java)
        service = WorkOrderService(workOrderRepository)

        TenantContext.set(
            AuthenticatedUser(
                userId = userId,
                tenantId = tenantId,
                email = "tech@tenant.test",
                roles = setOf("TECHNICIAN")
            )
        )
    }

    @AfterEach
    fun tearDown() {
        TenantContext.clear()
    }

    // ---------------------------------------------------------------------
    // listWorkOrders
    // ---------------------------------------------------------------------

    @Test
    fun `listWorkOrders calls repository search with tenant and maps DTOs`() {
        val wo1 = WorkOrder(
            tenantId = tenantId,
            title = "Fix Leak",
            description = "Pipe leaking",
            status = WorkOrderStatus.OPEN,
            priority = WorkOrderPriority.HIGH,
            type = WorkOrderType.REPAIR,
            assetId = UUID.randomUUID(),
            propertyId = null,
            unitId = null,
            locationId = null,
            assignedToUserId = userId,
            createdByUserId = userId,
            dueDate = LocalDate.now().plusDays(1),
            scheduledStart = Instant.now(),
            scheduledEnd = Instant.now().plusSeconds(3600),
            startedAt = null,
            completedAt = null,
            estimatedCost = BigDecimal("200.00"),
            actualCost = null,
            tags = "plumbing,urgent",
            externalTicketRef = null,
            notes = "Shut off main valve first"
        )
        setBaseEntityFields(wo1, UUID.randomUUID())

        val wo2 = WorkOrder(
            tenantId = tenantId,
            title = "Inspect Roof",
            description = "Annual inspection",
            status = WorkOrderStatus.IN_PROGRESS,
            priority = WorkOrderPriority.MEDIUM,
            type = WorkOrderType.INSPECTION,
            assetId = null,
            propertyId = UUID.randomUUID(),
            unitId = null,
            locationId = null,
            assignedToUserId = userId,
            createdByUserId = userId,
            dueDate = LocalDate.now().plusDays(7),
            scheduledStart = Instant.now(),
            scheduledEnd = Instant.now().plusSeconds(7200),
            startedAt = Instant.now(),
            completedAt = null,
            estimatedCost = BigDecimal("0.00"),
            actualCost = null,
            tags = null,
            externalTicketRef = "EXT-1",
            notes = null
        )
        setBaseEntityFields(wo2, UUID.randomUUID())

        Mockito.`when`(
            workOrderRepository.search(
                tenantId = tenantId,
                status = WorkOrderStatus.OPEN,
                priority = WorkOrderPriority.HIGH,
                type = WorkOrderType.REPAIR,
                assetId = null,
                propertyId = null,
                unitId = null,
                assignedToUserId = userId,
                search = "leak"
            )
        ).thenReturn(listOf(wo1))

        val result = service.listWorkOrders(
            status = WorkOrderStatus.OPEN,
            priority = WorkOrderPriority.HIGH,
            type = WorkOrderType.REPAIR,
            assetId = null,
            propertyId = null,
            unitId = null,
            assignedToUserId = userId,
            search = "leak"
        )

        assertEquals(1, result.size)
        val dto = result[0]
        assertEquals(wo1.id, dto.id)
        assertEquals("Fix Leak", dto.title)
        assertEquals(WorkOrderStatus.OPEN, dto.status)
        assertEquals(WorkOrderPriority.HIGH, dto.priority)
        assertEquals(tenantId, dto.tenantId)
    }

    // ---------------------------------------------------------------------
    // getWorkOrder
    // ---------------------------------------------------------------------

    @Test
    fun `getWorkOrder returns DTO when found for tenant`() {
        val id = UUID.randomUUID()
        val wo = WorkOrder(
            tenantId = tenantId,
            title = "Inspect Roof",
            description = "Annual inspection",
            status = WorkOrderStatus.IN_PROGRESS,
            priority = WorkOrderPriority.MEDIUM,
            type = WorkOrderType.INSPECTION,
            assetId = null,
            propertyId = null,
            unitId = null,
            locationId = null,
            assignedToUserId = userId,
            createdByUserId = userId,
            dueDate = LocalDate.now().plusDays(5),
            scheduledStart = Instant.now(),
            scheduledEnd = Instant.now().plusSeconds(3600),
            startedAt = Instant.now(),
            completedAt = null,
            estimatedCost = BigDecimal("0.00"),
            actualCost = null,
            tags = null,
            externalTicketRef = null,
            notes = null
        )
        setBaseEntityFields(wo, id)

        Mockito.`when`(
            workOrderRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(wo)

        val dto = service.getWorkOrder(id)

        assertEquals(id, dto.id)
        assertEquals("Inspect Roof", dto.title)
        assertEquals(WorkOrderStatus.IN_PROGRESS, dto.status)
        assertEquals(tenantId, dto.tenantId)
    }

    @Test
    fun `getWorkOrder throws NotFoundException when not found for tenant`() {
        val id = UUID.randomUUID()

        Mockito.`when`(
            workOrderRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(null)

        assertThrows<NotFoundException> {
            service.getWorkOrder(id)
        }
    }

    // ---------------------------------------------------------------------
    // createWorkOrder
    // ---------------------------------------------------------------------

    @Test
    fun `createWorkOrder populates tenant and createdBy and saves entity`() {
        val request = CreateWorkOrderRequest(
            title = "Fix Leak",
            description = "Pipe leaking in basement",
            priority = WorkOrderPriority.HIGH,
            type = WorkOrderType.REPAIR,
            assetId = UUID.randomUUID(),
            propertyId = null,
            unitId = null,
            locationId = null,
            assignedToUserId = userId,
            dueDate = LocalDate.now().plusDays(2),
            scheduledStart = Instant.now().plusSeconds(3600),
            scheduledEnd = Instant.now().plusSeconds(7200),
            estimatedCost = BigDecimal("200.00"),
            tags = "plumbing",
            externalTicketRef = null,
            notes = "Check valves"
        )

        val saved = WorkOrder(
            tenantId = tenantId,
            title = request.title,
            description = request.description,
            status = WorkOrderStatus.OPEN,
            priority = request.priority,
            type = request.type,
            assetId = request.assetId,
            propertyId = request.propertyId,
            unitId = request.unitId,
            locationId = request.locationId,
            assignedToUserId = request.assignedToUserId,
            createdByUserId = userId,
            dueDate = request.dueDate,
            scheduledStart = request.scheduledStart,
            scheduledEnd = request.scheduledEnd,
            startedAt = null,
            completedAt = null,
            estimatedCost = request.estimatedCost,
            actualCost = null,
            tags = request.tags,
            externalTicketRef = request.externalTicketRef,
            notes = request.notes
        )
        setBaseEntityFields(saved, UUID.randomUUID())

        Mockito.`when`(
            workOrderRepository.save(ArgumentMatchers.any(WorkOrder::class.java))
        ).thenReturn(saved)

        val dto = service.createWorkOrder(request)

        // capture persisted entity to verify fields
        val captor = ArgumentCaptor.forClass(WorkOrder::class.java)
        Mockito.verify(workOrderRepository).save(captor.capture())
        val persisted = captor.value

        assertEquals(tenantId, persisted.tenantId)
        assertEquals(userId, persisted.createdByUserId)
        assertEquals("Fix Leak", persisted.title)
        assertEquals(WorkOrderStatus.OPEN, persisted.status)
        assertEquals(WorkOrderPriority.HIGH, persisted.priority)

        // DTO basics
        assertEquals(saved.id, dto.id)
        assertEquals("Fix Leak", dto.title)
    }

    // ---------------------------------------------------------------------
    // updateWorkOrder (non-lifecycle fields only)
    // ---------------------------------------------------------------------

    @Test
    fun `updateWorkOrder updates non-lifecycle fields and keeps status unchanged`() {
        val id = UUID.randomUUID()

        val existing = WorkOrder(
            tenantId = tenantId,
            title = "Old Title",
            description = "Old description",
            status = WorkOrderStatus.OPEN,
            priority = WorkOrderPriority.LOW,
            type = WorkOrderType.REPAIR,
            assetId = null,
            propertyId = null,
            unitId = null,
            locationId = null,
            assignedToUserId = null,
            createdByUserId = userId,
            dueDate = null,
            scheduledStart = null,
            scheduledEnd = null,
            startedAt = null,
            completedAt = null,
            estimatedCost = null,
            actualCost = null,
            tags = null,
            externalTicketRef = null,
            notes = null
        )
        setBaseEntityFields(existing, id)

        Mockito.`when`(
            workOrderRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(existing)

        Mockito.`when`(
            workOrderRepository.save(existing)
        ).thenReturn(existing)

        val request = UpdateWorkOrderRequest(
            title = "New Title",
            description = "New description",
            priority = WorkOrderPriority.HIGH,
            type = WorkOrderType.INSPECTION,
            assetId = UUID.randomUUID(),
            propertyId = UUID.randomUUID(),
            unitId = UUID.randomUUID(),
            locationId = UUID.randomUUID(),
            assignedToUserId = userId,
            dueDate = LocalDate.now().plusDays(3),
            scheduledStart = Instant.now(),
            scheduledEnd = Instant.now().plusSeconds(3600),
            estimatedCost = BigDecimal("100.00"),
            actualCost = BigDecimal("90.00"),
            tags = "tag1,tag2",
            externalTicketRef = "TICKET-1",
            notes = "Updated"
        )

        val dto = service.updateWorkOrder(id, request)

        // non-lifecycle fields updated
        assertEquals("New Title", existing.title)
        assertEquals("New description", existing.description)
        assertEquals(WorkOrderPriority.HIGH, existing.priority)
        assertEquals(WorkOrderType.INSPECTION, existing.type)
        assertEquals(request.assetId, existing.assetId)
        assertEquals(request.propertyId, existing.propertyId)
        assertEquals(request.unitId, existing.unitId)
        assertEquals(request.locationId, existing.locationId)
        assertEquals(request.assignedToUserId, existing.assignedToUserId)
        assertEquals(request.dueDate, existing.dueDate)
        assertEquals(request.scheduledStart, existing.scheduledStart)
        assertEquals(request.scheduledEnd, existing.scheduledEnd)
        assertEquals(request.estimatedCost, existing.estimatedCost)
        assertEquals(request.actualCost, existing.actualCost)
        assertEquals(request.tags, existing.tags)
        assertEquals(request.externalTicketRef, existing.externalTicketRef)
        assertEquals(request.notes, existing.notes)

        // lifecycle fields unchanged
        assertEquals(WorkOrderStatus.OPEN, existing.status)
        assertNull(existing.startedAt)
        assertNull(existing.completedAt)

        // dto mirrors entity
        assertEquals("New Title", dto.title)
        assertEquals("New description", dto.description)
        assertEquals(WorkOrderPriority.HIGH, dto.priority)
        assertEquals(WorkOrderType.INSPECTION, dto.type)
        assertEquals(WorkOrderStatus.OPEN, dto.status)
    }

    @Test
    fun `updateWorkOrder throws NotFoundException when not found`() {
        val id = UUID.randomUUID()

        Mockito.`when`(
            workOrderRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(null)

        assertThrows<NotFoundException> {
            service.updateWorkOrder(
                id,
                UpdateWorkOrderRequest(
                    title = "X",
                    description = null,
                    priority = WorkOrderPriority.LOW,
                    type = WorkOrderType.REPAIR
                )
            )
        }
    }

    // ---------------------------------------------------------------------
    // updateStatus (lifecycle transitions)
    // ---------------------------------------------------------------------

    @Test
    fun `updateStatus moves to IN_PROGRESS and sets startedAt when null`() {
        val id = UUID.randomUUID()

        val existing = WorkOrder(
            tenantId = tenantId,
            title = "WO",
            description = null,
            status = WorkOrderStatus.OPEN,
            priority = WorkOrderPriority.LOW,
            type = WorkOrderType.REPAIR,
            assetId = null,
            propertyId = null,
            unitId = null,
            locationId = null,
            assignedToUserId = null,
            createdByUserId = userId,
            dueDate = null,
            scheduledStart = null,
            scheduledEnd = null,
            startedAt = null,
            completedAt = null,
            estimatedCost = null,
            actualCost = null,
            tags = null,
            externalTicketRef = null,
            notes = null
        )
        setBaseEntityFields(existing, id)

        Mockito.`when`(
            workOrderRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(existing)

        Mockito.`when`(
            workOrderRepository.save(existing)
        ).thenReturn(existing)

        val dto = service.updateStatus(id, UpdateWorkOrderStatusRequest(WorkOrderStatus.IN_PROGRESS))

        assertEquals(WorkOrderStatus.IN_PROGRESS, existing.status)
        assertNotNull(existing.startedAt)
        assertEquals(WorkOrderStatus.IN_PROGRESS, dto.status)
    }

    @Test
    fun `updateStatus moves to COMPLETED and sets completedAt when null`() {
        val id = UUID.randomUUID()

        val existing = WorkOrder(
            tenantId = tenantId,
            title = "WO",
            description = null,
            status = WorkOrderStatus.IN_PROGRESS,
            priority = WorkOrderPriority.LOW,
            type = WorkOrderType.REPAIR,
            assetId = null,
            propertyId = null,
            unitId = null,
            locationId = null,
            assignedToUserId = null,
            createdByUserId = userId,
            dueDate = null,
            scheduledStart = null,
            scheduledEnd = null,
            startedAt = Instant.now(),
            completedAt = null,
            estimatedCost = null,
            actualCost = null,
            tags = null,
            externalTicketRef = null,
            notes = null
        )
        setBaseEntityFields(existing, id)

        Mockito.`when`(
            workOrderRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(existing)

        Mockito.`when`(
            workOrderRepository.save(existing)
        ).thenReturn(existing)

        val dto = service.updateStatus(id, UpdateWorkOrderStatusRequest(WorkOrderStatus.COMPLETED))

        assertEquals(WorkOrderStatus.COMPLETED, existing.status)
        assertNotNull(existing.completedAt)
        assertEquals(WorkOrderStatus.COMPLETED, dto.status)
    }

    @Test
    fun `updateStatus throws NotFoundException when work order missing`() {
        val id = UUID.randomUUID()

        Mockito.`when`(
            workOrderRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(null)

        assertThrows<NotFoundException> {
            service.updateStatus(id, UpdateWorkOrderStatusRequest(WorkOrderStatus.COMPLETED))
        }
    }

    // ---------------------------------------------------------------------
    // deleteWorkOrder
    // ---------------------------------------------------------------------

    @Test
    fun `deleteWorkOrder deletes when found`() {
        val id = UUID.randomUUID()

        val existing = WorkOrder(
            tenantId = tenantId,
            title = "To Delete",
            description = null,
            status = WorkOrderStatus.OPEN,
            priority = WorkOrderPriority.LOW,
            type = WorkOrderType.REPAIR,
            assetId = null,
            propertyId = null,
            unitId = null,
            locationId = null,
            assignedToUserId = null,
            createdByUserId = userId,
            dueDate = null,
            scheduledStart = null,
            scheduledEnd = null,
            startedAt = null,
            completedAt = null,
            estimatedCost = null,
            actualCost = null,
            tags = null,
            externalTicketRef = null,
            notes = null
        )
        setBaseEntityFields(existing, id)

        Mockito.`when`(
            workOrderRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(existing)

        service.deleteWorkOrder(id)

        Mockito.verify(workOrderRepository).delete(existing)
    }

    @Test
    fun `deleteWorkOrder is noop when not found`() {
        val id = UUID.randomUUID()

        Mockito.`when`(
            workOrderRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(null)

        service.deleteWorkOrder(id)

        Mockito.verify(workOrderRepository, Mockito.never())
            .delete(ArgumentMatchers.any(WorkOrder::class.java))

    }

    // ---------------------------------------------------------------------
    // Helper to set BaseEntity fields
    // ---------------------------------------------------------------------

}
