package net.schedion.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import net.schedion.app.core.Resource
import net.schedion.app.data.local.dao.LessonDao
import net.schedion.app.data.local.dao.PairDao
import net.schedion.app.data.local.dao.WeekDao
import net.schedion.app.data.local.entity.ContentCacheEntity
import net.schedion.app.data.local.entity.LessonEntity
import net.schedion.app.data.local.entity.PairEntity
import net.schedion.app.data.local.entity.WeekEntity
import net.schedion.app.data.remote.SchedionApiService
import net.schedion.app.data.remote.dto.DocumentItemDto
import net.schedion.app.data.remote.dto.DocumentListDto
import net.schedion.app.data.remote.dto.GroupDto
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
import net.schedion.app.data.remote.dto.TeacherDto
import net.schedion.app.data.remote.dto.WeekColor
import net.schedion.app.data.remote.dto.WeekItemDto
import net.schedion.app.data.remote.dto.WeekListDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import net.schedion.app.data.remote.SchedionJson
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

    private class FakeApiService(
        var shouldFail: Boolean = false,
        var filters: ScheduleFiltersDto = ScheduleFiltersDto(emptyList(), emptyList(), emptyList()),
        var otherWeekFilters: Map<String, ScheduleFiltersDto> = emptyMap(),
    ) : SchedionApiService {
        var lastScheduleWasUnfiltered = false
            private set

        override suspend fun getSchedule(
            week: String?,
            group: Int?,
            course: Int?,
            teacher: Int?,
        ): ScheduleWeekDto {
            lastScheduleWasUnfiltered = week == null && group == null && course == null && teacher == null
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
                filters = otherWeekFilters[week] ?: filters,
            )
        }

        override suspend fun getWeeks(): WeekListDto {
            if (shouldFail) throw IOException("Network error")
            return WeekListDto(
                semester = SemesterDto("2026-09-01", "2027-01-25", "1 семестр"),
                currentWeekStart = "2026-11-09",
                defaultWeekStart = "2026-11-09",
                items = listOf(WeekItemDto("2026-11-09", "2026-11-15", 10, WeekColor.BLACK)) +
                    otherWeekFilters.keys.map { week -> WeekItemDto(week, week, 9, WeekColor.BLACK) },
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
        val repository = ScheduleRepositoryImpl(api, lessonDao, pairDao, weekDao, CachedContent(InMemoryContentCacheDao(), SchedionJson))

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
    fun getSchedule_onlineError_withCachedData_keepsSnapshotAndEmitsNetworkError() = runBlocking {
        val api = FakeApiService()
        val repository = ScheduleRepositoryImpl(api, FakeLessonDao(), FakePairDao(), FakeWeekDao(),
            CachedContent(InMemoryContentCacheDao(), SchedionJson))
        repository.getSchedule(week = "2026-11-09", group = 1).toList()
        api.shouldFail = true
        val states = repository.getSchedule(week = "2026-11-09", group = 1).toList()
        assertTrue(states.first() is Resource.Loading)
        val cached = states[1] as Resource.Success
        assertTrue(cached.isFromCache)
        assertTrue(checkNotNull(cached.savedAt) > 0)
        assertEquals(101, cached.data.days.first().lessons.first().id)
        assertTrue((states.last() as Resource.Error).cause is IOException)
    }

    @Test
    fun getSchedule_offline_doesNotShowAnotherGroupOrWeek() = runBlocking {
        val api = FakeApiService()
        val repository = ScheduleRepositoryImpl(api, FakeLessonDao(), FakePairDao(), FakeWeekDao(),
            CachedContent(InMemoryContentCacheDao(), SchedionJson))
        repository.getSchedule(week = "2026-11-09", group = 1).toList()
        api.shouldFail = true
        val otherGroup = repository.getSchedule(week = "2026-11-09", group = 2).toList()
        val otherWeek = repository.getSchedule(week = "2026-11-16", group = 1).toList()
        assertTrue(otherGroup.none { it is Resource.Success })
        assertTrue(otherWeek.none { it is Resource.Success })
    }

    @Test
    fun directoryTimesRestoreFromCacheAndOnlyAdvanceForDirectoriesInFreshResponse() = runBlocking {
        val groups = listOf(GroupDto(id = 1, name = "1ИВТ", course = 1))
        val teachers = listOf(TeacherDto(id = 2, shortName = "Иванов И. И."))
        val api = FakeApiService(filters = ScheduleFiltersDto(groups, teachers, emptyList()))
        val cacheDao = InMemoryContentCacheDao()
        val week = "2026-11-09"
        val snapshotTime = 1_700_000_000_000L
        val snapshot = api.getSchedule(week, 1, null, null)
        cacheDao.put(ContentCacheEntity("schedule:$week:group=1:teacher=null",
            SchedionJson.encodeToString(ScheduleWeekDto.serializer(), snapshot), snapshotTime))
        api.shouldFail = true
        val repository = ScheduleRepositoryImpl(api, FakeLessonDao(), FakePairDao(), FakeWeekDao(),
            CachedContent(cacheDao, SchedionJson))

        repository.getSchedule(week = week, group = 1).toList()
        repository.getSchedule(week = week, group = 2).toList()
        assertEquals(groups, repository.availableGroups.value)
        assertEquals(teachers, repository.availableTeachers.value)
        assertEquals(snapshotTime, repository.groupsSavedAt.value)
        assertEquals(snapshotTime, repository.teachersSavedAt.value)

        api.shouldFail = false
        api.filters = ScheduleFiltersDto(emptyList(), teachers, emptyList())
        val fresh = repository.getSchedule(week = week, group = 2).toList().last() as Resource.Success
        assertEquals(snapshotTime, repository.groupsSavedAt.value)
        assertEquals(fresh.savedAt, repository.teachersSavedAt.value)

        api.shouldFail = true
        repository.getSchedule(week = week, group = 1).toList()
        assertEquals(fresh.savedAt, repository.teachersSavedAt.value)
    }

    @Test
    fun getDirectories_afterNetworkError_refreshesBothListsWithoutScheduleFilters() = runBlocking {
        val groups = listOf(GroupDto(id = 1, name = "1ИВТ", course = 1))
        val teachers = listOf(TeacherDto(id = 2, shortName = "Иванов И. И."))
        val api = FakeApiService(shouldFail = true)
        val repository = ScheduleRepositoryImpl(api, FakeLessonDao(), FakePairDao(), FakeWeekDao(),
            CachedContent(InMemoryContentCacheDao(), SchedionJson))

        val offline = repository.getDirectories().toList()
        assertTrue(offline.first() is Resource.Loading)
        assertTrue((offline.last() as Resource.Error).cause is IOException)

        api.shouldFail = false
        api.filters = ScheduleFiltersDto(groups, teachers, emptyList())
        val fresh = repository.getDirectories().toList().last() as Resource.Success

        assertTrue(api.lastScheduleWasUnfiltered)
        assertFalse(fresh.isFromCache)
        assertEquals(groups, repository.availableGroups.value)
        assertEquals(teachers, repository.availableTeachers.value)
    }

    @Test
    fun getDirectories_offline_restoresListsWithoutOpeningTheirSchedule() = runBlocking {
        val groups = listOf(GroupDto(id = 1, name = "1ИВТ", course = 1))
        val teachers = listOf(TeacherDto(id = 2, shortName = "Иванов И. И."))
        val api = FakeApiService(filters = ScheduleFiltersDto(groups, teachers, emptyList()))
        val cache = CachedContent(InMemoryContentCacheDao(), SchedionJson)
        val onlineRepository = ScheduleRepositoryImpl(api, FakeLessonDao(), FakePairDao(), FakeWeekDao(), cache)
        val fresh = onlineRepository.getDirectories().toList().last() as Resource.Success

        api.shouldFail = true
        val offlineRepository = ScheduleRepositoryImpl(api, FakeLessonDao(), FakePairDao(), FakeWeekDao(), cache)
        val offline = offlineRepository.getDirectories().toList()

        val cached = offline[1] as Resource.Success
        assertTrue(cached.isFromCache)
        assertEquals(groups, offlineRepository.availableGroups.value)
        assertEquals(teachers, offlineRepository.availableTeachers.value)
        assertEquals(fresh.savedAt, offlineRepository.groupsSavedAt.value)
        assertEquals(fresh.savedAt, offlineRepository.teachersSavedAt.value)
        assertTrue((offline.last() as Resource.Error).cause is IOException)
    }

    @Test
    fun getDirectories_combinesSupportForOtherWeeksAndRefreshesThemOnDemand() = runBlocking {
        val group = GroupDto(id = 1, name = "1ИВТ", course = 1)
        val currentTeacher = TeacherDto(id = 2, shortName = "Иванов И. И.")
        val earlierTeacher = TeacherDto(id = 3, shortName = "Петров П. П.")
        val earlierWeek = "2026-11-02"
        val current = ScheduleFiltersDto(listOf(group), listOf(currentTeacher), listOf(1))
        val earlier = ScheduleFiltersDto(emptyList(), listOf(earlierTeacher), emptyList())
        val api = FakeApiService(filters = current, otherWeekFilters = mapOf(earlierWeek to earlier))
        val repository = ScheduleRepositoryImpl(api, FakeLessonDao(), FakePairDao(), FakeWeekDao(),
            CachedContent(InMemoryContentCacheDao(), SchedionJson))

        val first = repository.getDirectories().toList().last() as Resource.Success
        assertEquals(listOf(currentTeacher, earlierTeacher), first.data.teachers)

        val updatedEarlier = earlier.copy(teachers = listOf(earlierTeacher.copy(shortName = "Сидоров С. С.")))
        api.otherWeekFilters = mapOf(earlierWeek to updatedEarlier)
        val automatic = repository.getDirectories().toList().last() as Resource.Success
        assertEquals(first.data, automatic.data)

        val manual = repository.getDirectories(refreshAllWeeks = true).toList().last() as Resource.Success
        assertEquals(listOf(currentTeacher, updatedEarlier.teachers.single()), manual.data.teachers)
    }

    @Test
    fun getSchedule_onlineError_withoutCachedData_emitsError() = runBlocking {
        val api = FakeApiService(shouldFail = true)
        val lessonDao = FakeLessonDao()
        val pairDao = FakePairDao()
        val weekDao = FakeWeekDao()
        val repository = ScheduleRepositoryImpl(api, lessonDao, pairDao, weekDao, CachedContent(InMemoryContentCacheDao(), SchedionJson))

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
        val repository = ScheduleRepositoryImpl(api, lessonDao, pairDao, weekDao, CachedContent(InMemoryContentCacheDao(), SchedionJson))

        val states = repository.getWeeks().toList()

        assertTrue(states[1] is Resource.Success)
        val success = states[1] as Resource.Success
        assertEquals(1, success.data.size)
        assertEquals(10, success.data[0].number)
        assertTrue(success.data[0].isCurrent)
    }
}
