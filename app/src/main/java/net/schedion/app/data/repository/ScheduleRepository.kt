package net.schedion.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.CancellationException
import net.schedion.app.data.remote.dto.ScheduleWeekDto
import net.schedion.app.data.remote.dto.WeekListDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import net.schedion.app.core.Resource
import net.schedion.app.data.local.dao.LessonDao
import net.schedion.app.data.local.dao.PairDao
import net.schedion.app.data.local.dao.WeekDao
import net.schedion.app.data.mapper.toDomain
import net.schedion.app.data.mapper.toEntity
import net.schedion.app.data.remote.SchedionApiService
import net.schedion.app.data.remote.dto.GroupDto
import net.schedion.app.data.remote.dto.HomeContentDto
import net.schedion.app.data.remote.dto.TeacherDto
import net.schedion.app.domain.model.LessonItem
import net.schedion.app.domain.model.LessonStatus
import net.schedion.app.domain.model.PairItem
import net.schedion.app.domain.model.ScheduleDay
import net.schedion.app.domain.model.ScheduleWeek
import net.schedion.app.domain.model.WeekInfo
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/** Интерфейс репозитория расписания и справочников. */
interface ScheduleRepository {
    val availableGroups: StateFlow<List<GroupDto>>
    val availableTeachers: StateFlow<List<TeacherDto>>
    val groupsSavedAt: StateFlow<Long?>
    val teachersSavedAt: StateFlow<Long?>

    fun getSchedule(
        week: String? = null,
        group: Int? = null,
        teacher: Int? = null,
    ): Flow<Resource<ScheduleWeek>>

    fun getPairs(): Flow<Resource<List<PairItem>>>

    fun getWeeks(): Flow<Resource<List<WeekInfo>>>

    fun getHome(): Flow<Resource<HomeContentDto>>
}

/**
 * Репозиторий расписания, реализующий работу с сервером (Online-first)
 * с возможностью штатного перехода на локальный кэш Room при отсутствии сети.
 */
@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val apiService: SchedionApiService,
    private val lessonDao: LessonDao,
    private val pairDao: PairDao,
    private val weekDao: WeekDao,
    private val cache: CachedContent,
) : ScheduleRepository {

    private val _availableGroups = MutableStateFlow<List<GroupDto>>(emptyList())
    override val availableGroups: StateFlow<List<GroupDto>> = _availableGroups.asStateFlow()

    private val _availableTeachers = MutableStateFlow<List<TeacherDto>>(emptyList())
    override val availableTeachers: StateFlow<List<TeacherDto>> = _availableTeachers.asStateFlow()
    private val _groupsSavedAt = MutableStateFlow<Long?>(null)
    override val groupsSavedAt: StateFlow<Long?> = _groupsSavedAt.asStateFlow()
    private val _teachersSavedAt = MutableStateFlow<Long?>(null)
    override val teachersSavedAt: StateFlow<Long?> = _teachersSavedAt.asStateFlow()

    private var cachedDefaultWeek: String? = null

    override fun getSchedule(week: String?, group: Int?, teacher: Int?): Flow<Resource<ScheduleWeek>> = flow {
        emit(Resource.Loading)
        try {
            val resolvedWeek = week ?: resolveCurrentWeek()
            val key = "schedule:$resolvedWeek:group=$group:teacher=$teacher"
            val times = loadPairTimes()
            cache.read(key, ScheduleWeekDto.serializer())?.let { (snapshot, savedAt) ->
                updateDirectories(snapshot, savedAt)
                emit(Resource.Success(scheduleWeek(snapshot, times), true, savedAt))
            }
            val response = apiService.getSchedule(week = resolvedWeek, group = group, teacher = teacher)
            lessonDao.upsertAll(response.lessons.map { it.toEntity() })
            val savedAt = cache.save(key, response, ScheduleWeekDto.serializer())
            updateDirectories(response, savedAt)
            emit(Resource.Success(scheduleWeek(response, times), false, savedAt))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            emit(Resource.Error(message = "", cause = error))
        }
    }.flowOn(Dispatchers.IO)

    private fun updateDirectories(response: ScheduleWeekDto, savedAt: Long) {
        if (response.filters.groups.isNotEmpty()) {
            _availableGroups.update { mergeGroups(it, response.filters.groups) }
            _groupsSavedAt.update { maxOf(it ?: savedAt, savedAt) }
        }
        if (response.filters.teachers.isNotEmpty()) {
            _availableTeachers.update { known ->
                (known + response.filters.teachers).distinctBy { it.id }.sortedBy { it.shortName }
            }
            _teachersSavedAt.update { maxOf(it ?: savedAt, savedAt) }
        }
    }

    private fun scheduleWeek(response: ScheduleWeekDto, times: Map<Int, PairItem>): ScheduleWeek {
        val now = LocalTime.now()
        val today = LocalDate.now().toString()
        val lessons = response.lessons.map { lesson ->
            val pair = times[lesson.pair]
            val start = pair?.start.orEmpty()
            val end = pair?.end.orEmpty()
            lesson.toDomain(start, end, calculateLessonStatus(lesson.date, start, end, today, now))
        }
        return ScheduleWeek(response.weekStart, response.weekEnd,
            buildScheduleDays(response.weekStart, response.weekEnd, lessons, today))
    }

    override fun getPairs(): Flow<Resource<List<PairItem>>> = flow {
        emit(Resource.Loading)
        try {
            val remote = apiService.getPairs()
            pairDao.upsertAll(remote.items.map { it.toEntity() })
            val domain = remote.items.map { it.toDomain() }
            emit(Resource.Success(domain))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            val cached = pairDao.observeAll().first()
            if (cached.isNotEmpty()) {
                emit(Resource.Success(cached.map { it.toDomain() }))
            } else {
                emit(Resource.Error("Не удалось загрузить расписание звонков", e))
            }
        }
    }

    override fun getWeeks(): Flow<Resource<List<WeekInfo>>> = flow {
        emit(Resource.Loading)
        try {
            val remote = apiService.getWeeks()
            cache.save("schedule:weeks", remote, WeekListDto.serializer())
            cachedDefaultWeek = remote.currentWeekStart
            weekDao.upsertAll(remote.items.map { it.toEntity() })
            val currentStart = remote.currentWeekStart
            val domain = remote.items.map { item ->
                item.toEntity().toDomain(isCurrent = item.start == currentStart)
            }
            emit(Resource.Success(domain))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            val cached = weekDao.observeAll().first()
            if (cached.isNotEmpty()) {
                emit(Resource.Success(cached.map { it.toDomain() }))
            } else {
                emit(Resource.Error("Не удалось загрузить учебные недели", e))
            }
        }
    }

    override fun getHome(): Flow<Resource<HomeContentDto>> = flow {
        emit(Resource.Loading)
        try {
            val remote = apiService.getHome()
            emit(Resource.Success(remote))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            emit(Resource.Error("Не удалось загрузить сводку главной страницы", e))
        }
    }

    /**
     * Объединяет группы недели с уже известными.
     *
     * Сервер отдаёт в фильтрах только группы с занятиями на выбранной неделе, поэтому без
     * объединения часть групп периодически пропадала бы из общего списка.
     */
    private fun mergeGroups(known: List<GroupDto>, fresh: List<GroupDto>): List<GroupDto> =
        (known + fresh)
            .distinctBy { it.id }
            .sortedWith(compareBy({ it.course }, { it.name }))

    private suspend fun resolveCurrentWeek(): String {
        cachedDefaultWeek?.let { return it }
        return try {
            val weeksDto = apiService.getWeeks()
            cache.save("schedule:weeks", weeksDto, WeekListDto.serializer())
            cachedDefaultWeek = weeksDto.currentWeekStart
            weeksDto.currentWeekStart
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            cache.read("schedule:weeks", WeekListDto.serializer())?.first?.currentWeekStart
                ?: LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).toString()
        }
    }

    private suspend fun loadPairTimes(): Map<Int, PairItem> {
        return try {
            val cached = pairDao.observeAll().first()
            if (cached.isNotEmpty()) {
                cached.associate { it.number to it.toDomain() }
            } else {
                val remote = apiService.getPairs()
                pairDao.upsertAll(remote.items.map { it.toEntity() })
                remote.items.associate { it.number to it.toDomain() }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            defaultPairTimes
        }
    }

    private fun calculateLessonStatus(
        dateStr: String,
        timeStart: String,
        timeEnd: String,
        todayStr: String,
        nowTime: LocalTime,
    ): LessonStatus {
        if (dateStr < todayStr) return LessonStatus.Past
        if (dateStr > todayStr) return LessonStatus.Upcoming

        return try {
            val start = LocalTime.parse(timeStart)
            val end = LocalTime.parse(timeEnd)
            when {
                nowTime.isBefore(start) -> LessonStatus.Upcoming
                nowTime.isAfter(end) -> LessonStatus.Past
                else -> LessonStatus.Current
            }
        } catch (_: Exception) {
            LessonStatus.Upcoming
        }
    }

    private fun buildScheduleDays(
        weekStart: String,
        weekEnd: String,
        lessons: List<LessonItem>,
        todayStr: String,
    ): List<ScheduleDay> {
        val days = mutableListOf<ScheduleDay>()
        try {
            var curr = LocalDate.parse(weekStart)
            val end = LocalDate.parse(weekEnd)
            val shortDayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

            while (!curr.isAfter(end) && days.size < 6) {
                val dateStr = curr.toString()
                val dayOfWeekIndex = (curr.dayOfWeek.value - 1).coerceIn(0, 6)
                val dayLessons = lessons.filter { it.date == dateStr }
                val isWeekend = dayOfWeekIndex >= 5

                // Пустые выходные не показываем: суббота без пар не должна висеть в расписании
                if (!isWeekend || dayLessons.isNotEmpty()) {
                    days.add(
                        ScheduleDay(
                            date = dateStr,
                            dayOfWeek = shortDayNames[dayOfWeekIndex],
                            dayNumber = curr.dayOfMonth.toString(),
                            isToday = dateStr == todayStr,
                            lessons = dayLessons,
                        ),
                    )
                }
                curr = curr.plusDays(1)
            }
        } catch (_: Exception) {
            // fallback
        }
        return days
    }

    companion object {
        private val defaultPairTimes = mapOf(
            1 to PairItem(1, "08:30", "09:50"),
            2 to PairItem(2, "10:00", "11:20"),
            3 to PairItem(3, "11:40", "13:00"),
            4 to PairItem(4, "13:30", "14:50"),
            5 to PairItem(5, "15:00", "16:20"),
            6 to PairItem(6, "16:30", "17:50"),
            7 to PairItem(7, "18:00", "19:20"),
        )
    }
}
