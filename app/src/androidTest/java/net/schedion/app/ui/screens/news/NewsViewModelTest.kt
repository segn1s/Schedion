package net.schedion.app.ui.screens.news

import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.schedion.app.core.Resource
import net.schedion.app.data.repository.NewsRepository
import net.schedion.app.domain.model.NewsArticle
import net.schedion.app.domain.model.NewsItem
import net.schedion.app.domain.model.NewsPage
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NewsViewModelTest {
    private class Repository : NewsRepository {
        var fail = false
        override fun feed(offset: Int, count: Int): Flow<Resource<NewsPage>> = flow {
            emit(Resource.Loading)
            if (fail) emit(Resource.Error("", IOException()))
            else {
                val indices = if (offset == 0) 0..4 else 4..6
                emit(Resource.Success(NewsPage(indices.map { item(it) }, 7, offset), false, 123))
            }
        }
        override fun article(slug: String): Flow<Resource<NewsArticle>> = flow {
            emit(Resource.Loading)
            delay(100)
            emit(Resource.Success(NewsArticle(slug, slug, "", "2026-11-19", null, "<p>Body</p>", null, emptyList()), false, 123))
        }
        private fun item(id: Int) = NewsItem("item-$id", "Title $id", "Summary", "2026-11-19", null)
    }

    @Test fun paginationDeduplicatesAndFailedRefreshKeepsAllShownItems(): Unit = runBlocking {
        val repo = Repository()
        val store = ViewModelStore()
        val vm = withContext(Dispatchers.Main) { NewsViewModel(repo).also { store.put("news", it) } }
        try {
            await { vm.feed.value.content.data?.items?.size == 5 }
            withContext(Dispatchers.Main) { vm.loadMore() }
            await { vm.feed.value.content.data?.items?.size == 7 }
            assertEquals(7, vm.feed.value.content.data!!.items.distinctBy { it.slug }.size)
            assertFalse(vm.feed.value.hasMore)
            repo.fail = true
            withContext(Dispatchers.Main) { vm.refresh() }
            await { vm.feed.value.content.error != null }
            assertEquals(7, vm.feed.value.content.data!!.items.size)
            assertTrue(vm.feed.value.content.isOffline)
            repo.fail = false
            withContext(Dispatchers.Main) { vm.retryNetworkErrors() }
            await { vm.feed.value.content.error == null && !vm.feed.value.content.isRefreshing }
            assertEquals(5, vm.feed.value.content.data!!.items.size)
        } finally { withContext(Dispatchers.Main) { store.clear() } }
    }

    @Test fun selectingAnotherArticleCancelsThePreviousRequest(): Unit = runBlocking {
        val store = ViewModelStore()
        val vm = withContext(Dispatchers.Main) { NewsViewModel(Repository()).also { store.put("news", it) } }
        try {
            withContext(Dispatchers.Main) { vm.openArticle("one"); vm.openArticle("two") }
            await { vm.article.value.data != null }
            assertEquals("two", vm.article.value.data!!.slug)
            assertTrue(vm.blocks.value.isNotEmpty())
        } finally { withContext(Dispatchers.Main) { store.clear() } }
    }

    private suspend fun await(predicate: () -> Boolean) = withTimeout(5000) {
        while (!predicate()) delay(20)
    }
}
