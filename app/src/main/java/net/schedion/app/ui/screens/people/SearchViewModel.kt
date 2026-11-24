package net.schedion.app.ui.screens.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.schedion.app.core.ContentState
import net.schedion.app.core.Resource
import net.schedion.app.data.mapper.toFavorite
import net.schedion.app.data.remote.dto.GroupDto
import net.schedion.app.data.remote.dto.ScheduleFiltersDto
import net.schedion.app.data.remote.dto.TeacherDto
import net.schedion.app.data.repository.FavoritesRepository
import net.schedion.app.data.repository.ScheduleRepository
import net.schedion.app.domain.model.FavoriteSource

/** ViewModel экрана поиска групп и преподавателей. */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: ScheduleRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    val availableGroups: StateFlow<List<GroupDto>> = repository.availableGroups
    val availableTeachers: StateFlow<List<TeacherDto>> = repository.availableTeachers
    val groupsSavedAt: StateFlow<Long?> = repository.groupsSavedAt
    val teachersSavedAt: StateFlow<Long?> = repository.teachersSavedAt

    private val _directoryState = MutableStateFlow(ContentState<ScheduleFiltersDto>())
    val directoryState: StateFlow<ContentState<ScheduleFiltersDto>> = _directoryState.asStateFlow()

    private var directoryJob: Job? = null

    val favorites: StateFlow<List<FavoriteSource>> = favoritesRepository.favorites
    val favoritesLimit = FavoritesRepository.MaxFavorites

    private val favoritesLimitEvents = Channel<Unit>(Channel.CONFLATED)
    val favoritesLimitReached = favoritesLimitEvents.receiveAsFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchTab = MutableStateFlow(0) // 0 - Groups, 1 - Teachers
    val searchTab: StateFlow<Int> = _searchTab.asStateFlow()

    init {
        refreshDirectories(refreshAllWeeks = false)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchTab(tabIndex: Int) {
        _searchTab.value = tabIndex
    }

    fun refreshDirectories(refreshAllWeeks: Boolean) {
        directoryJob?.cancel()
        directoryJob = viewModelScope.launch {
            repository.getDirectories(refreshAllWeeks).collect { result ->
                val current = _directoryState.value
                if (result is Resource.Success && result.isFromCache && current.data != null) return@collect
                _directoryState.value = current.accept(result)
            }
        }
    }

    fun toggleGroupFavorite(group: GroupDto) {
        toggleFavorite(group.toFavorite())
    }

    fun toggleTeacherFavorite(teacher: TeacherDto) {
        toggleFavorite(teacher.toFavorite())
    }

    private fun toggleFavorite(favorite: FavoriteSource) {
        viewModelScope.launch {
            if (!favoritesRepository.toggle(favorite)) {
                favoritesLimitEvents.trySend(Unit)
            }
        }
    }
}
