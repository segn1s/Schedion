package net.schedion.app.ui.screens.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.schedion.app.core.Resource
import net.schedion.app.data.remote.dto.NewsFeedDto
import net.schedion.app.data.repository.ScheduleRepository
import javax.inject.Inject

/** ViewModel ленты новостей. */
@HiltViewModel
class NewsViewModel @Inject constructor(
    private val repository: ScheduleRepository,
) : ViewModel() {

    private val _newsFeed = MutableStateFlow<Resource<NewsFeedDto>>(Resource.Loading)
    val newsFeed: StateFlow<Resource<NewsFeedDto>> = _newsFeed.asStateFlow()

    init {
        loadNews()
    }

    fun loadNews() {
        viewModelScope.launch {
            repository.getNews().collect {
                _newsFeed.value = it
            }
        }
    }
}
