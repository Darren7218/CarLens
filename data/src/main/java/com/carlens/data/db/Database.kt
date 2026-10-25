package com.carlens.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles WHERE archived = 0 OR :includeArchived ORDER BY createdAt")
    fun observeAll(includeArchived: Boolean): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun observe(id: String): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun get(id: String): VehicleEntity?

    @Query("SELECT * FROM vehicles ORDER BY createdAt")
    suspend fun all(): List<VehicleEntity>

    @Upsert suspend fun upsert(v: VehicleEntity)

    @Query("UPDATE vehicles SET archived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, now: Long)
}

@Dao
interface OdometerDao {
    @Query("SELECT * FROM odometer_readings WHERE vehicleId = :vehicleId ORDER BY epochMillis")
    fun observe(vehicleId: String): Flow<List<OdometerReadingEntity>>

    @Query("SELECT * FROM odometer_readings WHERE vehicleId = :vehicleId ORDER BY epochMillis")
    suspend fun list(vehicleId: String): List<OdometerReadingEntity>

    @Query("SELECT * FROM odometer_readings WHERE id = :id")
    suspend fun get(id: String): OdometerReadingEntity?

    @Upsert suspend fun upsert(r: OdometerReadingEntity)

    @Query("DELETE FROM odometer_readings WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM odometer_readings") suspend fun all(): List<OdometerReadingEntity>
}

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY epochMillis DESC")
    fun observe(vehicleId: String): Flow<List<FuelEntryEntity>>

    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY epochMillis DESC")
    suspend fun list(vehicleId: String): List<FuelEntryEntity>

    @Query("SELECT * FROM fuel_entries WHERE id = :id")
    suspend fun get(id: String): FuelEntryEntity?

    @Upsert suspend fun upsert(f: FuelEntryEntity)

    @Query("DELETE FROM fuel_entries WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT station FROM fuel_entries WHERE station != '' GROUP BY station ORDER BY MAX(epochMillis) DESC LIMIT 30")
    fun observeStations(): Flow<List<String>>

    @Query("SELECT * FROM fuel_entries") suspend fun all(): List<FuelEntryEntity>
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM service_records WHERE vehicleId = :vehicleId ORDER BY epochMillis DESC")
    fun observe(vehicleId: String): Flow<List<ServiceRecordEntity>>

    @Query("SELECT * FROM service_records WHERE id = :id")
    suspend fun get(id: String): ServiceRecordEntity?

    @Upsert suspend fun upsert(s: ServiceRecordEntity)

    @Query("DELETE FROM service_records WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT i.* FROM service_items i JOIN service_records s ON s.id = i.serviceRecordId WHERE s.vehicleId = :vehicleId ORDER BY i.position")
    fun observeItems(vehicleId: String): Flow<List<ServiceItemEntity>>

    @Query("SELECT * FROM service_items WHERE serviceRecordId = :recordId ORDER BY position")
    suspend fun items(recordId: String): List<ServiceItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertItems(items: List<ServiceItemEntity>)

    @Query("DELETE FROM service_items WHERE serviceRecordId = :recordId")
    suspend fun deleteItems(recordId: String)

    @Query("SELECT workshop FROM service_records WHERE workshop != '' GROUP BY workshop ORDER BY COUNT(*) DESC, MAX(epochMillis) DESC LIMIT 30")
    fun observeWorkshops(): Flow<List<String>>

    @Query("SELECT task FROM service_items WHERE task != '' GROUP BY task ORDER BY COUNT(*) DESC LIMIT 40")
    fun observeTasks(): Flow<List<String>>

    @Query("SELECT * FROM service_records") suspend fun all(): List<ServiceRecordEntity>
    @Query("SELECT * FROM service_items") suspend fun allItems(): List<ServiceItemEntity>
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId ORDER BY epochMillis DESC")
    fun observe(vehicleId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY epochMillis DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId ORDER BY epochMillis DESC")
    suspend fun list(vehicleId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun get(id: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE fuelEntryId = :fuelId")
    suspend fun forFuel(fuelId: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE serviceRecordId = :recordId")
    suspend fun forService(recordId: String): ExpenseEntity?

    @Upsert suspend fun upsert(e: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT merchant FROM expenses WHERE merchant != '' AND fuelEntryId IS NULL AND serviceRecordId IS NULL GROUP BY merchant ORDER BY MAX(epochMillis) DESC LIMIT 30")
    fun observeMerchants(): Flow<List<String>>

    @Query("SELECT * FROM expenses") suspend fun all(): List<ExpenseEntity>
}

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_rules WHERE vehicleId = :vehicleId ORDER BY task")
    fun observeRules(vehicleId: String): Flow<List<MaintenanceRuleEntity>>

    @Query("SELECT * FROM maintenance_rules WHERE vehicleId = :vehicleId ORDER BY task")
    suspend fun rules(vehicleId: String): List<MaintenanceRuleEntity>

    @Query("SELECT * FROM maintenance_rules WHERE id = :id")
    suspend fun rule(id: String): MaintenanceRuleEntity?

    @Upsert suspend fun upsertRule(r: MaintenanceRuleEntity)

    @Query("DELETE FROM maintenance_rules WHERE id = :id")
    suspend fun deleteRule(id: String)

    @Query("UPDATE maintenance_rules SET snoozedUntil = :until, updatedAt = :now WHERE id = :id")
    suspend fun snooze(id: String, until: Long?, now: Long)

    @Query("SELECT * FROM maintenance_completions WHERE vehicleId = :vehicleId")
    fun observeCompletions(vehicleId: String): Flow<List<MaintenanceCompletionEntity>>

    @Query("SELECT * FROM maintenance_completions WHERE vehicleId = :vehicleId")
    suspend fun completions(vehicleId: String): List<MaintenanceCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCompletions(c: List<MaintenanceCompletionEntity>)

    @Query("DELETE FROM maintenance_completions WHERE serviceRecordId = :recordId")
    suspend fun deleteCompletionsFor(recordId: String)

    @Query("SELECT * FROM maintenance_rules") suspend fun allRules(): List<MaintenanceRuleEntity>
    @Query("SELECT * FROM maintenance_completions") suspend fun allCompletions(): List<MaintenanceCompletionEntity>
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE vehicleId = :vehicleId ORDER BY expiryDate IS NULL, expiryDate")
    fun observe(vehicleId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE vehicleId = :vehicleId")
    suspend fun list(vehicleId: String): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun get(id: String): DocumentEntity?

    @Upsert suspend fun upsert(d: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM documents") suspend fun all(): List<DocumentEntity>
}

data class AttachmentCountRow(
    val ownerId: String,
    val count: Int
)

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE ownerType = :ownerType AND ownerId = :ownerId ORDER BY createdAt")
    fun observe(ownerType: String, ownerId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE ownerId = :ownerId")
    suspend fun forOwner(ownerId: String): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun get(id: String): AttachmentEntity?

    @Upsert suspend fun upsert(a: AttachmentEntity)

    @Query("UPDATE attachments SET ownerId = :to WHERE ownerId = :from")
    suspend fun reassign(from: String, to: String)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM attachments") suspend fun all(): List<AttachmentEntity>

    @Query("SELECT * FROM attachments") fun observeAll(): Flow<List<AttachmentEntity>>

    @Query("SELECT ownerId, COUNT(*) AS count FROM attachments GROUP BY ownerId")
    fun observeCountsByOwner(): Flow<List<AttachmentCountRow>>
}

@Dao
interface ReminderDao {
    @Query("SELECT COUNT(*) FROM reminder_deliveries WHERE `key` = :key")
    suspend fun count(key: String): Int

    @Upsert suspend fun upsert(d: ReminderDeliveryEntity)

    @Query("SELECT * FROM reminder_deliveries") suspend fun all(): List<ReminderDeliveryEntity>
}

@Dao
interface BulkDao {
    @Query("DELETE FROM vehicles") suspend fun clearVehicles()
    @Query("DELETE FROM attachments") suspend fun clearAttachments()
    @Query("DELETE FROM reminder_deliveries") suspend fun clearDeliveries()

    @Insert suspend fun insertVehicles(v: List<VehicleEntity>)
    @Insert suspend fun insertReadings(v: List<OdometerReadingEntity>)
    @Insert suspend fun insertRules(v: List<MaintenanceRuleEntity>)
    @Insert suspend fun insertFuel(v: List<FuelEntryEntity>)
    @Insert suspend fun insertServices(v: List<ServiceRecordEntity>)
    @Insert suspend fun insertItems(v: List<ServiceItemEntity>)
    @Insert suspend fun insertDocuments(v: List<DocumentEntity>)
    @Insert suspend fun insertExpenses(v: List<ExpenseEntity>)
    @Insert suspend fun insertCompletions(v: List<MaintenanceCompletionEntity>)
    @Insert suspend fun insertAttachments(v: List<AttachmentEntity>)
    @Insert suspend fun insertDeliveries(v: List<ReminderDeliveryEntity>)
}

@Database(
    entities = [
        VehicleEntity::class, OdometerReadingEntity::class, FuelEntryEntity::class,
        ServiceRecordEntity::class, MaintenanceRuleEntity::class, ServiceItemEntity::class,
        DocumentEntity::class, ExpenseEntity::class, MaintenanceCompletionEntity::class,
        AttachmentEntity::class, ReminderDeliveryEntity::class,
    ],
    version = CarLensDatabase.SCHEMA_VERSION,
    exportSchema = true,
)
abstract class CarLensDatabase : RoomDatabase() {
    abstract fun vehicles(): VehicleDao
    abstract fun odometer(): OdometerDao
    abstract fun fuel(): FuelDao
    abstract fun services(): ServiceDao
    abstract fun expenses(): ExpenseDao
    abstract fun maintenance(): MaintenanceDao
    abstract fun documents(): DocumentDao
    abstract fun attachments(): AttachmentDao
    abstract fun reminders(): ReminderDao
    abstract fun bulk(): BulkDao

    companion object {
        /**
         * Bump with an explicit Migration. Destructive fallback is never enabled, so
         * records are always preserved across upgrades.
         */
        const val SCHEMA_VERSION = 1
        const val NAME = "carlens.db"
    }
}
