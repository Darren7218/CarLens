package com.carlens.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.carlens.data.db.AttachmentEntity
import com.carlens.data.db.CarLensDatabase
import com.carlens.data.db.DocumentEntity
import com.carlens.data.db.ExpenseEntity
import com.carlens.data.db.FuelEntryEntity
import com.carlens.data.db.MaintenanceCompletionEntity
import com.carlens.data.db.MaintenanceRuleEntity
import com.carlens.data.db.OdometerReadingEntity
import com.carlens.data.db.ReminderDeliveryEntity
import com.carlens.data.db.ServiceItemEntity
import com.carlens.data.db.ServiceRecordEntity
import com.carlens.data.db.VehicleEntity
import com.carlens.domain.calc.Units
import com.carlens.domain.repository.BackupException
import com.carlens.domain.repository.BackupPreview
import com.carlens.domain.repository.BackupRepository
import com.carlens.domain.repository.PasswordRequiredException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDate
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupManifest(
    val formatVersion: Int = 1,
    val schemaVersion: Int = CarLensDatabase.SCHEMA_VERSION,
    val createdAt: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = false,
    val vehicles: List<String> = emptyList(),
    val recordCounts: Map<String, Int> = emptyMap(),
    val fileChecksums: Map<String, String> = emptyMap()
)

@Serializable
data class DatabaseDump(
    val vehicles: List<VehicleEntity>,
    val odometerReadings: List<OdometerReadingEntity>,
    val fuelEntries: List<FuelEntryEntity>,
    val serviceRecords: List<ServiceRecordEntity>,
    val serviceItems: List<ServiceItemEntity>,
    val maintenanceRules: List<MaintenanceRuleEntity>,
    val maintenanceCompletions: List<MaintenanceCompletionEntity>,
    val documents: List<DocumentEntity>,
    val expenses: List<ExpenseEntity>,
    val attachments: List<AttachmentEntity>,
    val reminderDeliveries: List<ReminderDeliveryEntity>
)

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: CarLensDatabase
) : BackupRepository {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val storageDir = File(context.filesDir, "attachments").apply { mkdirs() }
    private val recoveryDir = File(context.filesDir, "recovery_backups").apply { mkdirs() }
    private val stagedDir = File(context.cacheDir, "staged_backups").apply { mkdirs() }

    override suspend fun export(destinationUri: String, password: CharArray?): Map<String, Int> =
        withContext(Dispatchers.IO) {
            val vehicles = db.vehicles().all()
            val readings = db.odometer().all()
            val fuel = db.fuel().all()
            val services = db.services().all()
            val items = db.services().allItems()
            val rules = db.maintenance().allRules()
            val completions = db.maintenance().allCompletions()
            val documents = db.documents().all()
            val expenses = db.expenses().all()
            val attachments = db.attachments().all()
            val deliveries = db.reminders().all()

            val counts = mapOf(
                "vehicles" to vehicles.size,
                "odometer" to readings.size,
                "fuel" to fuel.size,
                "services" to services.size,
                "items" to items.size,
                "rules" to rules.size,
                "documents" to documents.size,
                "expenses" to expenses.size,
                "attachments" to attachments.size
            )

            val checksums = mutableMapOf<String, String>()
            attachments.forEach { a ->
                val f = File(storageDir, a.relativePath)
                if (f.exists()) checksums[a.relativePath] = a.sha256
            }

            val manifest = BackupManifest(
                formatVersion = 1,
                schemaVersion = CarLensDatabase.SCHEMA_VERSION,
                createdAt = System.currentTimeMillis(),
                isEncrypted = password != null && password.isNotEmpty(),
                vehicles = vehicles.map { "${it.name} (${it.make} ${it.model})" },
                recordCounts = counts,
                fileChecksums = checksums
            )

            val dump = DatabaseDump(
                vehicles, readings, fuel, services, items,
                rules, completions, documents, expenses, attachments, deliveries
            )

            val manifestJson = json.encodeToString(manifest)
            val dumpJson = json.encodeToString(dump)

            val tempZip = File.createTempFile("carlens_exp_", ".zip", context.cacheDir)
            try {
                ZipOutputStream(BufferedOutputStream(FileOutputStream(tempZip))).use { zos ->
                    // manifest.json
                    zos.putNextEntry(ZipEntry("manifest.json"))
                    zos.write(manifestJson.toByteArray())
                    zos.closeEntry()

                    // data.json
                    zos.putNextEntry(ZipEntry("data.json"))
                    zos.write(dumpJson.toByteArray())
                    zos.closeEntry()

                    // attachments
                    attachments.forEach { a ->
                        val f = File(storageDir, a.relativePath)
                        if (f.exists()) {
                            zos.putNextEntry(ZipEntry("attachments/${a.relativePath}"))
                            FileInputStream(f).use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }

                // Write to target destinationUri
                val targetUri = Uri.parse(destinationUri)
                val outStream: OutputStream? = if (targetUri.scheme == "file" && targetUri.path != null) {
                    FileOutputStream(File(targetUri.path!!))
                } else {
                    context.contentResolver.openOutputStream(targetUri)
                }
                outStream?.use { os ->
                    if (password != null && password.isNotEmpty()) {
                        encryptStream(FileInputStream(tempZip), os, password)
                    } else {
                        FileInputStream(tempZip).use { it.copyTo(os) }
                    }
                } ?: throw BackupException("Could not open destination output stream")

            } finally {
                tempZip.delete()
            }

            counts
        }

    override suspend fun inspect(sourceUri: String, password: CharArray?): BackupPreview =
        withContext(Dispatchers.IO) {
            val token = UUID.randomUUID().toString()
            val targetFolder = File(stagedDir, token).apply { mkdirs() }
            val canonicalTargetRoot = targetFolder.canonicalPath + File.separator

            val uri = Uri.parse(sourceUri)
            val rawBytes = if (uri.scheme == "file" && uri.path != null) {
                File(uri.path!!).takeIf { it.exists() }?.readBytes()
            } else {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: throw BackupException("Could not read backup file")

            val zipBytes: ByteArray = if (isZipHeader(rawBytes)) {
                rawBytes
            } else {
                // Must be encrypted
                if (password == null || password.isEmpty()) {
                    throw PasswordRequiredException()
                }
                decryptBytes(rawBytes, password)
            }

            // Extract to targetFolder with Zip-Slip protection
            var manifest: BackupManifest? = null

            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val destFile = File(targetFolder, entry.name)
                    if (!destFile.canonicalPath.startsWith(canonicalTargetRoot)) {
                        throw BackupException("Invalid backup archive entry: ${entry.name}")
                    }
                    if (entry.isDirectory) {
                        destFile.mkdirs()
                    } else {
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { fos -> zis.copyTo(fos) }
                    }
                    if (entry.name == "manifest.json") {
                        manifest = json.decodeFromString(File(targetFolder, "manifest.json").readText())
                    }
                    entry = zis.nextEntry
                }
            }

            val validManifest = manifest ?: throw BackupException("Invalid backup: manifest.json is missing")
            val dataFile = File(targetFolder, "data.json")
            if (!dataFile.exists()) throw BackupException("Invalid backup: data.json is missing")

            // Verify attachment checksums if present
            var totalAttachBytes = 0L
            var attachCount = 0
            validManifest.fileChecksums.forEach { (relPath, expectedSha) ->
                val f = File(targetFolder, "attachments/$relPath")
                if (f.exists()) {
                    val calculatedSha = sha256Of(f)
                    if (!calculatedSha.equals(expectedSha, ignoreCase = true)) {
                        throw BackupException("Corrupted attachment: $relPath checksum mismatch")
                    }
                    totalAttachBytes += f.length()
                    attachCount++
                }
            }

            BackupPreview(
                token = token,
                createdAt = validManifest.createdAt,
                formatVersion = validManifest.formatVersion,
                schemaVersion = validManifest.schemaVersion,
                encrypted = validManifest.isEncrypted,
                vehicleNames = validManifest.vehicles,
                counts = validManifest.recordCounts,
                attachmentCount = attachCount,
                attachmentBytes = totalAttachBytes
            )
        }

    override suspend fun restore(token: String): Unit = withContext(Dispatchers.IO) {
        val stage = File(stagedDir, token)
        if (!stage.exists()) throw BackupException("Staged backup not found or expired")

        val dataFile = File(stage, "data.json")
        val dump: DatabaseDump = try {
            json.decodeFromString(dataFile.readText())
        } catch (e: Exception) {
            throw BackupException("Failed to parse database dump: ${e.message}", e)
        }

        // 1. Create safety recovery backup before modifying anything
        createRecoveryBackup()

        // 2. Transactionally replace all database rows
        db.withTransaction {
            db.bulk().clearVehicles() // Cascades to all vehicle-related tables
            db.bulk().clearAttachments()
            db.bulk().clearDeliveries()

            db.bulk().insertVehicles(dump.vehicles)
            db.bulk().insertReadings(dump.odometerReadings)
            db.bulk().insertRules(dump.maintenanceRules)
            db.bulk().insertFuel(dump.fuelEntries)
            db.bulk().insertServices(dump.serviceRecords)
            db.bulk().insertItems(dump.serviceItems)
            db.bulk().insertDocuments(dump.documents)
            db.bulk().insertExpenses(dump.expenses)
            db.bulk().insertCompletions(dump.maintenanceCompletions)
            db.bulk().insertAttachments(dump.attachments)
            db.bulk().insertDeliveries(dump.reminderDeliveries)
        }

        // 3. Copy attachments from stage to private attachments directory
        val stagedAttachDir = File(stage, "attachments")
        if (stagedAttachDir.exists()) {
            stagedAttachDir.listFiles()?.forEach { f ->
                if (f.isFile) {
                    val target = File(storageDir, f.name)
                    f.copyTo(target, overwrite = true)
                }
            }
        }

        // Clean up staged folder
        stage.deleteRecursively()
    }

    override suspend fun discardStaged(token: String) {
        withContext(Dispatchers.IO) {
            File(stagedDir, token).deleteRecursively()
        }
    }

    override suspend fun latestRecoveryBackup(): File? = withContext(Dispatchers.IO) {
        recoveryDir.listFiles()?.filter { it.extension == "carlens_recovery" }?.maxByOrNull { it.lastModified() }
    }

    private suspend fun createRecoveryBackup() {
        val recoveryFile = File(recoveryDir, "recovery_${System.currentTimeMillis()}.carlens_recovery")
        export(Uri.fromFile(recoveryFile).toString(), null)
        // Retain only the 3 most recent recovery archives
        recoveryDir.listFiles()
            ?.filter { it.extension == "carlens_recovery" }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(3)
            ?.forEach { it.delete() }
    }

    override suspend fun exportFuelCsv(vehicleId: String, destinationUri: String): Unit = withContext(Dispatchers.IO) {
        val entries = db.fuel().list(vehicleId)
        val readings = db.odometer().list(vehicleId).associateBy { it.id }
        val expenses = db.expenses().list(vehicleId).filter { it.fuelEntryId != null }.associateBy { it.fuelEntryId!! }

        val uri = Uri.parse(destinationUri)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.bufferedWriter().use { writer ->
                writer.write("Date,Odometer_km,Litres,Total_RM,Price_per_L_RM,Tank_Status,Missed_Previous,Station,Fuel_Type,Notes\n")
                entries.sortedBy { it.epochMillis }.forEach { e ->
                    val km = readings[e.odometerReadingId]?.km ?: 0L
                    val litres = e.volumeMl / 1000.0
                    val totalSen = expenses[e.id]?.amountSen ?: 0L
                    val totalRm = totalSen / 100.0
                    val unitPrice = e.unitPriceMilliSen?.let { it / 100000.0 } ?: (if (litres > 0) totalRm / litres else 0.0)
                    val date = LocalDate.ofEpochDay(e.localDate)
                    writer.write(
                        listOf(
                            date.toString(),
                            km.toString(),
                            "%.2f".format(litres),
                            "%.2f".format(totalRm),
                            "%.3f".format(unitPrice),
                            e.tankStatus,
                            e.missedPrevious.toString(),
                            "\"${e.station.replace("\"", "\"\"")}\"",
                            e.fuelType,
                            "\"${e.notes.replace("\"", "\"\"")}\""
                        ).joinToString(",") + "\n"
                    )
                }
            }
        }
    }

    override suspend fun exportExpensesCsv(vehicleId: String, destinationUri: String): Unit = withContext(Dispatchers.IO) {
        val expenses = db.expenses().list(vehicleId)
        val uri = Uri.parse(destinationUri)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.bufferedWriter().use { writer ->
                writer.write("Date,Category,Amount_RM,Merchant,Notes,Linked_Type\n")
                expenses.sortedBy { it.epochMillis }.forEach { e ->
                    val date = LocalDate.ofEpochDay(e.localDate)
                    val rm = e.amountSen / 100.0
                    val linkedType = when {
                        e.fuelEntryId != null -> "Fuel"
                        e.serviceRecordId != null -> "Service"
                        e.documentId != null -> "Document"
                        else -> "Standalone"
                    }
                    writer.write(
                        listOf(
                            date.toString(),
                            e.category,
                            "%.2f".format(rm),
                            "\"${e.merchant.replace("\"", "\"\"")}\"",
                            "\"${e.notes.replace("\"", "\"\"")}\"",
                            linkedType
                        ).joinToString(",") + "\n"
                    )
                }
            }
        }
    }

    // Encryption & Checksum helpers
    private fun isZipHeader(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()

    private fun sha256Of(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buf = ByteArray(8192)
            var n: Int
            while (fis.read(buf).also { n = it } != -1) {
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun encryptStream(input: InputStream, output: OutputStream, password: CharArray) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)

        // Write magic bytes "CLENC1", salt, and iv
        output.write("CLENC1".toByteArray())
        output.write(salt)
        output.write(iv)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, 65536, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

        CipherOutputStream(output, cipher).use { cos ->
            input.use { it.copyTo(cos) }
        }
    }

    private fun decryptBytes(bytes: ByteArray, password: CharArray): ByteArray {
        val magic = "CLENC1".toByteArray()
        if (bytes.size < magic.size + 16 + 12) {
            throw BackupException("File is not a valid encrypted CarLens backup")
        }
        for (i in magic.indices) {
            if (bytes[i] != magic[i]) throw BackupException("File is not a valid encrypted CarLens backup")
        }
        var offset = magic.size
        val salt = bytes.copyOfRange(offset, offset + 16)
        offset += 16
        val iv = bytes.copyOfRange(offset, offset + 12)
        offset += 12

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, 65536, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

        return try {
            val bis = ByteArrayInputStream(bytes, offset, bytes.size - offset)
            CipherInputStream(bis, cipher).use { it.readBytes() }
        } catch (e: Exception) {
            throw BackupException("Incorrect password or corrupted backup file", e)
        }
    }
}
