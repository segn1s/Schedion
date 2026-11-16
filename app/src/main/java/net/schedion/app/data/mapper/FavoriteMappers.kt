package net.schedion.app.data.mapper

import net.schedion.app.data.local.entity.FavoriteEntity
import net.schedion.app.data.remote.dto.GroupDto
import net.schedion.app.data.remote.dto.TeacherDto
import net.schedion.app.domain.model.FavoriteKind
import net.schedion.app.domain.model.FavoriteSource

/** Ключ типа источника для хранения в локальной БД. */
private fun FavoriteKind.storageKey(): String = name.lowercase()

/** Преобразование группы в запись избранного. */
fun GroupDto.toFavorite(): FavoriteSource = FavoriteSource(
    kind = FavoriteKind.Group,
    id = id,
    title = name,
    subtitle = description ?: "$course курс",
    course = course,
)

/** Преобразование преподавателя в запись избранного. */
fun TeacherDto.toFavorite(): FavoriteSource = FavoriteSource(
    kind = FavoriteKind.Teacher,
    id = id,
    title = shortName,
    subtitle = "Преподаватель",
)

/** Преобразование доменной модели избранного в Entity. */
fun FavoriteSource.toEntity(): FavoriteEntity = FavoriteEntity(
    kind = kind.storageKey(),
    sourceId = id,
    title = title,
    subtitle = subtitle,
    course = course,
)

/** Преобразование Entity избранного в доменную модель. */
fun FavoriteEntity.toDomain(): FavoriteSource = FavoriteSource(
    kind = FavoriteKind.entries.firstOrNull { it.storageKey() == kind } ?: FavoriteKind.Group,
    id = sourceId,
    title = title,
    subtitle = subtitle,
    course = course,
)
