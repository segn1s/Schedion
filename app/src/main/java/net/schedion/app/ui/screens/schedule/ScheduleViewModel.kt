package net.schedion.app.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import net.schedion.app.core.ContentState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.schedion.app.core.Resource
import net.schedion.app.data.local.SavedSourceStore
import net.schedion.app.data.mapper.toFavorite
import net.schedion.app.data.remote.dto.GroupDto
import net.schedion.app.data.remote.dto.TeacherDto
import net.schedion.app.data.repository.FavoritesRepository
import net.schedion.app.data.repository.ScheduleRepository
import net.schedion.app.domain.model.FavoriteKind
import net.schedion.app.domain.model.FavoriteSource
import net.schedion.app.domain.model.LessonItem
import net.schedion.app.domain.model.ScheduleWeek
import javax.inject.Inject

/**
 * ViewModel расписания и выбора источников.
 *
 * Связывает UI со слоем данных [ScheduleRepository], обрабатывает выбор групп и преподавателей,
 * избранное, реактивный поиск, переключение дней/режимов и обновление данных.
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

    // Поиск
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchTab = MutableStateFlow(0) // 0 - Groups, 1 - Teachers
    val searchTab: StateFlow<Int> = _searchTab.asStateFlow()

    init {
        viewModelScope.launch {
            val initialSource = favoritesRepository.getFirstFavorite() ?: savedSource
            initialSource?.let(::setSelectedSource)
            _hasInitialSource.value = initialSource != null
            loadSchedule()
        }
    }

    fun loadSchedule() {
        scheduleJob?.cancel()
        scheduleJob = viewModelScope.launch {
            repository.getSchedule(week = _currentWeek.value, group = _selectedGroup.value?.id,
                teacher = _selectedTeacher.value?.id).collect { resource ->
                if (resource is Resource.Success && resource.isFromCache && _contentState.value.data != null) return@collect
                _contentState.value = _contentState.value.accept(resource)
                _scheduleState.value = _contentState.value.resource
                _isRefreshing.value = _contentState.value.isRefreshing
                if (resource is Resource.Success) {
                    _selectedDayIndex.value = _selectedDayIndex.value.coerceIn(0, (resource.data.days.size - 1).coerceAtLeast(0))
                }
            }
        }
    }

    private fun clearShownSchedule() {
        _contentState.value = ContentState()
        _scheduleState.value = Resource.Loading
        _selectedDayIndex.value = 0
    }

    fun refreshSchedule() {
        loadSchedule()
    }

    fun selectGroup(group: GroupDto) {
        if (_selectedGroup.value?.id != group.id || _selectedTeacher.value != null) clearShownSchedule()
        _selectedGroup.value = group
        _selectedTeacher.value = null
        savedSourceStore.save(group.toFavorite())
        loadSchedule()
    }

    fun selectTeacher(teacher: TeacherDto) {
        if (_selectedTeacher.value?.id != teacher.id || _selectedGroup.value != null) clearShownSchedule()
        _selectedTeacher.value = teacher
        _selectedGroup.value = null
        savedSourceStore.save(teacher.toFavorite())
        loadSchedule()
    }

    fun selectDay(index: Int) {
        _selectedDayIndex.value = index.coerceIn(0, 5)
    }

    fun setWeeklyMode(weekly: Boolean) {
        _isWeeklyMode.value = weekly
    }

    fun selectLesson(lesson: LessonItem?) {
        _selectedLesson.value = lesson
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchTab(tabIndex: Int) {
        _searchTab.value = tabIndex
    }

    /** Добавляет группу в избранное или убирает её оттуда. */
    fun toggleGroupFavorite(group: GroupDto) {
        toggleFavorite(group.toFavorite())
    }

    /** Добавляет преподавателя в избранное или убирает его оттуда. */
    fun toggleTeacherFavorite(teacher: TeacherDto) {
        toggleFavorite(teacher.toFavorite())
    }

    private fun toggleFavorite(favorite: FavoriteSource) {
        viewModelScope.launch {
            if (!favoritesRepository.toggle(favorite)) favoritesLimitEvents.trySend(Unit)
        }
    }

    /** Убирает источник из избранного. */
    fun removeFavorite(favorite: FavoriteSource) {
        viewModelScope.launch { favoritesRepository.remove(favorite) }
    }

    /** Сохраняет новую позицию после завершения перетаскивания. */
    fun moveFavorite(favorite: FavoriteSource, toIndex: Int) {
        viewModelScope.launch { favoritesRepository.move(favorite, toIndex) }
    }

    /** Делает источник из избранного текущим расписанием. */
    fun openFavorite(favorite: FavoriteSource) {
        clearShownSchedule()
        savedSourceStore.save(setSelectedSource(favorite))
        loadSchedule()
    }

    /** Восстанавливает выбор из справочника или из локально сохранённого источника. */
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
