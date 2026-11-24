package net.schedion.app.domain.model

import java.net.URI
import java.security.MessageDigest

private const val ContentOrigin = "https://schedion.ru/"

/** Разрешает относительные ссылки API; для файлов и изображений допускает только HTTP(S). */
fun contentUrl(value: String?): String? = try {
    value?.takeIf { it.isNotBlank() }?.let {
        URI(ContentOrigin).resolve(it.trim()).takeIf { uri ->
            uri.scheme?.lowercase() in listOf("https", "http") && !uri.host.isNullOrBlank()
        }?.toASCIIString()
    }
} catch (_: IllegalArgumentException) {
    null
} catch (_: java.net.URISyntaxException) {
    null
}

/** Разрешённые внешние ссылки статьи: веб, Telegram, почта и телефон. */
fun articleLink(value: String): String? {
    contentUrl(value)?.let { return it }
    return try {
        URI(value).takeIf { it.scheme?.lowercase() in listOf("tg", "mailto", "tel") }?.toASCIIString()
    } catch (_: Exception) { null }
}

/** Файл и его версия: документ идентифицируется slug + updatedAt, вложение — статьёй и URL. */
data class ContentFile(
    val id: String,
    val version: String,
    val url: String,
    val fileName: String,
    val mimeType: String,
    val size: Long,
) {
    val key: String get() = sha256("$id|$version|$url")
}

fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

/** MIME из API, формата или расширения; неизвестные файлы открываются как бинарные. */
fun fileMimeType(format: String, fileName: String): String {
    val value = format.substringBefore(';').lowercase().trim()
    if (value != "application/octet-stream" && Regex("[a-z0-9.+-]+/[a-z0-9.+-]+").matches(value)) return value
    fun byExtension(extension: String): String? = when (extension) {
        "pdf" -> "application/pdf"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        else -> null
    }
    return byExtension(value) ?: byExtension(fileName.substringAfterLast('.', "").lowercase()) ?: "application/octet-stream"
}

/** Удаляет каталоги и недопустимые символы из имени, полученного от сервера. */
fun safeFileName(name: String): String = name.substringAfterLast('/').substringAfterLast('\\')
    .replace(Regex("[\\p{Cntrl}<>:\"/\\\\|?*]"), "_").trim().trim('.').take(120).ifBlank { "document" }

data class NewsItem(val slug: String, val title: String, val summary: String, val date: String, val coverUrl: String?)
data class NewsPage(val items: List<NewsItem>, val total: Int, val offset: Int) {
    val nextOffset: Int get() = offset + items.size
    val hasMore: Boolean get() = items.isNotEmpty() && nextOffset < total
}
data class NewsArticle(
    val slug: String, val title: String, val summary: String, val date: String,
    val coverUrl: String?, val html: String, val sourceUrl: String?, val files: List<ContentFile>,
)
data class PublicDocument(
    val slug: String, val title: String, val description: String?, val audiences: List<String>,
    val format: String, val updatedAt: String, val file: ContentFile,
)
data class DocumentCategory(val title: String, val documents: List<PublicDocument>)
