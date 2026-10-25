package com.carlens.data.repository

import androidx.room.withTransaction
import com.carlens.data.db.CarLensDatabase
import com.carlens.data.db.ExpenseEntity
import com.carlens.data.db.FuelEntryEntity
import com.carlens.data.db.MaintenanceCompletionEntity
import com.carlens.data.db.OdometerReadingEntity
import com.carlens.data.db.ServiceItemEntity
import com.carlens.data.db.ServiceRecordEntity
import com.carlens.data.db.toDomain
import com.carlens.data.db.toEntity
import com.carlens.domain.model.EventTime
import com.carlens.domain.model.Expense
import com.carlens.domain.model.ExpenseCategory
import com.carlens.domain.model.FuelEntry
import com.carlens.domain.model.FuelEntryInput
import com.carlens.domain.model.MaintenanceCompletion
import com.carlens.domain.model.MaintenanceRule
import com.carlens.domain.model.OdometerReading
import com.carlens.domain.model.OdometerSource
import com.carlens.domain.model.ReminderDelivery
import com.carlens.domain.model.ServiceRecord
import com.carlens.domain.model.ServiceRecordInput
import com.carlens.domain.model.Vehicle
import com.carlens.domain.model.VehicleDocument
import com.carlens.domain.repository.DocumentRepository
import com.carlens.domain.repository.ExpenseRepository
import com.carlens.domain.repository.FuelRepository
import com.carlens.domain.repository.MaintenanceRepository
import com.carlens.domain.repository.OdometerRepository
import com.carlens.domain.repository.ReminderRepository
import com.carlens.domain.repository.ServiceRepository
import com.carlens.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleRepositoryImpl @Inject constructor(
    private val db: CarLensDatabase,
    private val clock: Clock
) : VehicleRepository {
    override fun observeVehicles(includeArchived: Boolean): Flow<List<Vehicle>> =
        db.vehicles().observeAll(includeArchived).map { list -> list.map { it.toDomain() } }

    override fun observeVehicle(id: String): Flow<Vehicle?> =
        db.vehicles().observe(id).map { it?.toDomain() }

    override suspend fun getVehicle(id: String): Vehicle? = db.vehicles().get(id)?.toDomain()

    override suspend fun getVehicles(): List<Vehicle> = db.vehicles().all().map { it.toDomain() }

    override suspend fun upsert(vehicle: Vehicle) {
        db.vehicles().upsert(vehicle.toEntity())
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        db.vehicles().setArchived(id, archived, clock.millis())
    }
}

@Singleton
class OdometerRepositoryImpl @Inject constructor(
    private val db: CarLensDatabase,
    private val clock: Clock
) : OdometerRepository {
    override fun observeReadings(vehicleId: String): Flow<List<OdometerReading>> =
        db.odometer().observe(vehicleId).map { list -> list.map { it.toDomain() } }

    override suspend fun getReadings(vehicleId: String): List<OdometerReading> =
        db.odometer().list(vehicleId).map { it.toDomain() }

    override suspend fun addManualReading(
        vehicleId: String,
        time: EventTime,
        km: Long,
        startsNewSegment: Boolean
    ): String {
        val id = UUID.randomUUID().toString()
        val now = clock.millis()
        val entity = OdometerReadingEntity(
            id = id,
            vehicleId = vehicleId,
            epochMillis = time.epochMillis,
            localDate = time.localDate.toEpochDay(),
            zoneId = time.zoneId,
            km = km,
            source = OdometerSource.MANUAL.name,
            startsNewSegment = startsNewSegment,
            createdAt = now,
            updatedAt = now
        )
        db.odometer().upsert(entity)
        return id
    }

    override suspend fun deleteManualReading(id: String) {
        db.odometer().delete(id)
    }
}

@Singleton
class FuelRepositoryImpl @Inject constructor(
    private val db: CarLensDatabase,
    private val clock: Clock
) : FuelRepository {

    override fun observeEntries(vehicleId: String): Flow<List<FuelEntry>> {
        return combine(
            db.fuel().observe(vehicleId),
            db.odometer().observe(vehicleId),
            db.expenses().observe(vehicleId)
        ) { entries, readings, expenses ->
            val readingMap = readings.associateBy { it.id }
            val expenseMap = expenses.filter { it.fuelEntryId != null }.associateBy { it.fuelEntryId!! }
            entries.map { entry ->
                entry.toDomain(readingMap[entry.odometerReadingId], expenseMap[entry.id])
            }
        }
    }

    override suspend fun getEntries(vehicleId: String): List<FuelEntry> {
        val entries = db.fuel().list(vehicleId)
        val readings = db.odometer().list(vehicleId).associateBy { it.id }
        val expenses = db.expenses().list(vehicleId).filter { it.fuelEntryId != null }.associateBy { it.fuelEntryId!! }
        return entries.map { entry ->
            entry.toDomain(readings[entry.odometerReadingId], expenses[entry.id])
        }
    }

    override suspend fun getEntry(id: String): FuelEntry? {
        val entry = db.fuel().get(id) ?: return null
        val reading = db.odometer().get(entry.odometerReadingId)
        val expense = db.expenses().forFuel(entry.id)
        return entry.toDomain(reading, expense)
    }

    override suspend fun save(input: FuelEntryInput): String = db.withTransaction {
        val entryId = input.id ?: UUID.randomUUID().toString()
        val existing = if (input.id != null) db.fuel().get(entryId) else null
        val readingId = existing?.odometerReadingId ?: UUID.randomUUID().toString()
        val existingReading = existing?.odometerReadingId?.let { db.odometer().get(it) }
        val now = clock.millis()

        // 1. Odometer reading
        val reading = OdometerReadingEntity(
            id = readingId,
            vehicleId = input.vehicleId,
            epochMillis = input.time.epochMillis,
            localDate = input.time.localDate.toEpochDay(),
            zoneId = input.time.zoneId,
            km = input.odometerKm,
            source = OdometerSource.FUEL.name,
            startsNewSegment = input.startsNewSegment,
            createdAt = existingReading?.createdAt ?: existing?.createdAt ?: now,
            updatedAt = now
        )
        db.odometer().upsert(reading)

        // 2. Fuel entry
        val fuelEntity = FuelEntryEntity(
            id = entryId,
            vehicleId = input.vehicleId,
            odometerReadingId = readingId,
            epochMillis = input.time.epochMillis,
            localDate = input.time.localDate.toEpochDay(),
            zoneId = input.time.zoneId,
            volumeMl = input.volumeMl,
            unitPriceMilliSen = input.unitPriceMilliSen,
            tankStatus = input.tankStatus.name,
            missedPrevious = input.missedPrevious,
            station = input.station,
            fuelType = input.fuelType,
            notes = input.notes,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        db.fuel().upsert(fuelEntity)

        // 3. Single source of money: Linked expense
        val existingExpense = if (input.id != null) db.expenses().forFuel(entryId) else null
        val expenseId = existingExpense?.id ?: UUID.randomUUID().toString()
        val expenseEntity = ExpenseEntity(
            id = expenseId,
            vehicleId = input.vehicleId,
            epochMillis = input.time.epochMillis,
            localDate = input.time.localDate.toEpochDay(),
            zoneId = input.time.zoneId,
            amountSen = input.totalSen,
            currency = "MYR",
            category = ExpenseCategory.FUEL.name,
            merchant = input.station,
            notes = input.notes,
            fuelEntryId = entryId,
            serviceRecordId = null,
            documentId = null,
            createdAt = existingExpense?.createdAt ?: now,
            updatedAt = now
        )
        db.expenses().upsert(expenseEntity)

        entryId
    }

    override suspend fun delete(id: String): FuelEntry? = db.withTransaction {
        val entry = getEntry(id) ?: return@withTransaction null
        db.fuel().delete(id)
        db.odometer().delete(entry.odometerReadingId)
        entry.expenseId?.let { db.expenses().delete(it) }
        entry
    }

    override suspend fun restore(entry: FuelEntry): Unit = db.withTransaction {
        val now = clock.millis()
        val createdAt = entry.createdAt.takeIf { it > 0L } ?: now
        val readingId = entry.odometerReadingId.ifBlank { UUID.randomUUID().toString() }
        val expenseId = entry.expenseId ?: UUID.randomUUID().toString()

        db.odometer().upsert(
            OdometerReadingEntity(
                id = readingId,
                vehicleId = entry.vehicleId,
                epochMillis = entry.time.epochMillis,
                localDate = entry.time.localDate.toEpochDay(),
                zoneId = entry.time.zoneId,
                km = entry.odometerKm,
                source = OdometerSource.FUEL.name,
                startsNewSegment = entry.startsNewSegment,
                createdAt = createdAt,
                updatedAt = now
            )
        )

        db.fuel().upsert(
            FuelEntryEntity(
                id = entry.id,
                vehicleId = entry.vehicleId,
                odometerReadingId = readingId,
                epochMillis = entry.time.epochMillis,
                localDate = entry.time.localDate.toEpochDay(),
                zoneId = entry.time.zoneId,
                volumeMl = entry.volumeMl,
                unitPriceMilliSen = entry.unitPriceMilliSen,
                tankStatus = entry.tankStatus.name,
                missedPrevious = entry.missedPrevious,
                station = entry.station,
                fuelType = entry.fuelType,
                notes = entry.notes,
                createdAt = createdAt,
                updatedAt = now
            )
        )

        db.expenses().upsert(
            ExpenseEntity(
                id = expenseId,
                vehicleId = entry.vehicleId,
                epochMillis = entry.time.epochMillis,
                localDate = entry.time.localDate.toEpochDay(),
                zoneId = entry.time.zoneId,
                amountSen = entry.totalSen ?: 0L,
                currency = "MYR",
                category = ExpenseCategory.FUEL.name,
                merchant = entry.station,
                notes = entry.notes,
                fuelEntryId = entry.id,
                serviceRecordId = null,
                documentId = null,
                createdAt = createdAt,
                updatedAt = now
            )
        )
    }

    override fun observeStations(): Flow<List<String>> = db.fuel().observeStations()
}

@Singleton
class ServiceRepositoryImpl @Inject constructor(
    private val db: CarLensDatabase,
    private val clock: Clock
) : ServiceRepository {

    override fun observeRecords(vehicleId: String): Flow<List<ServiceRecord>> {
        return combine(
            db.services().observe(vehicleId),
            db.odometer().observe(vehicleId),
            db.expenses().observe(vehicleId),
            db.services().observeItems(vehicleId)
        ) { records, readings, expenses, allItems ->
            val readingMap = readings.associateBy { it.id }
            val expenseMap = expenses.filter { it.serviceRecordId != null }.associateBy { it.serviceRecordId!! }
            val itemMap = allItems.groupBy { it.serviceRecordId }
            records.map { record ->
                record.toDomain(
                    reading = record.odometerReadingId?.let { readingMap[it] },
                    expense = expenseMap[record.id],
                    items = itemMap[record.id] ?: emptyList()
                )
            }
        }
    }

    override suspend fun getRecord(id: String): ServiceRecord? {
        val record = db.services().get(id) ?: return null
        val reading = record.odometerReadingId?.let { db.odometer().get(it) }
        val expense = db.expenses().forService(record.id)
        val items = db.services().items(record.id)
        return record.toDomain(reading, expense, items)
    }

    override suspend fun save(input: ServiceRecordInput): String = db.withTransaction {
        val recordId = input.id ?: UUID.randomUUID().toString()
        val existing = if (input.id != null) db.services().get(recordId) else null
        val existingReading = existing?.odometerReadingId?.let { db.odometer().get(it) }
        val now = clock.millis()

        // 1. Odometer reading (if provided)
        val odoKm = input.odometerKm
        val readingId = if (odoKm != null) {
            val rId = existing?.odometerReadingId ?: UUID.randomUUID().toString()
            val reading = OdometerReadingEntity(
                id = rId,
                vehicleId = input.vehicleId,
                epochMillis = input.time.epochMillis,
                localDate = input.time.localDate.toEpochDay(),
                zoneId = input.time.zoneId,
                km = odoKm,
                source = OdometerSource.SERVICE.name,
                startsNewSegment = existingReading?.startsNewSegment ?: false,
                createdAt = existingReading?.createdAt ?: existing?.createdAt ?: now,
                updatedAt = now
            )
            db.odometer().upsert(reading)
            rId
        } else {
            existing?.odometerReadingId?.let { db.odometer().delete(it) }
            null
        }

        // 2. Service record
        val recordEntity = ServiceRecordEntity(
            id = recordId,
            vehicleId = input.vehicleId,
            odometerReadingId = readingId,
            epochMillis = input.time.epochMillis,
            localDate = input.time.localDate.toEpochDay(),
            zoneId = input.time.zoneId,
            title = input.title,
            workshop = input.workshop,
            notes = input.notes,
            category = input.category.name,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        db.services().upsert(recordEntity)

        // 3. Clear existing items & completions for this record if update
        if (input.id != null) {
            db.services().deleteItems(recordId)
            db.maintenance().deleteCompletionsFor(recordId)
        }

        // 4. Save items & maintenance completions
        val itemEntities = mutableListOf<ServiceItemEntity>()
        val completions = mutableListOf<MaintenanceCompletionEntity>()

        input.items.forEachIndexed { index, itemInput ->
            val itemId = itemInput.id ?: UUID.randomUUID().toString()
            itemEntities += ServiceItemEntity(
                id = itemId,
                serviceRecordId = recordId,
                position = index,
                task = itemInput.task,
                part = itemInput.part,
                quantity = itemInput.quantity,
                costSen = itemInput.costSen,
                maintenanceRuleId = itemInput.maintenanceRuleId
            )
            val mRuleId = itemInput.maintenanceRuleId
            if (mRuleId != null) {
                completions += MaintenanceCompletionEntity(
                    id = UUID.randomUUID().toString(),
                    vehicleId = input.vehicleId,
                    ruleId = mRuleId,
                    serviceRecordId = recordId,
                    serviceItemId = itemId,
                    localDate = input.time.localDate.toEpochDay(),
                    odometerKm = odoKm
                )
            }
        }
        db.services().insertItems(itemEntities)
        if (completions.isNotEmpty()) {
            db.maintenance().insertCompletions(completions)
        }

        // 5. Linked expense
        val existingExpense = if (input.id != null) db.expenses().forService(recordId) else null
        val expenseId = existingExpense?.id ?: UUID.randomUUID().toString()
        val expenseEntity = ExpenseEntity(
            id = expenseId,
            vehicleId = input.vehicleId,
            epochMillis = input.time.epochMillis,
            localDate = input.time.localDate.toEpochDay(),
            zoneId = input.time.zoneId,
            amountSen = input.totalSen,
            currency = "MYR",
            category = input.category.name,
            merchant = input.workshop,
            notes = input.notes,
            fuelEntryId = null,
            serviceRecordId = recordId,
            documentId = null,
            createdAt = existingExpense?.createdAt ?: now,
            updatedAt = now
        )
        db.expenses().upsert(expenseEntity)

        recordId
    }

    override suspend fun delete(id: String): ServiceRecord? = db.withTransaction {
        val record = getRecord(id) ?: return@withTransaction null
        db.services().delete(id)
        db.services().deleteItems(id)
        db.maintenance().deleteCompletionsFor(id)
        record.odometerReadingId?.let { db.odometer().delete(it) }
        record.expenseId?.let { db.expenses().delete(it) }
        record
    }

    override suspend fun restore(record: ServiceRecord): Unit = db.withTransaction {
        val now = clock.millis()
        val createdAt = record.createdAt.takeIf { it > 0L } ?: now
        val odoKm = record.odometerKm
        val readingId = if (odoKm != null) {
            val rId = record.odometerReadingId ?: UUID.randomUUID().toString()
            db.odometer().upsert(
                OdometerReadingEntity(
                    id = rId,
                    vehicleId = record.vehicleId,
                    epochMillis = record.time.epochMillis,
                    localDate = record.time.localDate.toEpochDay(),
                    zoneId = record.time.zoneId,
                    km = odoKm,
                    source = OdometerSource.SERVICE.name,
                    startsNewSegment = false,
                    createdAt = createdAt,
                    updatedAt = now
                )
            )
            rId
        } else null

        db.services().upsert(
            ServiceRecordEntity(
                id = record.id,
                vehicleId = record.vehicleId,
                odometerReadingId = readingId,
                epochMillis = record.time.epochMillis,
                localDate = record.time.localDate.toEpochDay(),
                zoneId = record.time.zoneId,
                title = record.title,
                workshop = record.workshop,
                notes = record.notes,
                category = record.category.name,
                createdAt = createdAt,
                updatedAt = now
            )
        )

        db.services().deleteItems(record.id)
        db.maintenance().deleteCompletionsFor(record.id)

        val itemEntities = mutableListOf<ServiceItemEntity>()
        val completions = mutableListOf<MaintenanceCompletionEntity>()
        record.items.forEachIndexed { index, item ->
            itemEntities += ServiceItemEntity(
                id = item.id,
                serviceRecordId = record.id,
                position = index,
                task = item.task,
                part = item.part,
                quantity = item.quantity,
                costSen = item.costSen,
                maintenanceRuleId = item.maintenanceRuleId
            )
            val mRuleId = item.maintenanceRuleId
            if (mRuleId != null) {
                completions += MaintenanceCompletionEntity(
                    id = UUID.randomUUID().toString(),
                    vehicleId = record.vehicleId,
                    ruleId = mRuleId,
                    serviceRecordId = record.id,
                    serviceItemId = item.id,
                    localDate = record.time.localDate.toEpochDay(),
                    odometerKm = odoKm
                )
            }
        }
        db.services().insertItems(itemEntities)
        if (completions.isNotEmpty()) {
            db.maintenance().insertCompletions(completions)
        }

        val expenseId = record.expenseId ?: UUID.randomUUID().toString()
        db.expenses().upsert(
            ExpenseEntity(
                id = expenseId,
                vehicleId = record.vehicleId,
                epochMillis = record.time.epochMillis,
                localDate = record.time.localDate.toEpochDay(),
                zoneId = record.time.zoneId,
                amountSen = record.totalSen,
                currency = "MYR",
                category = record.category.name,
                merchant = record.workshop,
                notes = record.notes,
                fuelEntryId = null,
                serviceRecordId = record.id,
                documentId = null,
                createdAt = createdAt,
                updatedAt = now
            )
        )
    }

    override fun observeWorkshops(): Flow<List<String>> = db.services().observeWorkshops()
    override fun observeCommonTasks(): Flow<List<String>> = db.services().observeTasks()
}

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val db: CarLensDatabase
) : ExpenseRepository {

    override fun observeExpenses(vehicleId: String): Flow<List<Expense>> =
        db.expenses().observe(vehicleId).map { list -> list.map { it.toDomain() } }

    override fun observeAllExpenses(): Flow<List<Expense>> =
        db.expenses().observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getExpenses(vehicleId: String): List<Expense> =
        db.expenses().list(vehicleId).map { it.toDomain() }

    override suspend fun getExpense(id: String): Expense? =
        db.expenses().get(id)?.toDomain()

    override suspend fun save(expense: Expense) {
        db.expenses().upsert(expense.toEntity())
    }

    override suspend fun delete(id: String): Expense? = db.withTransaction {
        val e = getExpense(id) ?: return@withTransaction null
        db.expenses().delete(id)
        e
    }

    override suspend fun restore(expense: Expense) {
        save(expense)
    }

    override fun observeMerchants(): Flow<List<String>> = db.expenses().observeMerchants()
}

@Singleton
class MaintenanceRepositoryImpl @Inject constructor(
    private val db: CarLensDatabase,
    private val clock: Clock
) : MaintenanceRepository {

    override fun observeRules(vehicleId: String): Flow<List<MaintenanceRule>> =
        db.maintenance().observeRules(vehicleId).map { list -> list.map { it.toDomain() } }

    override suspend fun getRules(vehicleId: String): List<MaintenanceRule> =
        db.maintenance().rules(vehicleId).map { it.toDomain() }

    override suspend fun getRule(id: String): MaintenanceRule? =
        db.maintenance().rule(id)?.toDomain()

    override suspend fun saveRule(rule: MaintenanceRule) {
        db.maintenance().upsertRule(rule.toEntity())
    }

    override suspend fun deleteRule(id: String) {
        db.maintenance().deleteRule(id)
    }

    override suspend fun snooze(ruleId: String, until: LocalDate?) {
        db.maintenance().snooze(ruleId, until?.toEpochDay(), clock.millis())
    }

    override fun observeCompletions(vehicleId: String): Flow<List<MaintenanceCompletion>> =
        db.maintenance().observeCompletions(vehicleId).map { list -> list.map { it.toDomain() } }

    override suspend fun getCompletions(vehicleId: String): List<MaintenanceCompletion> =
        db.maintenance().completions(vehicleId).map { it.toDomain() }
}

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val db: CarLensDatabase
) : DocumentRepository {

    override fun observeDocuments(vehicleId: String): Flow<List<VehicleDocument>> =
        db.documents().observe(vehicleId).map { list -> list.map { it.toDomain() } }

    override suspend fun getDocuments(vehicleId: String): List<VehicleDocument> =
        db.documents().list(vehicleId).map { it.toDomain() }

    override suspend fun getDocument(id: String): VehicleDocument? =
        db.documents().get(id)?.toDomain()

    override suspend fun save(doc: VehicleDocument) {
        db.documents().upsert(doc.toEntity())
    }

    override suspend fun delete(id: String) {
        db.documents().delete(id)
    }
}

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val db: CarLensDatabase
) : ReminderRepository {

    override suspend fun wasDelivered(key: String): Boolean =
        db.reminders().count(key) > 0

    override suspend fun recordDelivery(delivery: ReminderDelivery) {
        db.reminders().upsert(delivery.toEntity())
    }
}
