package net.schedion.app.data.repository

import android.app.DownloadManager
import android.content.Context
import androidx.core.net.toUri
import android.net.Uri
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.schedion.app.data.local.dao.DownloadDao
import net.schedion.app.data.local.entity.DownloadEntity
import net.schedion.app.domain.model.ContentFile
import net.schedion.app.domain.model.contentUrl
import net.schedion.app.domain.model.safeFileName

enum class DownloadStatus { Queued, Running, Complete, Failed }
data class DownloadProgress(val status: DownloadStatus, val percent: Int? = null)

/** DownloadManager продолжает загрузку после закрытия приложения; Room хранит ID конкретной версии. */
@Singleton
class DownloadsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: DownloadDao,
) {
    private val manager = context.getSystemService(DownloadManager::class.java)
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val downloads: StateFlow<Map<String, DownloadProgress>> = flow {
        while (currentCoroutineContext().isActive) {
            emit(dao.getAll().associate { it.fileKey to progress(it.downloadId) })
            delay(PollInterval)
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(StopTimeout), emptyMap())

    suspend fun download(file: ContentFile): Long = withContext(Dispatchers.IO) {
        mutex.withLock {
            require(contentUrl(file.url) != null)
            val existing = dao.get(file.key)
            if (existing != null) {
                val status = progress(existing.downloadId).status
                if (status == DownloadStatus.Queued || status == DownloadStatus.Running ||
                    (status == DownloadStatus.Complete && readable(existing.downloadId))) return@withLock existing.downloadId
            }
            val name = safeFileName(file.fileName)
            val extension = name.substringAfterLast('.', "")
            val stem = if (extension.isBlank()) name else name.substringBeforeLast('.')
            val destination = "$stem-${file.key.take(8)}-${System.currentTimeMillis()}" +
                if (extension.isBlank()) "" else ".$extension"
            val request = DownloadManager.Request(file.url.toUri())
                .setTitle(name)
                .setMimeType(file.mimeType)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Schedion/$destination")
            val id = manager.enqueue(request)
            dao.put(DownloadEntity(file.key, id, file.mimeType, name))
            id
        }
    }

    suspend fun downloadedUri(file: ContentFile): Uri? = withContext(Dispatchers.IO) {
        val entry = dao.get(file.key) ?: return@withContext null
        if (readable(entry.downloadId)) manager.getUriForDownloadedFile(entry.downloadId) else null
    }

    private fun readable(id: Long): Boolean { return try {
        val uri = manager.getUriForDownloadedFile(id) ?: return false
        context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
    } catch (_: Exception) { false } }

    private fun progress(id: Long): DownloadProgress { return try {
        manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            if (cursor == null || !cursor.moveToFirst()) return DownloadProgress(DownloadStatus.Failed)
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val received = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val percent = if (total > 0) ((received * 100 / total).coerceIn(0, 100)).toInt() else null
            DownloadProgress(when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> if (readable(id)) DownloadStatus.Complete else DownloadStatus.Failed
                DownloadManager.STATUS_FAILED -> DownloadStatus.Failed
                DownloadManager.STATUS_RUNNING -> DownloadStatus.Running
                else -> DownloadStatus.Queued
            }, percent)
        }
    } catch (_: Exception) { DownloadProgress(DownloadStatus.Failed) } }

    private companion object {
        const val PollInterval = 1000L
        const val StopTimeout = 5000L
    }
}
