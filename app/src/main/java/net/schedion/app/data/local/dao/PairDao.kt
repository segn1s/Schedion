package net.schedion.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import net.schedion.app.data.local.entity.PairEntity

/** Доступ к кэшу расписания звонков. */
@Dao
interface PairDao {

    /** Наблюдает за звонками в порядке номеров пар. */
    @Query("SELECT * FROM pairs ORDER BY number ASC")
    fun observeAll(): Flow<List<PairEntity>>

    /** Вставляет новые звонки и обновляет существующие по номеру пары. */
    @Upsert
    suspend fun upsertAll(pairs: List<PairEntity>)

    /** Полностью очищает кэш звонков. */
    @Query("DELETE FROM pairs")
    suspend fun clear()
}
