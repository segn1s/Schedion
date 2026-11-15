package net.schedion.app.domain.model

/** Тип источника расписания, который можно добавить в избранное. */
enum class FavoriteKind {
    Group,
    Teacher,
}

/** Сохранённый в избранном источник расписания. */
data class FavoriteSource(
    val kind: FavoriteKind,
    val id: Int,
    val title: String,
    val subtitle: String? = null,
    /** Курс группы; null для преподавателя. */
    val course: Int? = null,
)
