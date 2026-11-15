package net.schedion.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import net.schedion.app.core.Resource
import net.schedion.app.data.local.dao.LessonDao
import net.schedion.app.data.local.dao.PairDao
import net.schedion.app.data.local.dao.WeekDao
import net.schedion.app.data.mapper.toDomain
import net.schedion.app.data.mapper.toEntity
import net.schedion.app.data.remote.SchedionApiService
import net.schedion.app.data.remote.dto.GroupDto
import net.schedion.app.data.remote.dto.HomeContentDto
import net.schedion.app.data.remote.dto.NewsFeedDto
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

    fun getSchedule(
        week: String? = null,
        group: Int? = null,
        teacher: Int? = null,
    ): Flow<Resource<ScheduleWeek>>

    fun getPairs(): Flow<Resource<List<PairItem>>>

    fun getWeeks(): Flow<Resource<List<WeekInfo>>>

    fun getHome(): Flow<Resource<HomeContentDto>>

    fun getNews(offset: Int? = null, count: Int? = null): Flow<Resource<NewsFeedDto>>
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
) : ScheduleRepository {

    private val _availableGroups = MutableStateFlow<List<GroupDto>>(emptyList())
    override val availableGroups: StateFlow<List<GroupDto>> = _availableGroups.asStateFlow()

    private val _availableTeachers = MutableStateFlow<List<TeacherDto>>(emptyList())
    override val availableTeachers: StateFlow<List<TeacherDto>> = _availableTeachers.asStateFlow()

    private var cachedDefaultWeek: String? = null

    override fun getSchedule(
        week: String?,
        group: Int?,
        teacher: Int?,
    ): Flow<Resource<ScheduleWeek>> = flow {
        emit(Resource.Loading)

        val pairTimesMap = loadPairTimes()

        // Определяем актуальную учебную неделю, если не передана явно
        val resolvedWeek = week ?: resolveCurrentWeek()

        try {
            // Запрос данных с сервера
            val remoteSchedule = apiService.getSchedule(
                week = resolvedWeek,
                group = group,
                teacher = teacher,
            )

            // Обновляем доступные списки групп и преподавателей из фильтров API
            if (remoteSchedule.filters.groups.isNotEmpty()) {
                _availableGroups.value = mergeGroups(_availableGroups.value, remoteSchedule.filters.groups)
            }
            if (remoteSchedule.filters.teachers.isNotEmpty()) {
                _availableTeachers.value = remoteSchedule.filters.teachers
            }

            // Кэширование полученных занятий в локальную БД Room
            val entities = remoteSchedule.lessons.map { it.toEntity() }
            lessonDao.upsertAll(entities)

            // Маппинг в доменные модели занятий
            val now = LocalTime.now()
            val todayStr = LocalDate.now().toString()

            val lessonItems = remoteSchedule.lessons.map { lessonDto ->
                val pairTimes = pairTimesMap[lessonDto.pair]
                val start = pairTimes?.start ?: ""
                val end = pairTimes?.end ?: ""
                val status = calculateLessonStatus(lessonDto.date, start, end, todayStr, now)
                lessonDto.toDomain(timeStart = start, timeEnd = end, status = status)
            }

            val days = buildScheduleDays(
                weekStart = remoteSchedule.weekStart,
                weekEnd = remoteSchedule.weekEnd,
                lessons = lessonItems,
                todayStr = todayStr,
            )

            emit(
                Resource.Success(
                    ScheduleWeek(
                        weekStart = remoteSchedule.weekStart,
                        weekEnd = remoteSchedule.weekEnd,
                        days = days,
                        isFromCache = false,
                    ),
                ),
            )
        } catch (e: Exception) {
            // При ошибке сети пытаемся восстановить данные из кэша
            val cachedEntities = lessonDao.observeAll().first()
            if (cachedEntities.isNotEmpty()) {
                val now = LocalTime.now()
                val todayStr = LocalDate.now().toString()

                val lessonItems = cachedEntities.map { entity ->
                    val pairTimes = pairTimesMap[entity.pairNumber]
                    val start = pairTimes?.start ?: ""
                    val end = pairTimes?.end ?: ""
                    val status = calculateLessonStatus(entity.date, start, end, todayStr, now)
                    entity.toDomain(timeStart = start, timeEnd = end, status = status)
                }

                val minDate = cachedEntities.minOf { it.date }
                val maxDate = cachedEntities.maxOf { it.date }
                val days = buildScheduleDays(minDate, maxDate, lessonItems, todayStr)

                emit(
                    Resource.Success(
                        ScheduleWeek(
                            weekStart = minDate,
                            weekEnd = maxDate,
                            days = days,
                            isFromCache = true,
                            cachedTimestamp = "09.11, 10:42",
                        ),
                    ),
                )
            } else {
                emit(
                    Resource.Error(
                        message = "Не удалось подключиться к серверу. Проверьте соединение с интернетом.",
                        cause = e,
                    ),
                )
            }
        }
    }

    override fun getPairs(): Flow<Resource<List<PairItem>>> = flow {
        emit(Resource.Loading)
        try {
            val remote = apiService.getPairs()
            pairDao.upsertAll(remote.items.map { it.toEntity() })
            val domain = remote.items.map { it.toDomain() }
            emit(Resource.Success(domain))
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
            cachedDefaultWeek = remote.currentWeekStart
            weekDao.upsertAll(remote.items.map { it.toEntity() })
            val currentStart = remote.currentWeekStart
            val domain = remote.items.map { item ->
                item.toEntity().toDomain(isCurrent = item.start == currentStart)
            }
            emit(Resource.Success(domain))
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
        } catch (e: Exception) {
            emit(Resource.Error("Не удалось загрузить сводку главной страницы", e))
        }
    }

    override fun getNews(offset: Int?, count: Int?): Flow<Resource<NewsFeedDto>> = flow {
        emit(Resource.Loading)
        try {
            val remote = apiService.getNews(offset = offset, count = count)
            emit(Resource.Success(remote))
        } catch (e: Exception) {
            emit(Resource.Error("Не удалось загрузить новости", e))
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
            cachedDefaultWeek = weeksDto.currentWeekStart
            weeksDto.currentWeekStart
        } catch (_: Exception) {
            "2026-11-09"
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
