package net.schedion.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import net.schedion.app.data.local.entity.WeekEntity

/** Доступ к кэшу учебных недель. */
@Dao
interface WeekDao {

    /** Наблюдает за неделями семестра в хронологическом порядке. */
    @Query("SELECT * FROM weeks ORDER BY startDate ASC")
    fun observeAll(): Flow<List<WeekEntity>>

    /** Наблюдает за одной неделей по дате её начала; null, если неделя не закэширована. */
    @Query("SELECT * FROM weeks WHERE startDate = :startDate")
    fun observeByStart(startDate: String): Flow<WeekEntity?>

    /** Вставляет новые недели и обновляет существующие по дате начала. */
    @Upsert
    suspend fun upsertAll(weeks: List<WeekEntity>)

    /** Полностью очищает кэш недель. */
    @Query("DELETE FROM weeks")
    suspend fun clear()
}
