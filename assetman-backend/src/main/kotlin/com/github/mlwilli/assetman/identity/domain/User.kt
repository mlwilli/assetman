package com.github.mlwilli.assetman.identity.domain

import com.github.mlwilli.assetman.common.domain.BaseEntity
import com.github.mlwilli.assetman.common.domain.TenantScoped
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_users_email_tenant",
            columnNames = ["email", "tenant_id"]
        )
    ]
)
class User(

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(nullable = false)
    var fullName: String,

    @Column(nullable = false)
    var email: String,

    @Column(nullable = false)
    var passwordHash: String,

    @Column(nullable = true)
    var displayName: String? = null,

    @Column(nullable = false)
    var active: Boolean = true

) : BaseEntity(), TenantScoped {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private val _roles: MutableSet<Role> = mutableSetOf()

    /**
     * Read-only view of the user's roles.
     * JPA mutates `_roles` internally; callers see a Set<Role>.
     */
    val roles: Set<Role>
        get() = _roles.toSet()

    // -------- Domain helpers --------

    fun addRole(role: Role) {
        _roles.add(role)
    }

    fun removeRole(role: Role) {
        _roles.remove(role)
    }

    fun setRoles(roles: Collection<Role>) {
        _roles.clear()
        _roles.addAll(roles)
    }

    // Convenience constructor used by services/tests
    constructor(
        tenantId: UUID,
        fullName: String,
        email: String,
        passwordHash: String,
        displayName: String? = null,
        roles: Set<Role>,
        active: Boolean = true
    ) : this(
        tenantId = tenantId,
        fullName = fullName,
        email = email,
        passwordHash = passwordHash,
        displayName = displayName,
        active = active
    ) {
        setRoles(roles)
    }
}
