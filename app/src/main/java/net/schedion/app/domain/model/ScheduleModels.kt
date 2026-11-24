package net.schedion.app.domain.model

/** Статус проведения занятия относительно текущего времени. */
enum class LessonStatus {
    Past,
    Current,
    Cancelled,
    Upcoming,
}

/** Доменная модель одного занятия в расписании. */
data class LessonItem(
    val id: Int,
    val date: String,
    val pairNumber: Int,
    val timeStart: String,
    val timeEnd: String,
    val kind: String,
    val discipline: String,
    val room: String? = null,
    val subgroup: Int? = null,
    val groupName: String? = null,
    val teachers: String = "",
    val status: LessonStatus = LessonStatus.Upcoming,
    val cancellationReason: String? = null,
    val associationName: String? = null,
)

/** Занятия одного учебного дня. */
data class ScheduleDay(
    val date: String,
    val dayOfWeek: String,
    val dayNumber: String,
    val isToday: Boolean = false,
    val lessons: List<LessonItem> = emptyList(),
)

/** Полное расписание на учебную неделю. */
data class ScheduleWeek(
    val weekStart: String,
    val weekEnd: String,
    val days: List<ScheduleDay>,
    val isFromCache: Boolean = false,
    val cachedTimestamp: String? = null,
)

/** Расписание звонков пары. */
data class PairItem(
    val number: Int,
    val start: String,
    val end: String,
)

/** Статус текущей пары. */
data class CurrentPairStatus(
    val pairNumber: Int,
    val timeEnd: String,
    val remainingMinutes: Long,
)

/** Статус перемены перед следующей парой. */
data class BreakStatus(
    val nextPairNumber: Int,
    val nextPairStart: String,
    val remainingMinutes: Long,
)

/** Учебная неделя семестра. */
data class WeekInfo(
    val startDate: String,
    val endDate: String,
    val number: Int,
    val isRed: Boolean,
    val isCurrent: Boolean = false,
)
