package net.schedion.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Ответ `GET /documents`: документы, сгруппированные по категориям. */
@Serializable
data class DocumentListDto(
    val categories: List<DocumentCategoryDto>,
)

/** Категория документов. */
@Serializable
data class DocumentCategoryDto(
    val title: String,
    val documents: List<DocumentItemDto>,
)

/** Публичный документ с файлом. */
@Serializable
data class DocumentItemDto(
    val slug: String,
    val title: String,
    val description: String? = null,
    val audience: List<DocumentAudience>,
    val fileUrl: String,
    val fileName: String,
    val format: String,
    val size: Int,
    val updatedAt: String,
)

/** Аудитория, для которой предназначен документ. */
@Serializable
enum class DocumentAudience {
    @SerialName("students") STUDENTS,
    @SerialName("teachers") TEACHERS,
}
