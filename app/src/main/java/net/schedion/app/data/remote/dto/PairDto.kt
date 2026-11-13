package net.schedion.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Ответ `GET /pairs`: расписание звонков. */
@Serializable
data class PairListDto(
    val timeZone: String,
    val items: List<PairItemDto>,
)

/** Время начала и конца одной пары. */
@Serializable
data class PairItemDto(
    val number: Int,
    val start: String,
    val end: String,
)
