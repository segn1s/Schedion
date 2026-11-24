package net.schedion.app.ui.screens.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.schedion.app.data.repository.FavoritesRepository
import net.schedion.app.domain.model.FavoriteSource

/** Модель удалённого элемента для возможности отмены (Undo). */
data class RemovedFavorite(
    val favorite: FavoriteSource,
    val originalIndex: Int,
)

/** ViewModel экрана управления избранным с черновиком порядка и отменой удаления. */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    val favoritesLimit = FavoritesRepository.MaxFavorites

    private val _draftFavorites = MutableStateFlow<List<FavoriteSource>>(emptyList())
    val draftFavorites: StateFlow<List<FavoriteSource>> = _draftFavorites.asStateFlow()

    private val _lastRemoved = MutableStateFlow<RemovedFavorite?>(null)
    val lastRemoved: StateFlow<RemovedFavorite?> = _lastRemoved.asStateFlow()

    /** true, пока в черновике есть несохранённые правки: тогда список из Room его не перетирает. */
    private var hasEdits = false

    init {
        viewModelScope.launch {
            favoritesRepository.favorites.collect { list ->
                if (!hasEdits) _draftFavorites.value = list
            }
        }
    }

    /** Сбрасывает черновик к сохранённому списку избранного при открытии экрана. */
    fun resetDraft() {
        hasEdits = false
        _draftFavorites.value = favoritesRepository.favorites.value
        _lastRemoved.value = null
    }

    fun moveFavorite(source: FavoriteSource, destination: Int) {
        val current = _draftFavorites.value.toMutableList()
        val fromIndex = current.indexOf(source)
        if (fromIndex < 0 || fromIndex == destination) return
        val target = destination.coerceIn(current.indices)
        current.add(target, current.removeAt(fromIndex))
        hasEdits = true
        _draftFavorites.value = current
    }

    fun removeFavorite(source: FavoriteSource): RemovedFavorite? {
        val current = _draftFavorites.value.toMutableList()
        val index = current.indexOf(source)
        if (index < 0) return null
        current.removeAt(index)
        hasEdits = true
        _draftFavorites.value = current
        val removed = RemovedFavorite(source, index)
        _lastRemoved.value = removed
        return removed
    }

    fun undoRemove(): FavoriteSource? {
        val removed = _lastRemoved.value ?: return null
        val current = _draftFavorites.value.toMutableList()
        if (current.size >= favoritesLimit) return null
        val insertIndex = removed.originalIndex.coerceIn(0, current.size)
        current.add(insertIndex, removed.favorite)
        hasEdits = true
        _draftFavorites.value = current
        _lastRemoved.value = null
        return removed.favorite
    }

    fun clearLastRemoved() {
        _lastRemoved.value = null
    }

    fun saveFavorites() {
        val draft = _draftFavorites.value
        hasEdits = false
        viewModelScope.launch {
            favoritesRepository.saveFavorites(draft)
        }
    }
}
