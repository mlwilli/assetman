package com.github.mlwilli.assetman.asset.domain

import com.github.mlwilli.assetman.common.domain.BaseEntity
import com.github.mlwilli.assetman.common.domain.TenantScoped
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(
    name = "assets",
    indexes = [
        Index(name = "idx_assets_tenant", columnList = "tenant_id"),
        Index(name = "idx_assets_status", columnList = "status"),
        Index(name = "idx_assets_location", columnList = "location_id"),
        Index(name = "idx_assets_property", columnList = "property_id"),
        Index(name = "idx_assets_unit", columnList = "unit_id"),
        Index(name = "idx_assets_assigned_user", columnList = "assigned_user_id"),
        Index(name = "idx_assets_serial", columnList = "serial_number"),
        Index(name = "idx_assets_tag", columnList = "asset_tag")
    ]
)
class Asset(

    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(nullable = false, length = 255)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: AssetStatus = AssetStatus.IN_SERVICE,

    /**
     * High-level category, e.g. "IT", "VEHICLE", "HVAC", "APPLIANCE".
     */
    @Column(nullable = true, length = 255)
    var category: String? = null,

    /**
     * Human-friendly code or internal inventory code, e.g. "ASSET-000123".
     */
    @Column(name = "code", nullable = true, length = 64)
    var code: String? = null,

    /**
     * Asset tag or barcode identifier printed on the asset label.
     */
    @Column(name = "asset_tag", nullable = true, length = 128)
    var assetTag: String? = null,

    /**
     * Manufacturer and model help with parts ordering / maintenance.
     */
    @Column(nullable = true, length = 255)
    var manufacturer: String? = null,

    @Column(nullable = true, length = 255)
    var model: String? = null,

    @Column(name = "serial_number", nullable = true, length = 255)
    var serialNumber: String? = null,

    /**
     * Comma-separated tags for now (e.g. "network,server,critical").
     * We'll normalize later if needed.
     */
    @Column(nullable = true, length = 1024)
    var tags: String? = null,

    // Financial + lifecycle fields

    @Column(name = "purchase_date", nullable = true)
    var purchaseDate: LocalDate? = null,

    @Column(name = "purchase_cost", nullable = true, precision = 19, scale = 2)
    var purchaseCost: BigDecimal? = null,

    /**
     * Date the asset actually went into service.
     */
    @Column(name = "in_service_date", nullable = true)
    var inServiceDate: LocalDate? = null,

    @Column(name = "retired_date", nullable = true)
    var retiredDate: LocalDate? = null,

    @Column(name = "disposed_date", nullable = true)
    var disposedDate: LocalDate? = null,

    @Column(name = "warranty_expiry_date", nullable = true)
    var warrantyExpiryDate: LocalDate? = null,

    /**
     * Simple straight-line depreciation configuration.
     * This can be expanded to a richer model later.
     */
    @Column(name = "depreciation_years", nullable = true)
    var depreciationYears: Int? = null,

    @Column(name = "residual_value", nullable = true, precision = 19, scale = 2)
    var residualValue: BigDecimal? = null,

    // Relationships / placement

    /**
     * Link to Location hierarchy (site / building / room).
     */
    @Column(name = "location_id", nullable = true)
    var locationId: UUID? = null,

    /**
     * Link to property / real estate asset (for rentals, etc.).
     */
    @Column(name = "property_id", nullable = true)
    var propertyId: UUID? = null,

    /**
     * Link to a specific rentable unit (apartment, suite, etc.).
     */
    @Column(name = "unit_id", nullable = true)
    var unitId: UUID? = null,

    @Column(name = "assigned_user_id", nullable = true)
    var assignedUserId: UUID? = null,

    /**
     * External reference for integration with ERP, CMMS, etc.
     */
    @Column(name = "external_ref", nullable = true, length = 128)
    var externalRef: String? = null,

    /**
     * JSON string for flexible tenant-specific fields.
     * e.g. {"rack":"R12","u":"14","ipAddress":"10.0.0.5"}
     */
    @Lob
    @Column(name = "custom_fields_json", nullable = true)
    var customFieldsJson: String? = null

) : BaseEntity(), TenantScoped
