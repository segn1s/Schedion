package net.schedion.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Снимок конкретного запроса: ключ включает страницу или выбранный источник расписания. */
@Entity(tableName = "content_cache")
data class ContentCacheEntity(
    @PrimaryKey val cacheKey: String,
    val payload: String,
    val savedAt: Long,
)
