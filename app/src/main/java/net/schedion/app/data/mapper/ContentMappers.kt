package net.schedion.app.data.mapper

import net.schedion.app.data.remote.dto.*
import net.schedion.app.domain.model.*

fun NewsFeedDto.toDomain(): NewsPage = NewsPage(items.map { it.toDomain() }, total, offset)
fun NewsListItemDto.toDomain(): NewsItem = NewsItem(slug, title, summary, date, contentUrl(coverUrl))
fun NewsArticleDto.toDomain(): NewsArticle = NewsArticle(
    slug, title, summary, date, contentUrl(coverUrl), html,
    sourceUrl?.let(::articleLink),
    files.mapNotNull { file -> contentUrl(file.url)?.let { url ->
        ContentFile("news:$slug:${file.url}", publishedAt, url, file.fileName,
            fileMimeType(file.contentType, file.fileName), file.size.toLong())
    } },
)
fun DocumentItemDto.toDomain(): PublicDocument = PublicDocument(
    slug, title, description, audience.map { it.name }, format, updatedAt,
    ContentFile("document:$slug", updatedAt, contentUrl(fileUrl).orEmpty(), fileName,
        fileMimeType(format, fileName), size.toLong()),
)
fun DocumentListDto.toDomain(): List<DocumentCategory> = categories.map { category ->
    DocumentCategory(category.title, category.documents.map { it.toDomain() })
}
