package net.schedion.app.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    /** true, если источник расписания уже был выбран при прошлом запуске. */
    val hasSavedSource: Boolean = savedSource != null

    val availableGroups: StateFlow<List<GroupDto>> = repository.availableGroups
    val availableTeachers: StateFlow<List<TeacherDto>> = repository.availableTeachers

    /** Избранные группы и преподаватели в порядке добавления. */
    val favorites: StateFlow<List<FavoriteSource>> = favoritesRepository.favorites

    private val _selectedGroup = MutableStateFlow<GroupDto?>(null)
    val selectedGroup: StateFlow<GroupDto?> = _selectedGroup.asStateFlow()

    private val _selectedTeacher = MutableStateFlow<TeacherDto?>(null)
    val selectedTeacher: StateFlow<TeacherDto?> = _selectedTeacher.asStateFlow()

    private val _currentWeek = MutableStateFlow<String?>(null)
    val currentWeek: StateFlow<String?> = _currentWeek.asStateFlow()

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
        // Восстанавливаем выбор прошлого запуска, чтобы открывать сразу своё расписание
        savedSource?.let { saved ->
            when (saved.kind) {
                FavoriteKind.Group -> _selectedGroup.value = GroupDto(
                    id = saved.id,
                    name = saved.title,
                    description = saved.subtitle,
                    course = saved.course ?: 0,
                )
                FavoriteKind.Teacher -> _selectedTeacher.value = TeacherDto(
                    id = saved.id,
                    shortName = saved.title,
                )
            }
        }
        loadSchedule()
    }

    fun loadSchedule() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val groupId = _selectedGroup.value?.id
            val teacherId = _selectedTeacher.value?.id
            val week = _currentWeek.value

            repository.getSchedule(week = week, group = groupId, teacher = teacherId)
                .collect { resource ->
                    _scheduleState.value = resource
                    if (resource is Resource.Success && _selectedGroup.value == null && _selectedTeacher.value == null) {
                        repository.availableGroups.value.firstOrNull()?.let { group ->
                            _selectedGroup.value = group
                        }
                    }
                    _isRefreshing.value = false
                }
        }
    }

    fun refreshSchedule() {
        loadSchedule()
    }

    fun selectGroup(group: GroupDto) {
        _selectedGroup.value = group
        _selectedTeacher.value = null
        savedSourceStore.save(group.toFavorite())
        loadSchedule()
    }

    fun selectTeacher(teacher: TeacherDto) {
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
        viewModelScope.launch { favoritesRepository.toggle(group.toFavorite()) }
    }

    /** Добавляет преподавателя в избранное или убирает его оттуда. */
    fun toggleTeacherFavorite(teacher: TeacherDto) {
        viewModelScope.launch { favoritesRepository.toggle(teacher.toFavorite()) }
    }

    /** Убирает источник из избранного. */
    fun removeFavorite(favorite: FavoriteSource) {
        viewModelScope.launch { favoritesRepository.remove(favorite) }
    }

    /** Делает источник из избранного текущим расписанием. */
    fun openFavorite(favorite: FavoriteSource) {
        when (favorite.kind) {
            FavoriteKind.Group -> {
                val group = availableGroups.value.firstOrNull { it.id == favorite.id }
                    ?: GroupDto(
                        id = favorite.id,
                        name = favorite.title,
                        description = favorite.subtitle,
                        course = favorite.course ?: 0,
                    )
                selectGroup(group)
            }
            FavoriteKind.Teacher -> {
                val teacher = availableTeachers.value.firstOrNull { it.id == favorite.id }
                    ?: TeacherDto(
                        id = favorite.id,
                        shortName = favorite.title,
                    )
                selectTeacher(teacher)
            }
        }
    }
}
