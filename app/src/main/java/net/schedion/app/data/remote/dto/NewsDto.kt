package net.schedion.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Ответ `GET /news`: страница новостной ленты. */
@Serializable
data class NewsFeedDto(
    val items: List<NewsListItemDto>,
    val total: Int,
    val offset: Int,
    val count: Int,
)

/** Краткая карточка новости в ленте. */
@Serializable
data class NewsListItemDto(
    val slug: String,
    val title: String,
    val summary: String,
    val date: String,
    val publishedAt: String,
    val coverUrl: String? = null,
)

/** Ответ `GET /news/{slug}`: полная новость с HTML-содержимым. */
@Serializable
data class NewsArticleDto(
    val slug: String,
    val title: String,
    val summary: String,
    val date: String,
    val publishedAt: String,
    val coverUrl: String? = null,
    val html: String,
    val sourceUrl: String? = null,
    val files: List<FileItemDto> = emptyList(),
)

/** Вложенный файл новости. */
@Serializable
data class FileItemDto(
    val url: String,
    val fileName: String,
    val contentType: String,
    val size: Int,
    val isImage: Boolean,
)
