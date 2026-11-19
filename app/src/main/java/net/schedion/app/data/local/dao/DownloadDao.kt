package net.schedion.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import net.schedion.app.data.local.entity.DownloadEntity

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads WHERE fileKey = :key")
    suspend fun get(key: String): DownloadEntity?

    @Query("SELECT * FROM downloads")
    suspend fun getAll(): List<DownloadEntity>

    @Upsert
    suspend fun put(entry: DownloadEntity)
}
