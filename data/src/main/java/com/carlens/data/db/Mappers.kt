package com.carlens.data.db

import com.carlens.domain.model.Attachment
import com.carlens.domain.model.AttachmentOwner
import com.carlens.domain.model.DocumentType
import com.carlens.domain.model.EventTime
import com.carlens.domain.model.Expense
import com.carlens.domain.model.ExpenseCategory
import com.carlens.domain.model.FuelEntry
import com.carlens.domain.model.MaintenanceCompletion
import com.carlens.domain.model.MaintenanceRule
import com.carlens.domain.model.OdometerReading
import com.carlens.domain.model.OdometerSource
import com.carlens.domain.model.ReminderDelivery
import com.carlens.domain.model.ReminderTargetType
import com.carlens.domain.model.ServiceItem
import com.carlens.domain.model.ServiceRecord
import com.carlens.domain.model.TankStatus
import com.carlens.domain.model.Vehicle
import com.carlens.domain.model.VehicleDocument
import java.time.LocalDate

fun VehicleEntity.toDomain() = Vehicle(
    id = id,
    name = name,
    make = make,
    model = model,
    year = year,
    plate = plate,
    fuelType = fuelType,
    tankCapacityMl = tankCapacityMl,
    archived = archived,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Vehicle.toEntity() = VehicleEntity(
    id = id,
    name = name,
    make = make,
    model = model,
    year = year,
    plate = plate,
    fuelType = fuelType,
    tankCapacityMl = tankCapacityMl,
    archived = archived,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun OdometerReadingEntity.toDomain() = OdometerReading(
    id = id,
    vehicleId = vehicleId,
    time = EventTime(epochMillis, LocalDate.ofEpochDay(localDate), zoneId),
    km = km,
    source = OdometerSource.valueOf(source),
    startsNewSegment = startsNewSegment,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun OdometerReading.toEntity() = OdometerReadingEntity(
    id = id,
    vehicleId = vehicleId,
    epochMillis = time.epochMillis,
    localDate = time.localDate.toEpochDay(),
    zoneId = time.zoneId,
    km = km,
    source = source.name,
    startsNewSegment = startsNewSegment,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun FuelEntryEntity.toDomain(reading: OdometerReadingEntity?, expense: ExpenseEntity?) = FuelEntry(
    id = id,
    vehicleId = vehicleId,
    time = EventTime(epochMillis, LocalDate.ofEpochDay(localDate), zoneId),
    odometerReadingId = odometerReadingId,
    odometerKm = reading?.km ?: 0L,
    startsNewSegment = reading?.startsNewSegment ?: false,
    volumeMl = volumeMl,
    unitPriceMilliSen = unitPriceMilliSen,
    tankStatus = TankStatus.valueOf(tankStatus),
    missedPrevious = missedPrevious,
    station = station,
    fuelType = fuelType,
    notes = notes,
    expenseId = expense?.id,
    totalSen = expense?.amountSen,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ServiceRecordEntity.toDomain(
    reading: OdometerReadingEntity?,
    expense: ExpenseEntity?,
    items: List<ServiceItemEntity>
) = ServiceRecord(
    id = id,
    vehicleId = vehicleId,
    time = EventTime(epochMillis, LocalDate.ofEpochDay(localDate), zoneId),
    odometerReadingId = odometerReadingId,
    odometerKm = reading?.km,
    title = title,
    workshop = workshop,
    notes = notes,
    category = ExpenseCategory.valueOf(category),
    expenseId = expense?.id,
    totalSen = expense?.amountSen ?: 0L,
    items = items.map { it.toDomain() },
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ServiceItemEntity.toDomain() = ServiceItem(
    id = id,
    serviceRecordId = serviceRecordId,
    task = task,
    part = part,
    quantity = quantity,
    costSen = costSen,
    maintenanceRuleId = maintenanceRuleId
)

fun MaintenanceRuleEntity.toDomain() = MaintenanceRule(
    id = id,
    vehicleId = vehicleId,
    task = task,
    intervalMonths = intervalMonths,
    intervalKm = intervalKm,
    startDate = LocalDate.ofEpochDay(startDate),
    startKm = startKm,
    warnDays = warnDays,
    warnKm = warnKm,
    snoozedUntil = snoozedUntil?.let { LocalDate.ofEpochDay(it) },
    active = active,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MaintenanceRule.toEntity() = MaintenanceRuleEntity(
    id = id,
    vehicleId = vehicleId,
    task = task,
    intervalMonths = intervalMonths,
    intervalKm = intervalKm,
    startDate = startDate.toEpochDay(),
    startKm = startKm,
    warnDays = warnDays,
    warnKm = warnKm,
    snoozedUntil = snoozedUntil?.toEpochDay(),
    active = active,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MaintenanceCompletionEntity.toDomain() = MaintenanceCompletion(
    id = id,
    ruleId = ruleId,
    serviceRecordId = serviceRecordId,
    serviceItemId = serviceItemId,
    date = LocalDate.ofEpochDay(localDate),
    odometerKm = odometerKm
)

fun DocumentEntity.toDomain() = VehicleDocument(
    id = id,
    vehicleId = vehicleId,
    type = DocumentType.valueOf(type),
    title = title,
    provider = provider,
    reference = reference,
    startDate = startDate?.let { LocalDate.ofEpochDay(it) },
    expiryDate = expiryDate?.let { LocalDate.ofEpochDay(it) },
    warnDays = warnDays,
    expectedRenewalSen = expectedRenewalSen,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun VehicleDocument.toEntity() = DocumentEntity(
    id = id,
    vehicleId = vehicleId,
    type = type.name,
    title = title,
    provider = provider,
    reference = reference,
    startDate = startDate?.toEpochDay(),
    expiryDate = expiryDate?.toEpochDay(),
    warnDays = warnDays,
    expectedRenewalSen = expectedRenewalSen,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ExpenseEntity.toDomain() = Expense(
    id = id,
    vehicleId = vehicleId,
    time = EventTime(epochMillis, LocalDate.ofEpochDay(localDate), zoneId),
    amountSen = amountSen,
    currency = currency,
    category = ExpenseCategory.valueOf(category),
    merchant = merchant,
    notes = notes,
    fuelEntryId = fuelEntryId,
    serviceRecordId = serviceRecordId,
    documentId = documentId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Expense.toEntity() = ExpenseEntity(
    id = id,
    vehicleId = vehicleId,
    epochMillis = time.epochMillis,
    localDate = time.localDate.toEpochDay(),
    zoneId = time.zoneId,
    amountSen = amountSen,
    currency = currency,
    category = category.name,
    merchant = merchant,
    notes = notes,
    fuelEntryId = fuelEntryId,
    serviceRecordId = serviceRecordId,
    documentId = documentId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun AttachmentEntity.toDomain() = Attachment(
    id = id,
    ownerType = AttachmentOwner.valueOf(ownerType),
    ownerId = ownerId,
    relativePath = relativePath,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    sha256 = sha256,
    displayName = displayName,
    createdAt = createdAt
)

fun Attachment.toEntity() = AttachmentEntity(
    id = id,
    ownerType = ownerType.name,
    ownerId = ownerId,
    relativePath = relativePath,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    sha256 = sha256,
    displayName = displayName,
    createdAt = createdAt
)

fun ReminderDeliveryEntity.toDomain() = ReminderDelivery(
    key = key,
    targetType = ReminderTargetType.valueOf(targetType),
    targetId = targetId,
    dueCycle = dueCycle,
    stage = stage,
    deliveredAt = deliveredAt
)

fun ReminderDelivery.toEntity() = ReminderDeliveryEntity(
    key = key,
    targetType = targetType.name,
    targetId = targetId,
    dueCycle = dueCycle,
    stage = stage,
    deliveredAt = deliveredAt
)
