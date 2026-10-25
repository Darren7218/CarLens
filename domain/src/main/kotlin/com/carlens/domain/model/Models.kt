package com.carlens.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * When something happened, with the local calendar context captured at entry time.
 * Monthly reports group by [localDate] so records never move between months when the
 * phone changes time zone.
 */
data class EventTime(
    val epochMillis: Long,
    val localDate: LocalDate,
    val zoneId: String,
) {
    val localDateTime: LocalDateTime
        get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.of(zoneId))

    companion object {
        fun now(zone: ZoneId = ZoneId.systemDefault()): EventTime = of(LocalDateTime.now(zone), zone)

        fun of(local: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): EventTime =
            EventTime(local.atZone(zone).toInstant().toEpochMilli(), local.toLocalDate(), zone.id)

        /** Noon on a given date — used for date-only records. */
        fun ofDate(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): EventTime =
            of(date.atTime(12, 0), zone)
    }
}

data class Vehicle(
    val id: String,
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

enum class OdometerSource { FUEL, SERVICE, MANUAL }

data class OdometerReading(
    val id: String,
    val vehicleId: String,
    val time: EventTime,
    val km: Long,
    val source: OdometerSource,
    /** True when this reading starts a new continuity segment (e.g. odometer/cluster replaced). */
    val startsNewSegment: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class TankStatus { FULL, PARTIAL }

data class FuelEntry(
    val id: String,
    val vehicleId: String,
    val time: EventTime,
    val odometerReadingId: String,
    val odometerKm: Long,
    val startsNewSegment: Boolean,
    val volumeMl: Long,
    /** Price per litre in milli-sen (RM 2.05/L = 205_000). */
    val unitPriceMilliSen: Long?,
    val tankStatus: TankStatus,
    /** The fill-up before this one was not recorded; the economy chain restarts here. */
    val missedPrevious: Boolean,
    val station: String,
    val fuelType: String,
    val notes: String,
    /** Money lives in the linked expense (single source of truth). */
    val expenseId: String?,
    val totalSen: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class FuelEntryInput(
    val id: String? = null,
    val vehicleId: String,
    val time: EventTime,
    val odometerKm: Long,
    val startsNewSegment: Boolean = false,
    val volumeMl: Long,
    val totalSen: Long,
    val unitPriceMilliSen: Long?,
    val tankStatus: TankStatus,
    val missedPrevious: Boolean,
    val station: String,
    val fuelType: String,
    val notes: String,
)

enum class ExpenseCategory(val label: String, val isRunningCost: Boolean) {
    FUEL("Fuel", true),
    MAINTENANCE("Maintenance", true),
    REPAIR("Repair", true),
    TOLL("Tolls", true),
    PARKING("Parking", true),
    INSURANCE("Insurance", true),
    ROAD_TAX("Road tax", true),
    CLEANING("Cleaning", true),
    ACCESSORIES("Accessories", false),
    FINES("Fines", false),
    LOAN_PAYMENT("Loan payment", false),
    OTHER("Other", false),
}

data class Expense(
    val id: String,
    val vehicleId: String,
    val time: EventTime,
    val amountSen: Long,
    val currency: String = "MYR",
    val category: ExpenseCategory,
    val merchant: String,
    val notes: String,
    val fuelEntryId: String? = null,
    val serviceRecordId: String? = null,
    val documentId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
) {
    /** Linked expenses are edited through their fuel entry or service record. */
    val isLinked: Boolean get() = fuelEntryId != null || serviceRecordId != null
}

data class ServiceItem(
    val id: String,
    val serviceRecordId: String,
    val task: String,
    val part: String,
    val quantity: Double?,
    /** Optional allocation of the bill total to this item. */
    val costSen: Long?,
    val maintenanceRuleId: String?,
)

data class ServiceRecord(
    val id: String,
    val vehicleId: String,
    val time: EventTime,
    val odometerReadingId: String?,
    val odometerKm: Long?,
    val title: String,
    val workshop: String,
    val notes: String,
    val category: ExpenseCategory,
    val expenseId: String?,
    val totalSen: Long,
    val items: List<ServiceItem>,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val allocatedSen: Long get() = items.sumOf { it.costSen ?: 0L }
    val unallocatedSen: Long get() = totalSen - allocatedSen
}

data class ServiceItemInput(
    val id: String? = null,
    val task: String,
    val part: String,
    val quantity: Double?,
    val costSen: Long?,
    val maintenanceRuleId: String?,
)

data class ServiceRecordInput(
    val id: String? = null,
    val vehicleId: String,
    val time: EventTime,
    val odometerKm: Long?,
    val title: String,
    val workshop: String,
    val notes: String,
    val category: ExpenseCategory,
    val totalSen: Long,
    val items: List<ServiceItemInput>,
)

data class MaintenanceRule(
    val id: String,
    val vehicleId: String,
    val task: String,
    val intervalMonths: Int?,
    val intervalKm: Long?,
    /** Baseline used until the first recorded completion. */
    val startDate: LocalDate,
    val startKm: Long?,
    val warnDays: Int,
    val warnKm: Long,
    val snoozedUntil: LocalDate?,
    val active: Boolean,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class MaintenanceCompletion(
    val id: String,
    val ruleId: String,
    val serviceRecordId: String,
    val serviceItemId: String?,
    val date: LocalDate,
    val odometerKm: Long?,
)

enum class DocumentType(val label: String) {
    INSURANCE("Insurance"),
    ROAD_TAX("Road tax"),
    WARRANTY("Warranty"),
    DRIVING_LICENCE("Driving licence"),
    INSPECTION("Inspection"),
    REGISTRATION("Registration"),
    OTHER("Other"),
}

data class VehicleDocument(
    val id: String,
    val vehicleId: String,
    val type: DocumentType,
    val title: String,
    val provider: String,
    val reference: String,
    val startDate: LocalDate?,
    val expiryDate: LocalDate?,
    val warnDays: Int,
    val expectedRenewalSen: Long?,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class AttachmentOwner { FUEL, SERVICE, EXPENSE, DOCUMENT, VEHICLE }

data class Attachment(
    val id: String,
    val ownerType: AttachmentOwner,
    val ownerId: String,
    /** Path relative to the app's private attachments directory. */
    val relativePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
    val displayName: String,
    val createdAt: Long,
)

enum class ReminderTargetType { MAINTENANCE, DOCUMENT }

data class ReminderDelivery(
    val key: String,
    val targetType: ReminderTargetType,
    val targetId: String,
    val dueCycle: String,
    val stage: String,
    val deliveredAt: Long,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hidePlate: Boolean = false,
    val hideAmounts: Boolean = false,
    val biometricLock: Boolean = false,
    val hideNotificationDetails: Boolean = false,
    val monthlyBudgetSen: Long? = null,
    val selectedVehicleId: String? = null,
    val lastStation: String = "",
    val lastFuelType: String = "",
)
