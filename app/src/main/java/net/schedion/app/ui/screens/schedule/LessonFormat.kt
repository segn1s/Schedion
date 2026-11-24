package net.schedion.app.ui.screens.schedule

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RuLocale: Locale = Locale.forLanguageTag("ru")
private val FullDateFormatter = DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", RuLocale)
private val DayMonthFormatter = DateTimeFormatter.ofPattern("dd.MM", RuLocale)
private val FullShortYearFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", RuLocale)
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm", RuLocale)

/** Полная дата дня расписания: «Понедельник, 09.11.2026». */
internal fun formatFullDate(isoDate: String): String = runCatching {
    LocalDate.parse(isoDate).format(FullDateFormatter).replaceFirstChar { it.uppercase(RuLocale) }
}.getOrDefault(isoDate)

/** Короткая дата дня расписания: «09.11». */
internal fun formatShortDate(isoDate: String): String = runCatching {
    LocalDate.parse(isoDate).format(DayMonthFormatter)
}.getOrDefault(isoDate)

/** Диапазон недели: «09.11 – 15.11.2026», год указывается только во второй дате. */
internal fun formatWeekRange(startIso: String, endIso: String): String = runCatching {
    "${LocalDate.parse(startIso).format(DayMonthFormatter)} – ${LocalDate.parse(endIso).format(FullShortYearFormatter)}"
}.getOrDefault("$startIso – $endIso")

/** Время последнего обновления расписания: «10:42». */
internal fun formatUpdateTime(timestamp: Long): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).format(TimeFormatter)

/**
 * Пара проходит дистанционно: сервер помечает это в поле аудитории.
 * В этом случае аудиторию не показываем, а выводим пометку «Дистант».
 */
internal fun isDistantRoom(room: String?): Boolean {
    val value = room?.trim()?.lowercase(RuLocale) ?: return false
    return value.startsWith("дистан") ||
        value == "онлайн" ||
        value.contains("online") ||
        value.contains("distance")
}
