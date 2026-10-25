package com.carlens.domain.repository

import com.carlens.domain.model.AppSettings
import com.carlens.domain.model.Attachment
import com.carlens.domain.model.AttachmentOwner
import com.carlens.domain.model.EventTime
import com.carlens.domain.model.Expense
import com.carlens.domain.model.FuelEntry
import com.carlens.domain.model.FuelEntryInput
import com.carlens.domain.model.MaintenanceCompletion
import com.carlens.domain.model.MaintenanceRule
import com.carlens.domain.model.OdometerReading
import com.carlens.domain.model.ReminderDelivery
import com.carlens.domain.model.ServiceRecord
import com.carlens.domain.model.ServiceRecordInput
import com.carlens.domain.model.ThemeMode
import com.carlens.domain.model.Vehicle
import com.carlens.domain.model.VehicleDocument
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.time.LocalDate

interface VehicleRepository {
    fun observeVehicles(includeArchived: Boolean = false): Flow<List<Vehicle>>
    fun observeVehicle(id: String): Flow<Vehicle?>
    suspend fun getVehicle(id: String): Vehicle?
    suspend fun getVehicles(): List<Vehicle>
    suspend fun upsert(vehicle: Vehicle)
    suspend fun setArchived(id: String, archived: Boolean)
}

interface OdometerRepository {
    fun observeReadings(vehicleId: String): Flow<List<OdometerReading>>
    suspend fun getReadings(vehicleId: String): List<OdometerReading>
    suspend fun addManualReading(vehicleId: String, time: EventTime, km: Long, startsNewSegment: Boolean): String
    suspend fun deleteManualReading(id: String)
}

interface FuelRepository {
    fun observeEntries(vehicleId: String): Flow<List<FuelEntry>>
    suspend fun getEntries(vehicleId: String): List<FuelEntry>
    suspend fun getEntry(id: String): FuelEntry?
    /** Writes reading + fuel entry + linked expense in one transaction. */
    suspend fun save(input: FuelEntryInput): String
    /** Deletes and returns a snapshot for undo. */
    suspend fun delete(id: String): FuelEntry?
    suspend fun restore(entry: FuelEntry)
    fun observeStations(): Flow<List<String>>
}

interface ServiceRepository {
    fun observeRecords(vehicleId: String): Flow<List<ServiceRecord>>
    suspend fun getRecord(id: String): ServiceRecord?
    /** Writes record, items, linked expense, odometer reading and rule completions in one transaction. */
    suspend fun save(input: ServiceRecordInput): String
    suspend fun delete(id: String): ServiceRecord?
    suspend fun restore(record: ServiceRecord)
    fun observeWorkshops(): Flow<List<String>>
    fun observeCommonTasks(): Flow<List<String>>
}

interface ExpenseRepository {
    fun observeExpenses(vehicleId: String): Flow<List<Expense>>
    fun observeAllExpenses(): Flow<List<Expense>>
    suspend fun getExpenses(vehicleId: String): List<Expense>
    suspend fun getExpense(id: String): Expense?
    /** Only for standalone expenses; linked ones are written by fuel/service repositories. */
    suspend fun save(expense: Expense)
    suspend fun delete(id: String): Expense?
    suspend fun restore(expense: Expense)
    fun observeMerchants(): Flow<List<String>>
}

interface MaintenanceRepository {
    fun observeRules(vehicleId: String): Flow<List<MaintenanceRule>>
    suspend fun getRules(vehicleId: String): List<MaintenanceRule>
    suspend fun getRule(id: String): MaintenanceRule?
    suspend fun saveRule(rule: MaintenanceRule)
    suspend fun deleteRule(id: String)
    suspend fun snooze(ruleId: String, until: LocalDate?)
    fun observeCompletions(vehicleId: String): Flow<List<MaintenanceCompletion>>
    suspend fun getCompletions(vehicleId: String): List<MaintenanceCompletion>
}

interface DocumentRepository {
    fun observeDocuments(vehicleId: String): Flow<List<VehicleDocument>>
    suspend fun getDocuments(vehicleId: String): List<VehicleDocument>
    suspend fun getDocument(id: String): VehicleDocument?
    suspend fun save(doc: VehicleDocument)
    suspend fun delete(id: String)
}

interface AttachmentRepository {
    fun observe(ownerType: AttachmentOwner, ownerId: String): Flow<List<Attachment>>
    /** Copies the content into app-private storage. [sourceUri] is a content:// URI string. */
    suspend fun import(ownerType: AttachmentOwner, ownerId: String, sourceUri: String): Attachment
    /** Moves attachments created against a temporary owner (unsaved form) to the saved record. */
    suspend fun reassign(fromOwnerId: String, toOwnerId: String)
    suspend fun delete(id: String)
    fun fileFor(attachment: Attachment): File
    /** Creates an empty private file for the camera to write into; returns its absolute path. */
    suspend fun newCameraFile(): File
    suspend fun importLocalFile(ownerType: AttachmentOwner, ownerId: String, file: File, mimeType: String): Attachment
    /** Number of attachments keyed by owner id. */
    fun observeCounts(): Flow<Map<String, Int>>
    /** Removes attachments whose owner record no longer exists and that are older than [olderThanMillis]. */
    suspend fun pruneOrphans(olderThanMillis: Long): Int
}

interface ReminderRepository {
    suspend fun wasDelivered(key: String): Boolean
    suspend fun recordDelivery(delivery: ReminderDelivery)
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun current(): AppSettings
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setHidePlate(v: Boolean)
    suspend fun setHideAmounts(v: Boolean)
    suspend fun setBiometricLock(v: Boolean)
    suspend fun setHideNotificationDetails(v: Boolean)
    suspend fun setMonthlyBudgetSen(v: Long?)
    suspend fun setSelectedVehicle(id: String?)
    suspend fun rememberFuelChoice(station: String, fuelType: String)
    /** Unfinished form drafts, keyed by form name; JSON is opaque to the store. */
    suspend fun saveDraft(key: String, json: String?)
    suspend fun loadDraft(key: String): String?
}

data class BackupPreview(
    val token: String,
    val createdAt: Long,
    val formatVersion: Int,
    val schemaVersion: Int,
    val encrypted: Boolean,
    val vehicleNames: List<String>,
    val counts: Map<String, Int>,
    val attachmentCount: Int,
    val attachmentBytes: Long,
)

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)
class PasswordRequiredException : Exception("This backup is password protected")

interface BackupRepository {
    suspend fun export(destinationUri: String, password: CharArray?): Map<String, Int>
    /** Validates the archive fully and stages it; nothing is changed yet. */
    suspend fun inspect(sourceUri: String, password: CharArray?): BackupPreview
    /** Creates a recovery backup of current data, then replaces all data transactionally. */
    suspend fun restore(token: String)
    suspend fun discardStaged(token: String)
    suspend fun exportFuelCsv(vehicleId: String, destinationUri: String)
    suspend fun exportExpensesCsv(vehicleId: String, destinationUri: String)
    suspend fun latestRecoveryBackup(): File?
}
