package net.schedion.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.schedion.app.data.local.entity.FavoriteEntity

private const val OrderedFavorites = "SELECT * FROM favorites ORDER BY position ASC, id ASC"

/** Доступ к списку избранных источников расписания. */
@Dao
interface FavoriteDao {

    /** Наблюдает за избранным в сохранённом порядке. */
    @Query(OrderedFavorites)
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query(OrderedFavorites)
    suspend fun getAll(): List<FavoriteEntity>

    /** Добавляет источник в избранное; повторное добавление игнорируется. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: FavoriteEntity)

    /** Добавляет список источников в избранное. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favorites: List<FavoriteEntity>)

    /** Очищает всё избранное. */
    @Query("DELETE FROM favorites")
    suspend fun deleteAll()

    /** Убирает источник из избранного. */
    @Query("DELETE FROM favorites WHERE kind = :kind AND sourceId = :sourceId")
    suspend fun delete(kind: String, sourceId: Int)

    @Update
    suspend fun updateAll(favorites: List<FavoriteEntity>)
}
