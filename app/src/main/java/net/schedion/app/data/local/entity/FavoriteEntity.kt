package net.schedion.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Сохранённый в избранном источник расписания: группа или преподаватель. */
@Entity(
    tableName = "favorites",
    indices = [Index(value = ["kind", "sourceId"], unique = true)],
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Тип источника в виде строки: `group` или `teacher`. */
    val kind: String,
    /** Id группы или преподавателя — значение параметров `group` / `teacher`. */
    val sourceId: Int,
    val title: String,
    val subtitle: String?,
    /** Курс группы; null для преподавателя. */
    val course: Int?,
    @ColumnInfo(defaultValue = "0") val position: Long = 0,
)
