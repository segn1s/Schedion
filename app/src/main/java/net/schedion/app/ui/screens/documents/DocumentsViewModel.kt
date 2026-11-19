package net.schedion.app.ui.screens.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.schedion.app.core.ContentState
import net.schedion.app.core.Resource
import net.schedion.app.data.repository.DocumentsRepository
import net.schedion.app.domain.model.DocumentCategory
import net.schedion.app.domain.model.PublicDocument

@HiltViewModel
class DocumentsViewModel @Inject constructor(private val repository: DocumentsRepository) : ViewModel() {
    private val _documents = MutableStateFlow(ContentState<List<DocumentCategory>>())
    val documents = _documents.asStateFlow()
    private val _document = MutableStateFlow(ContentState<PublicDocument>())
    val document = _document.asStateFlow()
    private val _category = MutableStateFlow<String?>(null)
    val category = _category.asStateFlow()
    private var listJob: Job? = null
    private var detailJob: Job? = null
    private var selectedSlug: String? = null

    init { refresh() }

    internal fun retryNetworkErrors() {
        if (_documents.value.isOffline && listJob?.isActive != true) refresh()
        if (_document.value.isOffline && detailJob?.isActive != true) refreshDocument()
    }

    fun selectCategory(title: String?) { _category.value = title }

    fun refresh() {
        listJob?.cancel()
        listJob = viewModelScope.launch {
            repository.documents().collect { result ->
                if (result is Resource.Success) {
                    if (result.isFromCache && _documents.value.data != null) return@collect
                    if (result.data.none { it.title == _category.value }) _category.value = null
                }
                _documents.value = _documents.value.accept(result)
            }
        }
    }

    fun openDocument(slug: String) {
        if (selectedSlug == slug && (_document.value.data != null || detailJob?.isActive == true)) return
        selectedSlug = slug
        _document.value = ContentState()
        refreshDocument()
    }

    fun refreshDocument() {
        val slug = selectedSlug ?: return
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            repository.document(slug).collect { result ->
                if (result is Resource.Success && result.isFromCache && _document.value.data != null) return@collect
                _document.value = _document.value.accept(result)
            }
        }
    }
}
