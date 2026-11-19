package net.schedion.app.data.repository

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import net.schedion.app.core.Resource
import net.schedion.app.data.local.entity.ContentCacheEntity
import net.schedion.app.data.remote.SchedionApiService
import net.schedion.app.data.remote.SchedionJson
import net.schedion.app.data.remote.dto.*
import org.junit.Assert.*
import org.junit.Test

class ContentRepositoryTest {
    private class Api : SchedionApiService {
        var fail = false
        var feed = NewsFeedDto(listOf(item("first")), 8, 0, 5)
        var article = NewsArticleDto("first", "Title", "Summary", "2026-11-19", "2026-11-19T10:00:00Z", "/files/cover", "<p>Text</p>")
        var documents = DocumentListDto(emptyList())
        override suspend fun getNews(offset: Int?, count: Int?): NewsFeedDto {
            if (fail) throw IOException()
            return feed.copy(offset = offset ?: 0)
        }
        override suspend fun getNewsArticle(slug: String): NewsArticleDto {
            if (fail) throw IOException()
            return article.copy(slug = slug)
        }
        override suspend fun getDocuments(): DocumentListDto { if (fail) throw IOException(); return documents }
        override suspend fun getDocument(slug: String): DocumentItemDto {
            if (fail) throw IOException()
            return documents.categories.flatMap { it.documents }.first { it.slug == slug }
        }
        override suspend fun getSchedule(week: String?, group: Int?, course: Int?, teacher: Int?): ScheduleWeekDto = error("unused")
        override suspend fun getWeeks(): WeekListDto = error("unused")
        override suspend fun getPairs(): PairListDto = error("unused")
        override suspend fun getHome(): HomeContentDto = error("unused")
    }

    @Test fun feedPages_areCachedSeparatelyAndMappedWithRelativeCoverUrls() = runBlocking {
        val api = Api()
        val repository = NewsRepositoryImpl(api, CachedContent(InMemoryContentCacheDao(), SchedionJson))
        val first = repository.feed(0, 5).toList().last() as Resource.Success
        assertEquals("https://schedion.ru/files/first", first.data.items.first().coverUrl)
        api.feed = NewsFeedDto(listOf(item("second")), 8, 5, 5)
        repository.feed(5, 5).toList()
        api.fail = true
        val offlineFirst = repository.feed(0, 5).toList()
        val offlineSecond = repository.feed(5, 5).toList()
        assertEquals("first", (offlineFirst[1] as Resource.Success).data.items.first().slug)
        assertEquals("second", (offlineSecond[1] as Resource.Success).data.items.first().slug)
        assertTrue((offlineSecond[1] as Resource.Success).isFromCache)
        assertTrue(offlineSecond.last() is Resource.Error)
        assertTrue(repository.feed(10, 5).toList().none { it is Resource.Success })
    }

    @Test fun articles_keepOwnSnapshotsAndFileVersions() = runBlocking {
        val api = Api()
        api.article = api.article.copy(files = listOf(FileItemDto("/files/word", "letter.docx", "application/octet-stream", 123, false)))
        val repository = NewsRepositoryImpl(api, CachedContent(InMemoryContentCacheDao(), SchedionJson))
        val article = (repository.article("first").toList().last() as Resource.Success).data
        assertEquals("https://schedion.ru/files/word", article.files.single().url)
        assertTrue(article.files.single().mimeType.endsWith("wordprocessingml.document"))
        api.fail = true
        assertTrue(repository.article("first").toList()[1] is Resource.Success)
        assertTrue(repository.article("other").toList().none { it is Resource.Success })
    }

    @Test fun documents_mapCategoriesAudiencesAndVersionedFiles() = runBlocking {
        val api = Api()
        api.documents = DocumentListDto(listOf(DocumentCategoryDto("Category", listOf(
            DocumentItemDto("form", "Form", null, listOf(DocumentAudience.STUDENTS), "/files/form", "form.pdf", "PDF", 123, "2026-11-19"),
        ))))
        val repository = DocumentsRepositoryImpl(api, CachedContent(InMemoryContentCacheDao(), SchedionJson))
        val list = (repository.documents().toList().last() as Resource.Success).data
        assertEquals("Category", list.single().title)
        val document = (repository.document("form").toList().last() as Resource.Success).data
        assertEquals(listOf("STUDENTS"), document.audiences)
        assertEquals("application/pdf", document.file.mimeType)
        assertEquals("https://schedion.ru/files/form", document.file.url)
        api.fail = true
        assertTrue(repository.documents().toList()[1] is Resource.Success)
        assertTrue(repository.document("form").toList()[1] is Resource.Success)
    }

    @Test fun emptyDocumentList_isCachedAndRemainsDistinguishableFromNetworkFailure() = runBlocking {
        val api = Api()
        val repository = DocumentsRepositoryImpl(api, CachedContent(InMemoryContentCacheDao(), SchedionJson))
        assertTrue((repository.documents().toList().last() as Resource.Success).data.isEmpty())
        api.fail = true
        val cached = repository.documents().toList()
        assertEquals(3, cached.size)
        assertTrue((cached[1] as Resource.Success).data.isEmpty())
        assertTrue((cached.last() as Resource.Error).cause is IOException)
    }

    @Test fun corruptSnapshot_isIgnoredAndReplacedByFreshResponse() = runBlocking {
        val dao = InMemoryContentCacheDao()
        dao.put(ContentCacheEntity("news:feed:0:5", "{broken", 1))
        val repository = NewsRepositoryImpl(Api(), CachedContent(dao, SchedionJson))
        val states = repository.feed(0, 5).toList()
        assertEquals(2, states.size)
        assertTrue(states.last() is Resource.Success)
        assertTrue(checkNotNull(dao.get("news:feed:0:5")).savedAt > 1)
    }

    @Test(expected = CancellationException::class)
    fun cancellation_isNotConvertedToNetworkError(): Unit = runBlocking {
        CachedContent(InMemoryContentCacheDao(), SchedionJson)
            .request("cancel", NewsFeedDto.serializer()) { throw CancellationException() }.toList()
        Unit
    }

    private companion object {
        fun item(slug: String) = NewsListItemDto(slug, "Title", "Summary", "2026-11-19", "2026-11-19T10:00:00Z", "/files/$slug")
    }
}
