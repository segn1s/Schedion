package net.schedion.app.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.schedion.app.core.Resource
import net.schedion.app.data.mapper.toDomain
import net.schedion.app.data.remote.SchedionApiService
import net.schedion.app.data.remote.dto.NewsArticleDto
import net.schedion.app.data.remote.dto.NewsFeedDto
import net.schedion.app.domain.model.NewsArticle
import net.schedion.app.domain.model.NewsPage

interface NewsRepository {
    fun feed(offset: Int, count: Int): Flow<Resource<NewsPage>>
    fun article(slug: String): Flow<Resource<NewsArticle>>
}

@Singleton
class NewsRepositoryImpl @Inject constructor(private val api: SchedionApiService, private val cache: CachedContent) : NewsRepository {
    override fun feed(offset: Int, count: Int): Flow<Resource<NewsPage>> =
        cache.request("news:feed:$offset:$count", NewsFeedDto.serializer()) { api.getNews(offset, count) }
            .map { it.mapData { dto -> dto.toDomain() } }
    override fun article(slug: String): Flow<Resource<NewsArticle>> =
        cache.request("news:article:$slug", NewsArticleDto.serializer()) { api.getNewsArticle(slug) }
            .map { it.mapData { dto -> dto.toDomain() } }
}

/** Меняет только модель данных, сохраняя метаданные кэша и исходную ошибку. */
internal inline fun <T, R> Resource<T>.mapData(transform: (T) -> R): Resource<R> = when (this) {
    Resource.Loading -> Resource.Loading
    is Resource.Error -> this
    is Resource.Success -> Resource.Success(transform(data), isFromCache, savedAt)
}
