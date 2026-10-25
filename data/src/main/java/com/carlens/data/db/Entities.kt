package com.carlens.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/*
 * Room entities. They double as the versioned JSON backup records, so field names are part
 * of the backup format: add fields with defaults, never rename.
 * Dates are stored as epoch days, instants as epoch millis, enums as their names.
 */

@Serializable
@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val make: String,
    val model: String,
    val year: Int?,
    val plate: String?,
    val fuelType: String,
    val tankCapacityMl: Long?,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
@Entity(
    tableName = "odometer_readings",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId", "epochMillis")],
)
data class OdometerReadingEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val epochMillis: Long,
    val localDate: Long,
    val zoneId: String,
    val km: Long,
    val source: String,
    val startsNewSegment: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
@Entity(
    tableName = "fuel_entries",
    foreignKeys = [
        ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(OdometerReadingEntity::class, ["id"], ["odometerReadingId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("vehicleId", "epochMillis"), Index("odometerReadingId")],
)
data class FuelEntryEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val odometerReadingId: String,
    val epochMillis: Long,
    val localDate: Long,
    val zoneId: String,
    val volumeMl: Long,
    val unitPriceMilliSen: Long?,
    val tankStatus: String,
    val missedPrevious: Boolean,
    val station: String,
    val fuelType: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
@Entity(
    tableName = "service_records",
    foreignKeys = [
        ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(OdometerReadingEntity::class, ["id"], ["odometerReadingId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("vehicleId", "epochMillis"), Index("odometerReadingId")],
)
data class ServiceRecordEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val odometerReadingId: String?,
    val epochMillis: Long,
    val localDate: Long,
    val zoneId: String,
    val title: String,
    val workshop: String,
    val notes: String,
    val category: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
@Entity(
    tableName = "maintenance_rules",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId")],
)
data class MaintenanceRuleEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val task: String,
    val intervalMonths: Int?,
    val intervalKm: Long?,
    val startDate: Long,
    val startKm: Long?,
    val warnDays: Int,
    val warnKm: Long,
    val snoozedUntil: Long?,
    val active: Boolean,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
@Entity(
    tableName = "service_items",
    foreignKeys = [
        ForeignKey(ServiceRecordEntity::class, ["id"], ["serviceRecordId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MaintenanceRuleEntity::class, ["id"], ["maintenanceRuleId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("serviceRecordId"), Index("maintenanceRuleId")],
)
data class ServiceItemEntity(
    @PrimaryKey val id: String,
    val serviceRecordId: String,
    val position: Int,
    val task: String,
    val part: String,
    val quantity: Double?,
    val costSen: Long?,
    val maintenanceRuleId: String?,
)

@Serializable
@Entity(
    tableName = "documents",
    foreignKeys = [ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("vehicleId"), Index("expiryDate")],
)
data class DocumentEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val type: String,
    val title: String,
    val provider: String,
    val reference: String,
    val startDate: Long?,
    val expiryDate: Long?,
    val warnDays: Int,
    val expectedRenewalSen: Long?,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(FuelEntryEntity::class, ["id"], ["fuelEntryId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ServiceRecordEntity::class, ["id"], ["serviceRecordId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(DocumentEntity::class, ["id"], ["documentId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [
        Index("vehicleId", "localDate"),
        Index(value = ["fuelEntryId"], unique = true),
        Index(value = ["serviceRecordId"], unique = true),
        Index("documentId"),
    ],
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val epochMillis: Long,
    val localDate: Long,
    val zoneId: String,
    val amountSen: Long,
    val currency: String,
    val category: String,
    val merchant: String,
    val notes: String,
    val fuelEntryId: String?,
    val serviceRecordId: String?,
    val documentId: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
@Entity(
    tableName = "maintenance_completions",
    foreignKeys = [
        ForeignKey(VehicleEntity::class, ["id"], ["vehicleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(MaintenanceRuleEntity::class, ["id"], ["ruleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ServiceRecordEntity::class, ["id"], ["serviceRecordId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ServiceItemEntity::class, ["id"], ["serviceItemId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("vehicleId"), Index("ruleId"), Index("serviceRecordId"), Index("serviceItemId")],
)
data class MaintenanceCompletionEntity(
    @PrimaryKey val id: String,
    val vehicleId: String,
    val ruleId: String,
    val serviceRecordId: String,
    val serviceItemId: String?,
    val localDate: Long,
    val odometerKm: Long?,
)

@Serializable
@Entity(tableName = "attachments", indices = [Index("ownerType", "ownerId")])
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val ownerType: String,
    val ownerId: String,
    val relativePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
    val displayName: String,
    val createdAt: Long,
)

@Serializable
@Entity(tableName = "reminder_deliveries", indices = [Index("targetId")])
data class ReminderDeliveryEntity(
    @PrimaryKey val key: String,
    val targetType: String,
    val targetId: String,
    val dueCycle: String,
    val stage: String,
    val deliveredAt: Long,
)
