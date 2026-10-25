package com.carlens.data.repository

import android.content.Context
import android.net.Uri
import com.carlens.data.db.AttachmentEntity
import com.carlens.data.db.CarLensDatabase
import com.carlens.data.db.toDomain
import com.carlens.domain.model.Attachment
import com.carlens.domain.model.AttachmentOwner
import com.carlens.domain.repository.AttachmentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.time.Clock
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: CarLensDatabase,
    private val clock: Clock
) : AttachmentRepository {

    private val storageDir = File(context.filesDir, "attachments").apply { mkdirs() }
    private val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }

    override fun observe(ownerType: AttachmentOwner, ownerId: String): Flow<List<Attachment>> =
        db.attachments().observe(ownerType.name, ownerId).map { list -> list.map { it.toDomain() } }

    override suspend fun import(ownerType: AttachmentOwner, ownerId: String, sourceUri: String): Attachment =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(sourceUri)
            val extension = context.contentResolver.getType(uri)?.let { mime ->
                when {
                    mime.contains("jpeg") || mime.contains("jpg") -> ".jpg"
                    mime.contains("png") -> ".png"
                    mime.contains("pdf") -> ".pdf"
                    else -> ""
                }
            } ?: ""

            val fileName = "${UUID.randomUUID()}$extension"
            val targetFile = File(storageDir, fileName)

            var size = 0L
            val digest = MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        size += read
                    }
                }
            } ?: throw IllegalStateException("Could not open input stream from URI: $sourceUri")

            val sha256 = digest.digest().joinToString("") { String.format(Locale.US, "%02x", it) }
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val displayName = uri.lastPathSegment?.substringAfterLast('/') ?: fileName

            val entity = AttachmentEntity(
                id = UUID.randomUUID().toString(),
                ownerType = ownerType.name,
                ownerId = ownerId,
                relativePath = fileName,
                mimeType = mimeType,
                sizeBytes = size,
                sha256 = sha256,
                displayName = displayName,
                createdAt = clock.millis()
            )
            db.attachments().upsert(entity)
            entity.toDomain()
        }

    override suspend fun importLocalFile(
        ownerType: AttachmentOwner,
        ownerId: String,
        file: File,
        mimeType: String
    ): Attachment = withContext(Dispatchers.IO) {
        val extension = file.extension.let { if (it.isNotEmpty()) ".$it" else "" }
        val fileName = "${UUID.randomUUID()}$extension"
        val targetFile = File(storageDir, fileName)

        var size = 0L
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            FileOutputStream(targetFile).use { output ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    size += read
                }
            }
        }
        val sha256 = digest.digest().joinToString("") { String.format(Locale.US, "%02x", it) }

        val entity = AttachmentEntity(
            id = UUID.randomUUID().toString(),
            ownerType = ownerType.name,
            ownerId = ownerId,
            relativePath = fileName,
            mimeType = mimeType,
            sizeBytes = size,
            sha256 = sha256,
            displayName = file.name,
            createdAt = clock.millis()
        )
        db.attachments().upsert(entity)
        entity.toDomain()
    }

    override suspend fun reassign(fromOwnerId: String, toOwnerId: String) {
        db.attachments().reassign(fromOwnerId, toOwnerId)
    }

    override suspend fun delete(id: String) {
        withContext(Dispatchers.IO) {
            val entity = db.attachments().get(id) ?: return@withContext
            db.attachments().delete(id)
            val file = File(storageDir, entity.relativePath)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    override fun fileFor(attachment: Attachment): File = File(storageDir, attachment.relativePath)

    override suspend fun newCameraFile(): File = withContext(Dispatchers.IO) {
        File.createTempFile("carlens_cam_", ".jpg", cameraDir)
    }

    override fun observeCounts(): Flow<Map<String, Int>> =
        db.attachments().observeCountsByOwner().map { rows ->
            rows.associate { it.ownerId to it.count }
        }

    override suspend fun pruneOrphans(olderThanMillis: Long): Int = withContext(Dispatchers.IO) {
        val cutoff = clock.millis() - olderThanMillis
        val allAttachments = db.attachments().all().filter { it.createdAt < cutoff }
        if (allAttachments.isEmpty()) return@withContext 0

        val validIds = buildSet {
            addAll(db.vehicles().all().map { it.id })
            addAll(db.fuel().all().map { it.id })
            addAll(db.services().all().map { it.id })
            addAll(db.expenses().all().map { it.id })
            addAll(db.documents().all().map { it.id })
        }
        var removed = 0
        for (att in allAttachments) {
            if (att.ownerId !in validIds) {
                delete(att.id)
                removed++
            }
        }
        removed
    }
}
