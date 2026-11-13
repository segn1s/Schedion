package net.schedion.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import net.schedion.app.data.local.dao.LessonDao
import net.schedion.app.data.local.dao.PairDao
import net.schedion.app.data.local.dao.WeekDao
import net.schedion.app.data.local.entity.LessonEntity
import net.schedion.app.data.local.entity.PairEntity
import net.schedion.app.data.local.entity.WeekEntity

/** Локальная база данных приложения — офлайн-кэш расписания, звонков и недель. */
@Database(
    entities = [LessonEntity::class, PairEntity::class, WeekEntity::class],
    version = 1,
)
abstract class SchedionDatabase : RoomDatabase() {

    abstract fun lessonDao(): LessonDao

    abstract fun pairDao(): PairDao

    abstract fun weekDao(): WeekDao
}
