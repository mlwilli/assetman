package com.github.mlwilli.assetman.location.service

import com.github.mlwilli.assetman.common.error.ConflictException
import com.github.mlwilli.assetman.common.error.NotFoundException
import com.github.mlwilli.assetman.common.security.AuthenticatedUser
import com.github.mlwilli.assetman.common.security.TenantContext
import com.github.mlwilli.assetman.location.domain.Location
import com.github.mlwilli.assetman.location.domain.LocationType
import com.github.mlwilli.assetman.location.repo.LocationRepository
import com.github.mlwilli.assetman.location.web.CreateLocationRequest
import com.github.mlwilli.assetman.location.web.UpdateLocationRequest
import org.junit.jupiter.api.*
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.util.UUID

class LocationServiceTest {

    private lateinit var locationRepository: LocationRepository
    private lateinit var service: LocationService

    private val tenantId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        locationRepository = Mockito.mock(LocationRepository::class.java)
        service = LocationService(locationRepository)

        TenantContext.set(
            AuthenticatedUser(
                userId = userId,
                tenantId = tenantId,
                email = "tester@example.com",
                roles = setOf("ADMIN")
            )
        )
    }

    @AfterEach
    fun tearDown() {
        TenantContext.clear()
    }

    // ----------------------------------------------------------------------
    // listLocations
    // ----------------------------------------------------------------------

    @Test
    fun `listLocations returns mapped DTOs`() {
        val loc = Location(
            tenantId = tenantId,
            name = "Boise HQ",
            type = LocationType.SITE,
            parentId = null,
            path = "/some/path"
        )
        setEntityId(loc, UUID.randomUUID())

        Mockito.`when`(
            locationRepository.search(
                tenantId = tenantId,
                type = LocationType.SITE,
                parentId = null,
                active = null,
                search = "boise"
            )
        ).thenReturn(listOf(loc))

        val result = service.listLocations(
            type = LocationType.SITE,
            parentId = null,
            active = null,
            search = "boise"
        )

        Assertions.assertEquals(1, result.size)
        val dto = result[0]
        Assertions.assertEquals(loc.id, dto.id)
        Assertions.assertEquals(tenantId, dto.tenantId)
        Assertions.assertEquals("Boise HQ", dto.name)
        Assertions.assertEquals(LocationType.SITE, dto.type)
        Assertions.assertEquals("/some/path", dto.path)
    }

    // ----------------------------------------------------------------------
    // getLocation
    // ----------------------------------------------------------------------

    @Test
    fun `getLocation returns dto for existing tenant-scoped location`() {
        val id = UUID.randomUUID()
        val loc = Location(
            tenantId = tenantId,
            name = "Data Center",
            type = LocationType.BUILDING,
            parentId = null,
            path = "/dc"
        )
        setEntityId(loc, id)

        Mockito.`when`(
            locationRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(loc)

        val dto = service.getLocation(id)

        Assertions.assertEquals(id, dto.id)
        Assertions.assertEquals(tenantId, dto.tenantId)
        Assertions.assertEquals("Data Center", dto.name)
        Assertions.assertEquals(LocationType.BUILDING, dto.type)
        Assertions.assertEquals("/dc", dto.path)
    }

    @Test
    fun `getLocation throws NotFoundException when missing`() {
        val id = UUID.randomUUID()

        Mockito.`when`(
            locationRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(null)

        Assertions.assertThrows(NotFoundException::class.java) {
            service.getLocation(id)
        }
    }

    // ----------------------------------------------------------------------
    // createLocation (no parent)
    // ----------------------------------------------------------------------

    @Test
    fun `createLocation without parent saves root location with generated path`() {
        val request = CreateLocationRequest(
            name = "HQ",
            type = LocationType.SITE,
            parentId = null
        )

        val captor = ArgumentCaptor.forClass(Location::class.java)
        Mockito.`when`(locationRepository.save(Mockito.any(Location::class.java)))
            .thenAnswer { invocation -> invocation.arguments[0] as Location }

        val dto = service.createLocation(request)

        Mockito.verify(locationRepository, Mockito.atLeastOnce()).save(captor.capture())
        val persisted = captor.value

        Assertions.assertNotNull(persisted.id, "Saved location should have an id")
        Assertions.assertEquals(persisted.id, dto.id, "DTO id should match saved entity id")

        // For root: "/<id>"
        Assertions.assertEquals("/${dto.id}", persisted.path)
        Assertions.assertEquals(persisted.path, dto.path)

        Assertions.assertEquals(tenantId, persisted.tenantId)
        Assertions.assertEquals(request.name, persisted.name)
        Assertions.assertEquals(request.type, persisted.type)
    }

    // ----------------------------------------------------------------------
    // createLocation (with parent)
    // ----------------------------------------------------------------------

    @Test
    fun `createLocation with parent computes path based on parent path`() {
        val parentId = UUID.randomUUID()

        val parent = Location(
            tenantId = tenantId,
            name = "campus",
            type = LocationType.SITE
        ).apply {
            path = "/campus"
            setBaseFields(this, parentId)
        }

        Mockito.`when`(
            locationRepository.findByIdAndTenantId(parentId, tenantId)
        ).thenReturn(parent)

        Mockito.`when`(
            locationRepository.save(Mockito.any(Location::class.java))
        ).thenAnswer { it.arguments[0] as Location }

        val request = CreateLocationRequest(
            name = "Building A",
            type = LocationType.BUILDING,
            parentId = parentId
        )

        val dto = service.createLocation(request)

        val captor = ArgumentCaptor.forClass(Location::class.java)
        Mockito.verify(locationRepository, Mockito.atLeastOnce()).save(captor.capture())
        val savedChild = captor.allValues.last()

        // parentId correct
        Assertions.assertEquals(parentId, savedChild.parentId)

        // Path should not be null
        Assertions.assertNotNull(savedChild.path, "Child path should not be null")
        val childPath = savedChild.path!!

        // Path: "/campus/<child-id>"
        Assertions.assertTrue(
            childPath.startsWith(parent.path + "/"),
            "Child path should start with parent path plus '/'; actual=$childPath"
        )
        Assertions.assertEquals(
            savedChild.id.toString(),
            childPath.substringAfterLast("/"),
            "Last path segment should be the child id"
        )

        Assertions.assertEquals(savedChild.id, dto.id)
        Assertions.assertEquals(savedChild.path, dto.path)
    }

    // ----------------------------------------------------------------------
    // updateLocation
    // ----------------------------------------------------------------------

    @Test
    fun `updateLocation updates fields and recalculates path when parent changes`() {
        val id = UUID.randomUUID()
        val parentId = UUID.randomUUID()

        val existing = Location(
            tenantId = tenantId,
            name = "Old Name",
            type = LocationType.ROOM,
            parentId = null,
            path = "/old"
        )
        setEntityId(existing, id)

        val parent = Location(
            tenantId = tenantId,
            name = "Floor 1",
            type = LocationType.FLOOR,
            parentId = null,
            path = "/$parentId"
        )
        setEntityId(parent, parentId)

        Mockito.`when`(
            locationRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(existing)

        Mockito.`when`(
            locationRepository.findByIdAndTenantId(parentId, tenantId)
        ).thenReturn(parent)

        // No descendants in this scenario.
        Mockito.`when`(
            locationRepository.findAllByTenantIdAndPathStartingWith(tenantId, "/old/")
        ).thenReturn(emptyList())

        Mockito.`when`(
            locationRepository.save(Mockito.any(Location::class.java))
        ).thenAnswer { invocation -> invocation.arguments[0] as Location }

        val request = UpdateLocationRequest(
            name = "New Room",
            type = LocationType.ROOM,
            parentId = parentId
        )

        val dto = service.updateLocation(id, request)

        Assertions.assertEquals("New Room", dto.name)
        Assertions.assertEquals(LocationType.ROOM, dto.type)
        Assertions.assertEquals(parentId, dto.parentId)
        Assertions.assertEquals("/$parentId/$id", dto.path)
    }

    @Test
    fun `updateLocation throws NotFoundException when location is missing`() {
        val id = UUID.randomUUID()

        Mockito.`when`(
            locationRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(null)

        Assertions.assertThrows(NotFoundException::class.java) {
            service.updateLocation(
                id,
                UpdateLocationRequest(
                    name = "Does not matter",
                    type = LocationType.OTHER,
                    parentId = null
                )
            )
        }
    }

    @Test
    fun `updateLocation throws ConflictException when setting self as parent`() {
        val id = UUID.randomUUID()
        val existing = Location(
            tenantId = tenantId,
            name = "Room",
            type = LocationType.ROOM,
            parentId = null,
            path = "/$id"
        )
        setEntityId(existing, id)

        Mockito.`when`(
            locationRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(existing)

        Assertions.assertThrows(ConflictException::class.java) {
            service.updateLocation(
                id,
                UpdateLocationRequest(
                    name = "Room",
                    type = LocationType.ROOM,
                    parentId = id
                )
            )
        }

        // Only the initial lookup should have happened.
        Mockito.verify(locationRepository).findByIdAndTenantId(id, tenantId)
        Mockito.verifyNoMoreInteractions(locationRepository)
    }

    @Test
    fun `updateLocation throws ConflictException when setting descendant as parent`() {
        val rootId = UUID.randomUUID()
        val childId = UUID.randomUUID()

        val root = Location(
            tenantId = tenantId,
            name = "Root",
            type = LocationType.SITE,
            parentId = null,
            path = "/$rootId"
        )
        setEntityId(root, rootId)

        val child = Location(
            tenantId = tenantId,
            name = "Child",
            type = LocationType.BUILDING,
            parentId = rootId,
            path = "/$rootId/$childId"
        )
        setEntityId(child, childId)

        Mockito.`when`(
            locationRepository.findByIdAndTenantId(rootId, tenantId)
        ).thenReturn(root)

        Mockito.`when`(
            locationRepository.findByIdAndTenantId(childId, tenantId)
        ).thenReturn(child)

        Assertions.assertThrows(ConflictException::class.java) {
            service.updateLocation(
                rootId,
                UpdateLocationRequest(
                    name = "Root Updated",
                    type = LocationType.SITE,
                    parentId = childId
                )
            )
        }

        Mockito.verify(locationRepository).findByIdAndTenantId(rootId, tenantId)
        Mockito.verify(locationRepository).findByIdAndTenantId(childId, tenantId)
        Mockito.verifyNoMoreInteractions(locationRepository)
    }

    @Test
    fun `updateLocation reparenting moves subtree and updates child paths`() {
        val rootId = UUID.randomUUID()
        val newParentId = UUID.randomUUID()
        val childId = UUID.randomUUID()

        val root = Location(
            tenantId = tenantId,
            name = "Root",
            type = LocationType.SITE,
            parentId = null,
            path = "/$rootId"
        )
        setEntityId(root, rootId)

        val newParent = Location(
            tenantId = tenantId,
            name = "Campus",
            type = LocationType.SITE,
            parentId = null,
            path = "/$newParentId"
        )
        setEntityId(newParent, newParentId)

        val child = Location(
            tenantId = tenantId,
            name = "Child",
            type = LocationType.BUILDING,
            parentId = rootId,
            path = "/$rootId/$childId"
        )
        setEntityId(child, childId)

        Mockito.`when`(
            locationRepository.findByIdAndTenantId(rootId, tenantId)
        ).thenReturn(root)

        Mockito.`when`(
            locationRepository.findByIdAndTenantId(newParentId, tenantId)
        ).thenReturn(newParent)

        Mockito.`when`(
            locationRepository.findAllByTenantIdAndPathStartingWith(tenantId, "/$rootId/")
        ).thenReturn(listOf(child))

        Mockito.`when`(
            locationRepository.save(Mockito.any(Location::class.java))
        ).thenAnswer { invocation -> invocation.arguments[0] as Location }

        val request = UpdateLocationRequest(
            name = "Root Renamed",
            type = LocationType.SITE,
            parentId = newParentId
        )

        val dto = service.updateLocation(rootId, request)

        Assertions.assertEquals("/$newParentId/$rootId", dto.path)
        Assertions.assertEquals("/$newParentId/$rootId/$childId", child.path)
        Assertions.assertEquals(newParentId, dto.parentId)
    }

    // ----------------------------------------------------------------------
    // deleteLocation
    // ----------------------------------------------------------------------

    @Test
    fun `deleteLocation deletes entity when found`() {
        val id = UUID.randomUUID()
        val loc = Location(
            tenantId = tenantId,
            name = "Obsolete",
            type = LocationType.OTHER,
            parentId = null,
            path = "/obsolete"
        )
        setEntityId(loc, id)

        Mockito.`when`(
            locationRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(loc)

        Mockito.`when`(
            locationRepository.existsByTenantIdAndParentId(tenantId, id)
        ).thenReturn(false)

        service.deleteLocation(id)

        Mockito.verify(locationRepository).findByIdAndTenantId(id, tenantId)
        Mockito.verify(locationRepository).existsByTenantIdAndParentId(tenantId, id)
        Mockito.verify(locationRepository).delete(loc)
        Mockito.verifyNoMoreInteractions(locationRepository)
    }

    @Test
    fun `deleteLocation is silent when entity does not exist`() {
        val id = UUID.randomUUID()

        Mockito.`when`(
            locationRepository.findByIdAndTenantId(id, tenantId)
        ).thenReturn(null)

        service.deleteLocation(id)

        Mockito.verify(locationRepository).findByIdAndTenantId(id, tenantId)
        // No delete or other calls should happen.
        Mockito.verifyNoMoreInteractions(locationRepository)
    }

    // ----------------------------------------------------------------------
    // getLocationTree
    // ----------------------------------------------------------------------

    @Test
    fun `getLocationTree builds nested tree by parentId`() {
        val root1Id = UUID.randomUUID()
        val root2Id = UUID.randomUUID()
        val child1Id = UUID.randomUUID()
        val child2Id = UUID.randomUUID()

        val root1 = Location(
            tenantId = tenantId,
            name = "Root 1",
            type = LocationType.SITE,
            parentId = null,
            path = "/$root1Id"
        )
        val root2 = Location(
            tenantId = tenantId,
            name = "Root 2",
            type = LocationType.SITE,
            parentId = null,
            path = "/$root2Id"
        )
        val child1 = Location(
            tenantId = tenantId,
            name = "Child 1",
            type = LocationType.BUILDING,
            parentId = root1Id,
            path = "/$root1Id/$child1Id"
        )
        val child2 = Location(
            tenantId = tenantId,
            name = "Child 2",
            type = LocationType.ROOM,
            parentId = child1Id,
            path = "/$root1Id/$child1Id/$child2Id"
        )

        setEntityId(root1, root1Id)
        setEntityId(root2, root2Id)
        setEntityId(child1, child1Id)
        setEntityId(child2, child2Id)

        Mockito.`when`(
            locationRepository.findAllByTenantIdAndActiveTrueOrderByNameAsc(tenantId)
        ).thenReturn(listOf(root1, root2, child1, child2))

        val tree = service.getLocationTree()

        Assertions.assertEquals(2, tree.size, "Should have two root nodes in the tree")

        val root1Node = tree.first { it.id == root1Id }
        val root2Node = tree.first { it.id == root2Id }

        Assertions.assertEquals("Root 1", root1Node.name)
        Assertions.assertEquals("Root 2", root2Node.name)

        Assertions.assertEquals(1, root1Node.children.size)
        val child1Node = root1Node.children[0]
        Assertions.assertEquals(child1Id, child1Node.id)
        Assertions.assertEquals("Child 1", child1Node.name)

        Assertions.assertEquals(1, child1Node.children.size)
        val child2Node = child1Node.children[0]
        Assertions.assertEquals(child2Id, child2Node.id)
        Assertions.assertEquals("Child 2", child2Node.name)

        Assertions.assertTrue(root2Node.children.isEmpty())
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    private fun setEntityId(entity: Any, id: UUID) {
        val field = entity.javaClass.superclass.getDeclaredField("id")
        field.isAccessible = true
        field.set(entity, id)
    }

    private fun setBaseFields(entity: Any, id: UUID) {
        val field = entity.javaClass.superclass.getDeclaredField("id")
        field.isAccessible = true
        field.set(entity, id)
    }
}
