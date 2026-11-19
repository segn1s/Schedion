package net.schedion.app.ui.screens.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.schedion.app.core.ContentState
import net.schedion.app.core.Resource
import net.schedion.app.data.repository.NewsRepository
import net.schedion.app.domain.model.NewsArticle
import net.schedion.app.domain.model.NewsPage

data class NewsFeedState(
    val content: ContentState<NewsPage> = ContentState(),
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val pageError: Throwable? = null,
)

@HiltViewModel
class NewsViewModel @Inject constructor(private val repository: NewsRepository) : ViewModel() {
    private val _feed = MutableStateFlow(NewsFeedState())
    val feed = _feed.asStateFlow()
    private val _article = MutableStateFlow(ContentState<NewsArticle>())
    val article = _article.asStateFlow()
    private val _blocks = MutableStateFlow<List<ArticleBlock>>(emptyList())
    val blocks = _blocks.asStateFlow()
    private val pages = sortedMapOf<Int, NewsPage>()
    private var nextOffset = 0
    private var feedJob: Job? = null
    private var pageJob: Job? = null
    private var articleJob: Job? = null
    private var selectedSlug: String? = null

    init { refresh() }

    internal fun retryNetworkErrors() {
        if (_feed.value.content.isOffline && feedJob?.isActive != true && pageJob?.isActive != true) refresh()
        if (_article.value.isOffline && articleJob?.isActive != true) refreshArticle()
    }

    fun refresh() {
        feedJob?.cancel()
        pageJob?.cancel()
        _feed.value = _feed.value.copy(content = _feed.value.content.copy(isRefreshing = true), isLoadingMore = false, pageError = null)
        feedJob = viewModelScope.launch {
            repository.feed(0, PageSize).collect { result ->
                if (result is Resource.Success) {
                    // При повторном обновлении сохранённая первая страница не стирает показанные страницы.
                    if (result.isFromCache && _feed.value.content.data != null) return@collect
                    pages.clear()
                    pages[0] = result.data
                    nextOffset = result.data.nextOffset
                }
                _feed.value = _feed.value.copy(
                    content = _feed.value.content.accept(result),
                    hasMore = if (result is Resource.Success) result.data.hasMore else _feed.value.hasMore,
                )
            }
        }
    }

    fun loadMore(retry: Boolean = false) {
        val current = _feed.value
        if (!current.hasMore || current.isLoadingMore || current.content.isRefreshing ||
            (current.pageError != null && !retry)) return
        val offset = nextOffset
        _feed.value = current.copy(isLoadingMore = true, pageError = null)
        pageJob = viewModelScope.launch {
            repository.feed(offset, PageSize).collect { result ->
                when (result) {
                    Resource.Loading -> Unit
                    is Resource.Success -> {
                        pages[offset] = result.data
                        val merged = NewsPage(pages.values.flatMap { it.items }.distinctBy { it.slug }, result.data.total, 0)
                        nextOffset = result.data.nextOffset
                        val savedAt = listOfNotNull(_feed.value.content.savedAt, result.savedAt).minOrNull()
                        _feed.value = _feed.value.copy(
                            content = _feed.value.content.accept(Resource.Success(merged, result.isFromCache, savedAt))
                                .copy(isRefreshing = false),
                            isLoadingMore = result.isFromCache,
                            hasMore = result.data.hasMore,
                        )
                    }
                    is Resource.Error -> _feed.value = _feed.value.copy(
                        content = _feed.value.content.accept(result),
                        pageError = result.cause ?: IllegalStateException(),
                        isLoadingMore = false,
                    )
                }
            }
        }
    }

    fun openArticle(slug: String) {
        if (selectedSlug == slug && (_article.value.data != null || articleJob?.isActive == true)) return
        selectedSlug = slug
        _article.value = ContentState()
        _blocks.value = emptyList()
        refreshArticle()
    }

    fun refreshArticle() {
        val slug = selectedSlug ?: return
        articleJob?.cancel()
        articleJob = viewModelScope.launch {
            repository.article(slug).collect { result ->
                if (result is Resource.Success) {
                    if (result.isFromCache && _article.value.data != null) return@collect
                    _blocks.value = withContext(Dispatchers.Default) { parseArticleHtml(result.data.html) }
                }
                _article.value = _article.value.accept(result)
            }
        }
    }

    private companion object {
        const val PageSize = 5
    }
}
