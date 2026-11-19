package net.schedion.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Связь версии файла с системной загрузкой сохраняется после перезапуска приложения. */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val fileKey: String,
    val downloadId: Long,
    val mimeType: String,
    val fileName: String,
)
