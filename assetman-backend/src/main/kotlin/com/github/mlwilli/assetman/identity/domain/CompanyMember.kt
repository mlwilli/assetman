package com.github.mlwilli.assetman.identity.domain

import com.github.mlwilli.assetman.common.domain.BaseEntity
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "company_members",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_company_member",
            columnNames = ["company_id", "user_id"]
        )
    ],
    indexes = [
        Index(name = "ix_company_member_company", columnList = "tenant_id, company_id"),
        Index(name = "ix_company_member_user", columnList = "tenant_id, user_id")
    ]
)
class CompanyMember(

    @Column(name = "tenant_id", nullable = false, updatable = false)
    val tenantId: UUID,

    @Column(name = "company_id", nullable = false, updatable = false)
    val companyId: UUID,

    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,

    @Column(nullable = false)
    var active: Boolean = true

) : BaseEntity() {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "company_member_roles",
        joinColumns = [JoinColumn(name = "company_member_id")]
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private val _roles: MutableSet<Role> = mutableSetOf(Role.USER)

    val roles: Set<Role>
        get() = _roles.toSet()

    fun setRoles(roles: Collection<Role>) {
        _roles.clear()
        _roles.addAll(roles)
    }
}
