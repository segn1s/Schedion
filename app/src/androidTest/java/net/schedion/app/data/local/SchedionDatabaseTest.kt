package net.schedion.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.schedion.app.data.local.entity.LessonEntity
import net.schedion.app.data.local.entity.PairEntity
import net.schedion.app.data.local.entity.WeekEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Проверяет чтение и запись DAO на InMemory-базе Room. */
@RunWith(AndroidJUnit4::class)
class SchedionDatabaseTest {

    private lateinit var database: SchedionDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SchedionDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun lessonDao_persistsAndClearsLessons() = runBlocking {
        val lesson = LessonEntity(
            id = 12,
            date = "2026-11-09",
            pairNumber = 1,
            kind = "lecture",
            disciplineId = 4,
            discipline = "Математический анализ",
            room = "А-201",
            subgroup = null,
            groupId = 3,
            groupName = "2ИСиП_ИС1",
            course = 2,
            teachers = "Иванов И.И.",
            cancellationReason = null,
        )

        database.lessonDao().upsertAll(listOf(lesson))
        assertEquals(listOf(lesson), database.lessonDao().observeAll().first())

        database.lessonDao().clear()
        assertTrue(database.lessonDao().observeAll().first().isEmpty())
    }

    @Test
    fun pairAndWeekDaos_persistCachedEntries() = runBlocking {
        database.pairDao().upsertAll(listOf(PairEntity(number = 1, start = "08:30:00", end = "10:00:00")))
        database.weekDao().upsertAll(
            listOf(WeekEntity(startDate = "2026-11-09", endDate = "2026-11-15", number = 11, color = "red")),
        )

        assertEquals(1, database.pairDao().observeAll().first().size)
        assertEquals(11, database.weekDao().observeByStart("2026-11-09").first()?.number)
    }
}
