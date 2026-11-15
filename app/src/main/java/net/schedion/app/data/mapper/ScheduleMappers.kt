package net.schedion.app.data.mapper

import net.schedion.app.data.local.entity.LessonEntity
import net.schedion.app.data.local.entity.PairEntity
import net.schedion.app.data.local.entity.WeekEntity
import net.schedion.app.data.remote.dto.LessonKind
import net.schedion.app.data.remote.dto.PairItemDto
import net.schedion.app.data.remote.dto.ScheduleLessonDto
import net.schedion.app.data.remote.dto.WeekColor
import net.schedion.app.data.remote.dto.WeekItemDto
import net.schedion.app.domain.model.LessonItem
import net.schedion.app.domain.model.LessonStatus
import net.schedion.app.domain.model.PairItem
import net.schedion.app.domain.model.WeekInfo

/** Преобразование типа занятия в читаемую строку. */
fun LessonKind.toReadableString(): String = when (this) {
    LessonKind.LECTURE -> "Лекция"
    LessonKind.SEMINAR -> "Семинар"
    LessonKind.LABORATORY -> "Лабораторная работа"
    LessonKind.PRACTICAL -> "Практическое занятие"
    LessonKind.SELF_STUDY -> "Самостоятельная работа"
    LessonKind.NONE -> ""
}

/** Преобразование DTO занятия в Entity для локальной БД. */
fun ScheduleLessonDto.toEntity(): LessonEntity = LessonEntity(
    id = id,
    date = date,
    pairNumber = pair,
    kind = kind.toReadableString(),
    disciplineId = disciplineId,
    discipline = discipline,
    room = room,
    subgroup = subgroup,
    groupId = groupId,
    groupName = groupName,
    course = course,
    teachers = teachers.joinToString(", ") { it.shortName },
    cancellationReason = cancellationReason,
)

/** Преобразование DTO занятия в доменную модель. */
fun ScheduleLessonDto.toDomain(
    timeStart: String = "",
    timeEnd: String = "",
    status: LessonStatus = LessonStatus.Upcoming,
): LessonItem = LessonItem(
    id = id,
    date = date,
    pairNumber = pair,
    timeStart = timeStart,
    timeEnd = timeEnd,
    kind = kind.toReadableString(),
    discipline = discipline,
    room = room,
    subgroup = subgroup,
    groupName = groupName,
    teachers = teachers.joinToString(", ") { it.shortName },
    status = if (cancellationReason != null) LessonStatus.Cancelled else status,
    cancellationReason = cancellationReason,
    associationName = associationName,
)

/** Преобразование Entity занятия из БД в доменную модель. */
fun LessonEntity.toDomain(
    timeStart: String = "",
    timeEnd: String = "",
    status: LessonStatus = LessonStatus.Upcoming,
): LessonItem = LessonItem(
    id = id,
    date = date,
    pairNumber = pairNumber,
    timeStart = timeStart,
    timeEnd = timeEnd,
    kind = kind,
    discipline = discipline,
    room = room,
    subgroup = subgroup,
    groupName = groupName,
    teachers = teachers,
    status = if (cancellationReason != null) LessonStatus.Cancelled else status,
    cancellationReason = cancellationReason,
    associationName = null,
)

/** Преобразование DTO звонков в Entity. */
fun PairItemDto.toEntity(): PairEntity = PairEntity(
    number = number,
    start = start,
    end = end,
)

/** Преобразование Entity звонков в доменную модель. */
fun PairEntity.toDomain(): PairItem = PairItem(
    number = number,
    start = start.take(5), // форматируем "08:30:00" -> "08:30"
    end = end.take(5),
)

/** Преобразование DTO звонков в доменную модель. */
fun PairItemDto.toDomain(): PairItem = PairItem(
    number = number,
    start = start.take(5),
    end = end.take(5),
)

/** Преобразование DTO недели в Entity. */
fun WeekItemDto.toEntity(): WeekEntity = WeekEntity(
    startDate = start,
    endDate = end,
    number = number,
    color = if (color == WeekColor.RED) "red" else "black",
)

/** Преобразование Entity недели в доменную модель. */
fun WeekEntity.toDomain(isCurrent: Boolean = false): WeekInfo = WeekInfo(
    startDate = startDate,
    endDate = endDate,
    number = number,
    isRed = color == "red",
    isCurrent = isCurrent,
)
