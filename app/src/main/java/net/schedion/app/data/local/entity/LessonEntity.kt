package net.schedion.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Кэш одного занятия расписания. */
@Entity(
    tableName = "schedule_lessons",
    indices = [Index(value = ["date"]), Index(value = ["groupId"])],
)
data class LessonEntity(
    @PrimaryKey val id: Int,
    /** Дата занятия в формате `yyyy-MM-dd`. */
    val date: String,
    /** Номер пары (1–7). */
    val pairNumber: Int,
    /** Вид занятия в виде строки, чтобы новые значения сервера не ломали кэш. */
    val kind: String,
    val disciplineId: Int,
    val discipline: String,
    val room: String?,
    val subgroup: Int?,
    val groupId: Int?,
    val groupName: String?,
    val course: Int?,
    /** Сокращённые имена преподавателей, разделённые запятой. */
    val teachers: String,
    val cancellationReason: String?,
)
