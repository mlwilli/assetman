package com.github.mlwilli.assetman.identity.domain

import com.github.mlwilli.assetman.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "tenants")
class Tenant(
    @Column(nullable = false, unique = true)
    val name: String,

    @Column(nullable = false, unique = true)
    val slug: String
) : BaseEntity()
