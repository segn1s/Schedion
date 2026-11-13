package net.schedion.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Кэш расписания звонков: время начала и конца пары. */
@Entity(tableName = "pairs")
data class PairEntity(
    @PrimaryKey val number: Int,
    /** Время начала пары в формате `HH:mm:ss`. */
    val start: String,
    /** Время конца пары в формате `HH:mm:ss`. */
    val end: String,
)
