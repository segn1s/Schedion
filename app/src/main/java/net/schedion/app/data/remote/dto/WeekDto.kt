package net.schedion.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Ответ `GET /weeks`: семестр и учебные недели с чётностью. */
@Serializable
data class WeekListDto(
    val semester: SemesterDto,
    val currentWeekStart: String,
    val defaultWeekStart: String,
    val items: List<WeekItemDto>,
)

/** Границы семестра. */
@Serializable
data class SemesterDto(
    val start: String,
    val end: String,
    val title: String,
)

/** Учебная неделя с номером и цветом. */
@Serializable
data class WeekItemDto(
    val start: String,
    val end: String,
    val number: Int,
    val color: WeekColor,
)

/** Цвет (чётность) учебной недели. */
@Serializable
enum class WeekColor {
    @SerialName("black") BLACK,
    @SerialName("red") RED,
}
