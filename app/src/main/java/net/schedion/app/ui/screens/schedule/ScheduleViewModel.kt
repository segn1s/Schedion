package net.schedion.app.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.schedion.app.core.Resource
import net.schedion.app.data.repository.ScheduleRepository
import net.schedion.app.domain.model.ScheduleWeek
import javax.inject.Inject

/**
 * ViewModel экрана расписания.
 *
 * Отвечает за загрузку данных через [ScheduleRepository], управление состоянием
 * текущего выбранного дня, переключение между режимами «День/Неделя» и обновление данных.
 */
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: ScheduleRepository,
) : ViewModel() {

    private val _scheduleState = MutableStateFlow<Resource<ScheduleWeek>>(Resource.Loading)
    val scheduleState: StateFlow<Resource<ScheduleWeek>> = _scheduleState.asStateFlow()

    private val _selectedDayIndex = MutableStateFlow(0)
    val selectedDayIndex: StateFlow<Int> = _selectedDayIndex.asStateFlow()

    private val _isWeeklyMode = MutableStateFlow(false)
    val isWeeklyMode: StateFlow<Boolean> = _isWeeklyMode.asStateFlow()

    private val _isTeacherMode = MutableStateFlow(false)
    val isTeacherMode: StateFlow<Boolean> = _isTeacherMode.asStateFlow()

    private val _currentTitle = MutableStateFlow("2ИВТ_ТИИ1")
    val currentTitle: StateFlow<String> = _currentTitle.asStateFlow()

    init {
        loadSchedule()
    }

    fun loadSchedule(week: String? = null) {
        viewModelScope.launch {
            repository.getSchedule(week = week).collect { resource ->
                _scheduleState.value = resource
            }
        }
    }

    fun refreshSchedule() {
        loadSchedule()
    }

    fun selectDay(index: Int) {
        _selectedDayIndex.value = index
    }

    fun setWeeklyMode(weekly: Boolean) {
        _isWeeklyMode.value = weekly
    }

    fun setTeacherMode(teacher: Boolean, name: String = "Петров Алексей Сергеевич") {
        _isTeacherMode.value = teacher
        _currentTitle.value = if (teacher) name else "2ИВТ_ТИИ1"
        loadSchedule()
    }
}
