package net.schedion.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.schedion.app.data.local.SchedionDatabase
import net.schedion.app.data.local.dao.LessonDao
import net.schedion.app.data.local.dao.PairDao
import net.schedion.app.data.local.dao.WeekDao

/** Hilt-модуль, предоставляющий Room-базу данных и её DAO. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** Имя файла локальной базы данных. */
    private const val DATABASE_NAME = "schedion.db"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SchedionDatabase =
        Room.databaseBuilder(context, SchedionDatabase::class.java, DATABASE_NAME)
            .addMigrations(SchedionDatabase.MIGRATION_1_2, SchedionDatabase.MIGRATION_2_3, SchedionDatabase.MIGRATION_3_4)
            .build()

    @Provides
    fun provideLessonDao(database: SchedionDatabase): LessonDao = database.lessonDao()

    @Provides
    fun providePairDao(database: SchedionDatabase): PairDao = database.pairDao()

    @Provides
    fun provideWeekDao(database: SchedionDatabase): WeekDao = database.weekDao()
    @Provides
    fun provideContentCacheDao(database: SchedionDatabase): net.schedion.app.data.local.dao.ContentCacheDao = database.contentCacheDao()

    @Provides
    fun provideDownloadDao(database: SchedionDatabase): net.schedion.app.data.local.dao.DownloadDao = database.downloadDao()
}
