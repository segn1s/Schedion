package net.schedion.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.schedion.app.data.local.entity.FavoriteEntity

/** Доступ к списку избранных источников расписания. */
@Dao
interface FavoriteDao {

    /** Наблюдает за избранным в порядке добавления. */
    @Query("SELECT * FROM favorites ORDER BY id ASC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    /** Добавляет источник в избранное; повторное добавление игнорируется. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: FavoriteEntity)

    /** Убирает источник из избранного. */
    @Query("DELETE FROM favorites WHERE kind = :kind AND sourceId = :sourceId")
    suspend fun delete(kind: String, sourceId: Int)

    /** Возвращает количество записей избранного с указанным источником. */
    @Query("SELECT COUNT(*) FROM favorites WHERE kind = :kind AND sourceId = :sourceId")
    suspend fun count(kind: String, sourceId: Int): Int
}
