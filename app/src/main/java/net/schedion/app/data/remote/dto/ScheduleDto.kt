package net.schedion.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Ответ `GET /schedule`: занятия и отмены на выбранную учебную неделю. */
@Serializable
data class ScheduleWeekDto(
    val weekStart: String,
    val weekEnd: String,
    val lessons: List<ScheduleLessonDto>,
    val cancellations: List<ScheduleCancellationDto>,
    val filters: ScheduleFiltersDto,
    val nextLessonDate: String? = null,
)

/** Одно занятие в расписании. */
@Serializable
data class ScheduleLessonDto(
    val id: Int,
    val date: String,
    val pair: Int,
    val kind: LessonKind,
    val disciplineId: Int,
    val discipline: String,
    val room: String? = null,
    val subgroup: Int? = null,
    val groupId: Int? = null,
    val groupName: String? = null,
    val course: Int? = null,
    val associationName: String? = null,
    val associationGroupIds: List<Int> = emptyList(),
    val teachers: List<ScheduleTeacherDto> = emptyList(),
    val cancellationReason: String? = null,
)

/** Вид занятия. */
@Serializable
enum class LessonKind {
    @SerialName("none") NONE,
    @SerialName("lecture") LECTURE,
    @SerialName("seminar") SEMINAR,
    @SerialName("laboratory") LABORATORY,
    @SerialName("practical") PRACTICAL,
    @SerialName("selfStudy") SELF_STUDY,
}

/** Преподаватель занятия в сокращённом виде. */
@Serializable
data class ScheduleTeacherDto(
    val id: Int,
    val shortName: String,
)

/** Отмена занятий на диапазон дат. */
@Serializable
data class ScheduleCancellationDto(
    val startDate: String,
    val endDate: String,
    val pairs: List<Int>,
    val reason: String,
    val scope: String,
    val groupId: Int? = null,
    val course: Int? = null,
)

/** Справочники, доступные для фильтрации расписания. */
@Serializable
data class ScheduleFiltersDto(
    val groups: List<GroupDto>,
    val teachers: List<TeacherDto>,
    val courses: List<Int>,
)

/** Учебная группа. */
@Serializable
data class GroupDto(
    val id: Int,
    val name: String,
    val description: String? = null,
    val course: Int,
)

/** Преподаватель из справочника. */
@Serializable
data class TeacherDto(
    val id: Int,
    val fullName: String,
    val shortName: String? = null,
)
