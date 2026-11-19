package net.schedion.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import net.schedion.app.data.local.entity.ContentCacheEntity

@Dao
interface ContentCacheDao {
    @Query("SELECT * FROM content_cache WHERE cacheKey = :key")
    suspend fun get(key: String): ContentCacheEntity?

    @Upsert
    suspend fun put(entry: ContentCacheEntity)
}
