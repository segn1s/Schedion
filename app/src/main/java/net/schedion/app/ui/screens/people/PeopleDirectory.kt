package net.schedion.app.ui.screens.people

import net.schedion.app.data.remote.dto.GroupDto
import net.schedion.app.domain.model.FavoriteKind
import net.schedion.app.domain.model.FavoriteSource

/** Группы, разделённые по курсам: курс и его группы в порядке названия. */
internal fun List<GroupDto>.groupedByCourse(): List<Pair<Int, List<GroupDto>>> =
    groupBy { it.course }
        .toSortedMap()
        .map { (course, groups) -> course to groups.sortedBy { it.name } }

/** true, если источник с таким типом и id уже сохранён в избранном. */
internal fun List<FavoriteSource>.contains(kind: FavoriteKind, id: Int): Boolean =
    any { it.kind == kind && it.id == id }

/**
 * Поднимает избранные источники в начало списка, сохраняя порядок избранного.
 *
 * @param id идентификатор элемента списка.
 */
internal fun <T> List<T>.favoritesFirst(
    favorites: List<FavoriteSource>,
    kind: FavoriteKind,
    id: (T) -> Int,
): List<T> {
    val favoriteIds = favorites.filter { it.kind == kind }.map { it.id }
    return sortedBy { item -> favoriteIds.indexOf(id(item)).takeIf { it >= 0 } ?: Int.MAX_VALUE }
}
