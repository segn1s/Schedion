package net.schedion.app.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.schedion.app.core.ContentState
import net.schedion.app.core.Resource
import net.schedion.app.data.local.SavedSourceStore
import net.schedion.app.data.mapper.toFavorite
import net.schedion.app.data.remote.dto.GroupDto
import net.schedion.app.data.remote.dto.ScheduleFiltersDto
import net.schedion.app.data.remote.dto.TeacherDto
import net.schedion.app.data.repository.FavoritesRepository
import net.schedion.app.data.repository.ScheduleRepository
import net.schedion.app.domain.model.BreakStatus
import net.schedion.app.domain.model.CurrentPairStatus
import net.schedion.app.domain.model.FavoriteKind
import net.schedion.app.domain.model.FavoriteSource
import net.schedion.app.domain.model.LessonItem
import net.schedion.app.domain.model.LessonStatus
import net.schedion.app.domain.model.ScheduleWeek
import net.schedion.app.domain.model.WeekInfo

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm[:ss]")

/**
 * ViewModel расписания занятий.
 *
 * Связывает UI со слоем данных [ScheduleRepository], обрабатывает отображение расписания,
 * выбор дня/режима и статус текущей пары с переменой по времени звонков (#38).
 */
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: ScheduleRepository,
    private val favoritesRepository: FavoritesRepository,
    private val savedSourceStore: SavedSourceStore,
) : ViewModel() {

    private val savedSource: FavoriteSource? = savedSourceStore.load()

    private val _hasInitialSource = MutableStateFlow<Boolean?>(null)

    /** null до чтения избранного, true при наличии источника, false для онбординга. */
    val hasInitialSource: StateFlow<Boolean?> = _hasInitialSource.asStateFlow()

    val availableGroups: StateFlow<List<GroupDto>> = repository.availableGroups
    val availableTeachers: StateFlow<List<TeacherDto>> = repository.availableTeachers
    internal val groupsSavedAt: StateFlow<Long?> = repository.groupsSavedAt
    internal val teachersSavedAt: StateFlow<Long?> = repository.teachersSavedAt
    private val _directoryState = MutableStateFlow(ContentState<ScheduleFiltersDto>())
    internal val directoryState = _directoryState.asStateFlow()
    private var directoryJob: Job? = null

    /** Избранные группы и преподаватели в сохранённом порядке. */
    val favorites: StateFlow<List<FavoriteSource>> = favoritesRepository.favorites
    val favoritesLimit = FavoritesRepository.MaxFavorites

    private val favoritesLimitEvents = Channel<Unit>(Channel.CONFLATED)
    val favoritesLimitReached = favoritesLimitEvents.receiveAsFlow()

    private val _selectedGroup = MutableStateFlow<GroupDto?>(null)
    val selectedGroup: StateFlow<GroupDto?> = _selectedGroup.asStateFlow()

    private val _selectedTeacher = MutableStateFlow<TeacherDto?>(null)
    val selectedTeacher: StateFlow<TeacherDto?> = _selectedTeacher.asStateFlow()

    private val _currentWeek = MutableStateFlow<String?>(null)
    val currentWeek: StateFlow<String?> = _currentWeek.asStateFlow()

    private var scheduleJob: Job? = null
    private val _contentState = MutableStateFlow(ContentState<ScheduleWeek>())
    val contentState = _contentState.asStateFlow()

    private val _scheduleState = MutableStateFlow<Resource<ScheduleWeek>>(Resource.Loading)
    val scheduleState: StateFlow<Resource<ScheduleWeek>> = _scheduleState.asStateFlow()

    private val _selectedDayIndex = MutableStateFlow(0)
    val selectedDayIndex: StateFlow<Int> = _selectedDayIndex.asStateFlow()

    private val _isWeeklyMode = MutableStateFlow(false)
    val isWeeklyMode: StateFlow<Boolean> = _isWeeklyMode.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedLesson = MutableStateFlow<LessonItem?>(null)
    val selectedLesson: StateFlow<LessonItem?> = _selectedLesson.asStateFlow()

    private val _weeks = MutableStateFlow<List<WeekInfo>>(emptyList())

    /** Учебные недели семестра по возрастанию даты начала. */
    val weeks: StateFlow<List<WeekInfo>> = _weeks.asStateFlow()
    private var weeksJob: Job? = null

    private val favoritePromptEvents = Channel<FavoriteSource>(Channel.CONFLATED)

    /** Предложения добавить только что выбранный источник в избранное. */
    val favoritePrompts = favoritePromptEvents.receiveAsFlow()

    /** Статус текущей пары (#38). */
    private val _currentPairStatus = MutableStateFlow<CurrentPairStatus?>(null)
    val currentPairStatus: StateFlow<CurrentPairStatus?> = _currentPairStatus.asStateFlow()

    /** Статус перемены перед следующей парой (#38). */
    private val _breakStatus = MutableStateFlow<BreakStatus?>(null)
    val breakStatus: StateFlow<BreakStatus?> = _breakStatus.asStateFlow()

    init {
        refreshDirectories(refreshAllWeeks = false)
        refreshWeeks()
        viewModelScope.launch {
            val initialSource = favoritesRepository.getFirstFavorite() ?: savedSource
            initialSource?.let(::setSelectedSource)
            _hasInitialSource.value = initialSource != null
            loadSchedule()
        }

        // Периодическое обновление статуса текущей пары и перемены каждую минуту (#38)
        viewModelScope.launch {
            while (isActive) {
                updateTimeStatuses()
                delay(30_000)
            }
        }
    }

    internal fun refreshDirectories(refreshAllWeeks: Boolean) {
        directoryJob?.cancel()
        directoryJob = viewModelScope.launch {
            repository.getDirectories(refreshAllWeeks).collect { result ->
                val current = _directoryState.value
                if (result is Resource.Success && result.isFromCache && current.data != null) return@collect
                _directoryState.value = current.accept(result)
            }
        }
    }

    /** Загружает список учебных недель для переключения недель в режиме «Неделя». */
    internal fun refreshWeeks() {
        weeksJob?.cancel()
        weeksJob = viewModelScope.launch {
            repository.getWeeks().collect { result ->
                if (result is Resource.Success) _weeks.value = result.data.sortedBy { it.startDate }
            }
        }
    }

    fun loadSchedule() {
        scheduleJob?.cancel()
        scheduleJob = viewModelScope.launch {
            repository.getSchedule(
                week = _currentWeek.value,
                group = _selectedGroup.value?.id,
                teacher = _selectedTeacher.value?.id,
            ).collect { resource ->
                if (resource is Resource.Success && resource.isFromCache && _contentState.value.data != null) return@collect
                _contentState.value = _contentState.value.accept(resource)
                _scheduleState.value = _contentState.value.resource
                _isRefreshing.value = _contentState.value.isRefreshing
                if (resource is Resource.Success) {
                    _selectedDayIndex.value = _selectedDayIndex.value.coerceIn(
                        0,
                        (resource.data.days.size - 1).coerceAtLeast(0),
                    )
                }
                updateTimeStatuses()
            }
        }
    }

    private fun clearShownSchedule() {
        _contentState.value = ContentState()
        _scheduleState.value = Resource.Loading
        _selectedDayIndex.value = 0
        _currentPairStatus.value = null
        _breakStatus.value = null
    }

    fun refreshSchedule() {
        loadSchedule()
    }

    fun selectGroup(group: GroupDto) {
        if (_selectedGroup.value?.id != group.id || _selectedTeacher.value != null) clearShownSchedule()
        _selectedGroup.value = group
        _selectedTeacher.value = null
        val favorite = group.toFavorite()
        savedSourceStore.save(favorite)
        promptFavoriteIfNeeded(favorite)
        loadSchedule()
    }

    fun selectTeacher(teacher: TeacherDto) {
        if (_selectedTeacher.value?.id != teacher.id || _selectedGroup.value != null) clearShownSchedule()
        _selectedTeacher.value = teacher
        _selectedGroup.value = null
        val favorite = teacher.toFavorite()
        savedSourceStore.save(favorite)
        promptFavoriteIfNeeded(favorite)
        loadSchedule()
    }

    /** Предлагает добавить в избранное источник, которого там ещё нет. */
    private fun promptFavoriteIfNeeded(favorite: FavoriteSource) {
        val saved = favoritesRepository.favorites.value
        if (saved.none { it.kind == favorite.kind && it.id == favorite.id }) {
            favoritePromptEvents.trySend(favorite)
        }
    }

    /** Добавляет источник в избранное после подтверждения в диалоге. */
    fun addFavorite(favorite: FavoriteSource) {
        viewModelScope.launch {
            val saved = favoritesRepository.favorites.value
            if (saved.none { it.kind == favorite.kind && it.id == favorite.id } &&
                !favoritesRepository.toggle(favorite)
            ) {
                favoritesLimitEvents.trySend(Unit)
            }
        }
    }

    /** Переключает расписание на выбранную учебную неделю. */
    fun selectWeek(week: WeekInfo) {
        if (_currentWeek.value == week.startDate) return
        _currentWeek.value = week.startDate
        clearShownSchedule()
        loadSchedule()
    }

    fun selectDay(index: Int) {
        _selectedDayIndex.value = index.coerceIn(0, 5)
    }

    fun setWeeklyMode(weekly: Boolean) {
        _isWeeklyMode.value = weekly
    }

    fun selectLesson(lesson: LessonItem?) {
        _selectedLesson.value = lesson?.let(::withFreshStatus)
    }

    /** Пересчитывает статус занятия по текущему времени: он в расписании не обновляется сам. */
    private fun withFreshStatus(lesson: LessonItem, now: LocalTime = LocalTime.now()): LessonItem {
        if (lesson.status == LessonStatus.Cancelled) return lesson
        val today = LocalDate.now().toString()
        val status = when {
            lesson.date < today -> LessonStatus.Past
            lesson.date > today -> LessonStatus.Upcoming
            else -> {
                val start = parseTimeOrNull(lesson.timeStart)
                val end = parseTimeOrNull(lesson.timeEnd)
                when {
                    start == null || end == null -> lesson.status
                    now.isBefore(start) -> LessonStatus.Upcoming
                    now.isAfter(end) -> LessonStatus.Past
                    else -> LessonStatus.Current
                }
            }
        }
        return lesson.copy(status = status)
    }

    /**
     * Вычисляет остаток времени текущей пары и статус перемены (#38).
     *
     * Расчёт производится по времени звонков занятий и локальному времени устройства [LocalTime.now].
     * Таймзона сервера из GET /pairs сейчас не используется и расчёт опирается на системные часы.
     */
    fun updateTimeStatuses(now: LocalTime = LocalTime.now()) {
        val schedule = _contentState.value.data
        val today = schedule?.days?.firstOrNull { it.isToday }
        if (today == null || today.lessons.isEmpty()) {
            _currentPairStatus.value = null
            _breakStatus.value = null
            return
        }

        var activePair: CurrentPairStatus? = null
        var activeBreak: BreakStatus? = null

        val lessons = today.lessons
            .filterNot { it.status == LessonStatus.Cancelled }
            .sortedBy { it.pairNumber }

        for (lesson in lessons) {
            val start = parseTimeOrNull(lesson.timeStart)
            val end = parseTimeOrNull(lesson.timeEnd)
            if (start != null && end != null) {
                if (!now.isBefore(start) && !now.isAfter(end)) {
                    val remaining = ChronoUnit.MINUTES.between(now, end).coerceAtLeast(0)
                    activePair = CurrentPairStatus(lesson.pairNumber, lesson.timeEnd, remaining)
                    break
                }
            }
        }

        // Перемена показывается только между парами: до первой пары дня её ещё нет
        if (activePair == null) {
            val previousEnded = lessons.any { lesson ->
                parseTimeOrNull(lesson.timeEnd)?.let { now.isAfter(it) } == true
            }
            if (previousEnded) {
                for (lesson in lessons) {
                    val start = parseTimeOrNull(lesson.timeStart)
                    if (start != null && now.isBefore(start)) {
                        val remaining = ChronoUnit.MINUTES.between(now, start).coerceAtLeast(0)
                        activeBreak = BreakStatus(lesson.pairNumber, lesson.timeStart, remaining)
                        break
                    }
                }
            }
        }

        _currentPairStatus.value = activePair
        _breakStatus.value = activeBreak
    }

    private fun parseTimeOrNull(timeStr: String): LocalTime? = try {
        if (timeStr.isBlank()) null else LocalTime.parse(timeStr, TimeFormat)
    } catch (_: Exception) {
        null
    }

    fun toggleGroupFavorite(group: GroupDto) {
        toggleFavorite(group.toFavorite())
    }

    fun toggleTeacherFavorite(teacher: TeacherDto) {
        toggleFavorite(teacher.toFavorite())
    }

    private fun toggleFavorite(favorite: FavoriteSource) {
        viewModelScope.launch {
            if (!favoritesRepository.toggle(favorite)) favoritesLimitEvents.trySend(Unit)
        }
    }

    fun removeFavorite(favorite: FavoriteSource) {
        viewModelScope.launch { favoritesRepository.remove(favorite) }
    }

    fun moveFavorite(favorite: FavoriteSource, toIndex: Int) {
        viewModelScope.launch { favoritesRepository.move(favorite, toIndex) }
    }

    fun openFavorite(favorite: FavoriteSource) {
        clearShownSchedule()
        savedSourceStore.save(setSelectedSource(favorite))
        loadSchedule()
    }

    private fun setSelectedSource(favorite: FavoriteSource): FavoriteSource =
        when (favorite.kind) {
            FavoriteKind.Group -> {
                val group = availableGroups.value.firstOrNull { it.id == favorite.id }
                    ?: GroupDto(
                        id = favorite.id,
                        name = favorite.title,
                        description = favorite.subtitle,
                        course = favorite.course ?: 0,
                    )
                _selectedGroup.value = group
                _selectedTeacher.value = null
                group.toFavorite()
            }
            FavoriteKind.Teacher -> {
                val teacher = availableTeachers.value.firstOrNull { it.id == favorite.id }
                    ?: TeacherDto(
                        id = favorite.id,
                        shortName = favorite.title,
                    )
                _selectedTeacher.value = teacher
                _selectedGroup.value = null
                teacher.toFavorite()
            }
        }
}
