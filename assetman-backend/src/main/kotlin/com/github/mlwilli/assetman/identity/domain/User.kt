package com.github.mlwilli.assetman.identity.domain

import com.github.mlwilli.assetman.shared.domain.BaseEntity
import com.github.mlwilli.assetman.shared.domain.TenantScoped
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_users_email_tenant", columnNames = ["email", "tenant_id"])
    ]
)
class User(
    @Column(nullable = false)
    override val tenantId: UUID,

    @Column(nullable = false)
    var fullName: String,

    @Column(nullable = false)
    var email: String,

    @Column(nullable = false)
    var passwordHash: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    var roles: MutableSet<Role> = mutableSetOf()
) : BaseEntity(), TenantScoped
