package net.schedion.app.data.local

import net.schedion.app.data.local.dao.ContentCacheDao
import net.schedion.app.data.local.dao.DownloadDao
import net.schedion.app.data.local.entity.ContentCacheEntity
import net.schedion.app.data.local.entity.DownloadEntity
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.schedion.app.data.local.dao.FavoriteDao
import net.schedion.app.data.local.dao.LessonDao
import net.schedion.app.data.local.dao.PairDao
import net.schedion.app.data.local.dao.WeekDao
import net.schedion.app.data.local.entity.FavoriteEntity
import net.schedion.app.data.local.entity.LessonEntity
import net.schedion.app.data.local.entity.PairEntity
import net.schedion.app.data.local.entity.WeekEntity

/** Локальная база данных приложения — офлайн-кэш расписания, звонков, недель и избранного. */
@Database(
    entities = [LessonEntity::class, PairEntity::class, WeekEntity::class, FavoriteEntity::class, ContentCacheEntity::class, DownloadEntity::class],
    version = 4,
)
abstract class SchedionDatabase : RoomDatabase() {

    abstract fun lessonDao(): LessonDao

    abstract fun pairDao(): PairDao

    abstract fun weekDao(): WeekDao

    abstract fun favoriteDao(): FavoriteDao

    abstract fun contentCacheDao(): ContentCacheDao

    abstract fun downloadDao(): DownloadDao

    companion object {
        /** Добавляет снимки ответов API и реестр системных загрузок без изменения существующих таблиц. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `content_cache` (`cacheKey` TEXT NOT NULL, `payload` TEXT NOT NULL, `savedAt` INTEGER NOT NULL, PRIMARY KEY(`cacheKey`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `downloads` (`fileKey` TEXT NOT NULL, `downloadId` INTEGER NOT NULL, `mimeType` TEXT NOT NULL, `fileName` TEXT NOT NULL, PRIMARY KEY(`fileKey`))")
            }
        }

        /** Добавляет таблицу избранных источников расписания. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorites` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`kind` TEXT NOT NULL, " +
                        "`sourceId` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`subtitle` TEXT, " +
                        "`course` INTEGER)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_favorites_kind_sourceId` " +
                        "ON `favorites` (`kind`, `sourceId`)",
                )
            }
        }

        /** Сохраняет порядок добавления уже существующих источников избранного. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `favorites` ADD COLUMN `position` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `favorites` SET `position` = `id`")
            }
        }
    }
}
