package net.schedion.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Ответ `GET /home`: сводные данные главного экрана. */
@Serializable
data class HomeContentDto(
    val banner: BannerDto? = null,
    val weekHighlight: WeekHighlightDto? = null,
    val events: List<EventDto>,
    val nextApplicantEvent: EventDto? = null,
    val documents: List<DocumentItemDto>,
)

/** Информационный баннер главной страницы. */
@Serializable
data class BannerDto(
    val text: String,
    val linkUrl: String? = null,
    val linkText: String? = null,
)

/** Выделенная неделя семестра. */
@Serializable
data class WeekHighlightDto(
    val weekStart: String,
    val title: String,
    val description: String? = null,
    val linkUrl: String,
    val linkText: String,
)

/** Событие из афиши. */
@Serializable
data class EventDto(
    val title: String,
    val date: String,
    val time: String? = null,
    val place: String? = null,
    val linkUrl: String? = null,
    val forApplicants: Boolean,
)
