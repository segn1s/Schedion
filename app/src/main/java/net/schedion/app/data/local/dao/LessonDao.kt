package net.schedion.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import net.schedion.app.data.local.entity.LessonEntity

/** Доступ к кэшу занятий расписания. */
@Dao
interface LessonDao {

    /** Наблюдает за всем кэшем занятий в порядке даты и номера пары. */
    @Query("SELECT * FROM schedule_lessons ORDER BY date ASC, pairNumber ASC")
    fun observeAll(): Flow<List<LessonEntity>>

    /** Наблюдает за занятиями конкретного дня. */
    @Query("SELECT * FROM schedule_lessons WHERE date = :date ORDER BY pairNumber ASC")
    fun observeByDate(date: String): Flow<List<LessonEntity>>

    /** Вставляет новые занятия и обновляет уже закэшированные по их id. */
    @Upsert
    suspend fun upsertAll(lessons: List<LessonEntity>)

    /** Полностью очищает кэш занятий. */
    @Query("DELETE FROM schedule_lessons")
    suspend fun clear()
}
