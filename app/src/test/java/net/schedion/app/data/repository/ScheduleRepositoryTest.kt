package net.schedion.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import net.schedion.app.core.Resource
import net.schedion.app.data.local.dao.LessonDao
import net.schedion.app.data.local.dao.PairDao
import net.schedion.app.data.local.dao.WeekDao
import net.schedion.app.data.local.entity.LessonEntity
import net.schedion.app.data.local.entity.PairEntity
import net.schedion.app.data.local.entity.WeekEntity
import net.schedion.app.data.remote.SchedionApiService
import net.schedion.app.data.remote.dto.DocumentItemDto
import net.schedion.app.data.remote.dto.DocumentListDto
import net.schedion.app.data.remote.dto.HomeContentDto
import net.schedion.app.data.remote.dto.LessonKind
import net.schedion.app.data.remote.dto.NewsArticleDto
import net.schedion.app.data.remote.dto.NewsFeedDto
import net.schedion.app.data.remote.dto.PairItemDto
import net.schedion.app.data.remote.dto.PairListDto
import net.schedion.app.data.remote.dto.ScheduleFiltersDto
import net.schedion.app.data.remote.dto.ScheduleLessonDto
import net.schedion.app.data.remote.dto.ScheduleWeekDto
import net.schedion.app.data.remote.dto.SemesterDto
import net.schedion.app.data.remote.dto.WeekColor
import net.schedion.app.data.remote.dto.WeekItemDto
import net.schedion.app.data.remote.dto.WeekListDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ScheduleRepositoryTest {

    private class FakeLessonDao : LessonDao {
        val storage = mutableListOf<LessonEntity>()

        override fun observeAll(): Flow<List<LessonEntity>> = flowOf(storage.toList())
        override fun observeByDate(date: String): Flow<List<LessonEntity>> =
            flowOf(storage.filter { it.date == date })
        override suspend fun upsertAll(lessons: List<LessonEntity>) {
            storage.clear()
            storage.addAll(lessons)
        }
        override suspend fun clear() {
            storage.clear()
        }
    }

    private class FakePairDao : PairDao {
        val storage = mutableListOf<PairEntity>()

        override fun observeAll(): Flow<List<PairEntity>> = flowOf(storage.toList())
        override suspend fun upsertAll(pairs: List<PairEntity>) {
            storage.clear()
            storage.addAll(pairs)
        }
        override suspend fun clear() {
            storage.clear()
        }
    }

    private class FakeWeekDao : WeekDao {
        val storage = mutableListOf<WeekEntity>()

        override fun observeAll(): Flow<List<WeekEntity>> = flowOf(storage.toList())
        override fun observeByStart(startDate: String): Flow<WeekEntity?> =
            flowOf(storage.find { it.startDate == startDate })
        override suspend fun upsertAll(weeks: List<WeekEntity>) {
            storage.clear()
            storage.addAll(weeks)
        }
        override suspend fun clear() {
            storage.clear()
        }
    }

    private class FakeApiService(var shouldFail: Boolean = false) : SchedionApiService {
        override suspend fun getSchedule(
            week: String?,
            group: Int?,
            course: Int?,
            teacher: Int?,
        ): ScheduleWeekDto {
            if (shouldFail) throw IOException("Network error")
            return ScheduleWeekDto(
                weekStart = "2026-11-09",
                weekEnd = "2026-11-14",
                lessons = listOf(
                    ScheduleLessonDto(
                        id = 101,
                        date = "2026-11-09",
                        pair = 1,
                        kind = LessonKind.LECTURE,
                        disciplineId = 1,
                        discipline = "Математический анализ",
                        room = "214",
                    ),
                ),
                cancellations = emptyList(),
                filters = ScheduleFiltersDto(emptyList(), emptyList(), emptyList()),
            )
        }

        override suspend fun getWeeks(): WeekListDto {
            if (shouldFail) throw IOException("Network error")
            return WeekListDto(
                semester = SemesterDto("2026-09-01", "2027-01-25", "1 семестр"),
                currentWeekStart = "2026-11-09",
                defaultWeekStart = "2026-11-09",
                items = listOf(
                    WeekItemDto("2026-11-09", "2026-11-15", 10, WeekColor.BLACK),
                ),
            )
        }

        override suspend fun getPairs(): PairListDto {
            if (shouldFail) throw IOException("Network error")
            return PairListDto(
                timeZone = "Europe/Moscow",
                items = listOf(
                    PairItemDto(1, "08:30:00", "09:50:00"),
                ),
            )
        }

        override suspend fun getHome(): HomeContentDto = throw UnsupportedOperationException()
        override suspend fun getNews(offset: Int?, count: Int?): NewsFeedDto {
            if (shouldFail) throw IOException("Network error")
            return NewsFeedDto(total = 0, offset = 0, count = 0, items = emptyList())
        }
        override suspend fun getNewsArticle(slug: String): NewsArticleDto = throw UnsupportedOperationException()
        override suspend fun getDocuments(): DocumentListDto = throw UnsupportedOperationException()
        override suspend fun getDocument(slug: String): DocumentItemDto = throw UnsupportedOperationException()
    }

    @Test
    fun getSchedule_onlineSuccess_savesToDaoAndEmitsSuccess() = runBlocking {
        val api = FakeApiService(shouldFail = false)
        val lessonDao = FakeLessonDao()
        val pairDao = FakePairDao()
        val weekDao = FakeWeekDao()
        val repository = ScheduleRepositoryImpl(api, lessonDao, pairDao, weekDao)

        val states = repository.getSchedule().toList()

        assertEquals(2, states.size)
        assertTrue(states[0] is Resource.Loading)
        assertTrue(states[1] is Resource.Success)

        val success = states[1] as Resource.Success
        assertFalse(success.data.isFromCache)
        assertEquals(1, lessonDao.storage.size)
        assertEquals(101, lessonDao.storage[0].id)
        assertEquals("Математический анализ", lessonDao.storage[0].discipline)
    }

    @Test
    fun getSchedule_onlineError_withCachedData_emitsCachedSuccess() = runBlocking {
        val api = FakeApiService(shouldFail = true)
        val lessonDao = FakeLessonDao().apply {
            storage.add(
                LessonEntity(
                    id = 101,
                    date = "2026-11-09",
                    pairNumber = 1,
                    kind = "Лекция",
                    disciplineId = 1,
                    discipline = "Математический анализ",
                    room = "214",
                    subgroup = null,
                    groupId = 1,
                    groupName = "2ИВТ_ТИИ1",
                    course = 2,
                    teachers = "Петров А.С.",
                    cancellationReason = null,
                ),
            )
        }
        val pairDao = FakePairDao()
        val weekDao = FakeWeekDao()
        val repository = ScheduleRepositoryImpl(api, lessonDao, pairDao, weekDao)

        val states = repository.getSchedule().toList()

        assertEquals(2, states.size)
        assertTrue(states[0] is Resource.Loading)
        assertTrue(states[1] is Resource.Success)

        val success = states[1] as Resource.Success
        assertTrue(success.data.isFromCache)
    }

    @Test
    fun getSchedule_onlineError_withoutCachedData_emitsError() = runBlocking {
        val api = FakeApiService(shouldFail = true)
        val lessonDao = FakeLessonDao()
        val pairDao = FakePairDao()
        val weekDao = FakeWeekDao()
        val repository = ScheduleRepositoryImpl(api, lessonDao, pairDao, weekDao)

        val states = repository.getSchedule().toList()

        assertEquals(2, states.size)
        assertTrue(states[0] is Resource.Loading)
        assertTrue(states[1] is Resource.Error)
    }

    @Test
    fun getWeeks_onlineSuccess_emitsWeeks() = runBlocking {
        val api = FakeApiService(shouldFail = false)
        val lessonDao = FakeLessonDao()
        val pairDao = FakePairDao()
        val weekDao = FakeWeekDao()
        val repository = ScheduleRepositoryImpl(api, lessonDao, pairDao, weekDao)

        val states = repository.getWeeks().toList()

        assertTrue(states[1] is Resource.Success)
        val success = states[1] as Resource.Success
        assertEquals(1, success.data.size)
        assertEquals(10, success.data[0].number)
        assertTrue(success.data[0].isCurrent)
    }
}
