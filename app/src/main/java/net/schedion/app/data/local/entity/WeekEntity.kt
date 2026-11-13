package net.schedion.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Кэш учебной недели семестра. */
@Entity(tableName = "weeks")
data class WeekEntity(
    /** Дата начала недели в формате `yyyy-MM-dd`, используется как первичный ключ. */
    @PrimaryKey val startDate: String,
    val endDate: String,
    /** Порядковый номер недели в семестре. */
    val number: Int,
    /** Цвет (чётность) недели: `black` или `red`. */
    val color: String,
)
