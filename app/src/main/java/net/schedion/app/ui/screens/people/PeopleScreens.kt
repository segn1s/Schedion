package net.schedion.app.ui.screens.people

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import net.schedion.app.data.remote.dto.GroupDto
import net.schedion.app.data.remote.dto.TeacherDto
import net.schedion.app.domain.model.FavoriteKind
import net.schedion.app.domain.model.FavoriteSource
import net.schedion.app.ui.components.Glyph
import net.schedion.app.ui.components.LineIcon
import net.schedion.app.ui.components.MockButton
import net.schedion.app.ui.components.MockCard
import net.schedion.app.ui.components.MockDivider
import net.schedion.app.ui.components.ScreenColumn
import net.schedion.app.ui.components.SearchField
import net.schedion.app.ui.components.Segments
import net.schedion.app.ui.components.bottomSpacing
import net.schedion.app.ui.screens.schedule.ScheduleViewModel
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

/** Группы, разделённые по курсам: курс и его группы в порядке названия. */
private fun List<GroupDto>.groupedByCourse(): List<Pair<Int, List<GroupDto>>> =
    groupBy { it.course }
        .toSortedMap()
        .map { (course, groups) -> course to groups.sortedBy { it.name } }

/** true, если источник с таким типом и id уже сохранён в избранном. */
private fun List<FavoriteSource>.contains(kind: FavoriteKind, id: Int): Boolean =
    any { it.kind == kind && it.id == id }

/**
 * Поднимает избранные источники в начало списка, сохраняя порядок избранного.
 *
 * @param id идентификатор элемента списка.
 */
private fun <T> List<T>.favoritesFirst(
    favorites: List<FavoriteSource>,
    kind: FavoriteKind,
    id: (T) -> Int,
): List<T> {
    val favoriteIds = favorites.filter { it.kind == kind }.map { it.id }
    return sortedBy { item -> favoriteIds.indexOf(id(item)).takeIf { it >= 0 } ?: Int.MAX_VALUE }
}

@Composable
internal fun SearchScreen(
    viewModel: ScheduleViewModel,
    onGroup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val query by viewModel.searchQuery.collectAsState()
    val tabIndex by viewModel.searchTab.collectAsState()
    val groups by viewModel.availableGroups.collectAsState()
    val teachers by viewModel.availableTeachers.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    val filteredGroups = remember(groups, query) {
        if (query.isBlank()) groups
        else groups.filter {
            it.name.contains(query, ignoreCase = true) ||
                (it.description?.contains(query, ignoreCase = true) == true)
        }
    }

    val filteredTeachers = remember(teachers, query) {
        if (query.isBlank()) teachers
        else teachers.filter {
            it.shortName.contains(query, ignoreCase = true)
        }
    }

    ScreenColumn(modifier) {
        Text("Поиск", style = T.Heading)
        Spacer(Modifier.height(D.L))

        SearchField(
            value = query,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = "Группа или преподаватель",
        )
        Spacer(Modifier.height(D.L))

        Segments(
            labels = listOf(
                "Группы (${filteredGroups.size})",
                "Преподаватели (${filteredTeachers.size})",
            ),
            active = tabIndex,
            onSelect = { viewModel.setSearchTab(it) },
        )
        Spacer(Modifier.height(D.L))

        if (tabIndex == 0) {
            PeopleHeading("Группы")
            if (filteredGroups.isEmpty()) {
                Text("Группы не найдены", color = C.Muted, style = T.Body)
            } else {
                MockCard {
                    filteredGroups.favoritesFirst(favorites, FavoriteKind.Group) { it.id }
                        .forEachIndexed { index, group ->
                        if (index > 0) MockDivider()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectGroup(group)
                                    onGroup()
                                }
                                .padding(start = D.L, end = D.Xs, top = D.Sm, bottom = D.Sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(D.Page),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(group.name, style = T.Title)
                                Text(
                                    group.description ?: "${group.course} курс",
                                    color = C.Muted,
                                    style = T.Label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            FavoriteStar(
                                favorite = favorites.contains(FavoriteKind.Group, group.id),
                                onToggle = { viewModel.toggleGroupFavorite(group) },
                            )
                        }
                    }
                }
            }
        } else {
            PeopleHeading("Преподаватели")
            if (filteredTeachers.isEmpty()) {
                Text("Преподаватели не найдены", color = C.Muted, style = T.Body)
            } else {
                MockCard {
                    filteredTeachers.favoritesFirst(favorites, FavoriteKind.Teacher) { it.id }
                        .forEachIndexed { index, teacher ->
                        if (index > 0) MockDivider()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectTeacher(teacher)
                                    onGroup()
                                }
                                .padding(start = D.L, end = D.Xs, top = D.Sm, bottom = D.Sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(D.Page),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(teacher.shortName, style = T.Title)
                                Text(
                                    "Преподаватель",
                                    color = C.Muted,
                                    style = T.Label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            FavoriteStar(
                                favorite = favorites.contains(FavoriteKind.Teacher, teacher.id),
                                onToggle = { viewModel.toggleTeacherFavorite(teacher) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun OnboardingScreen(
    viewModel: ScheduleViewModel,
    onGroup: () -> Unit,
    onTeacher: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups by viewModel.availableGroups.collectAsState()
    val teachers by viewModel.availableTeachers.collectAsState()
    var query by remember { mutableStateOf("") }

    val filteredGroups = remember(groups, query) {
        if (query.isBlank()) groups
        else groups.filter {
            it.name.contains(query, ignoreCase = true) ||
                (it.description?.contains(query, ignoreCase = true) == true)
        }
    }

    val filteredTeachers = remember(teachers, query) {
        if (query.isBlank()) teachers
        else teachers.filter {
            it.shortName.contains(query, ignoreCase = true)
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val panelHeight = (maxHeight - D.OnboardingPanelTop).coerceAtLeast(D.Tiny)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().heightIn(min = D.OnboardingPanelTop).padding(start = D.Xxl, end = D.Xxl, top = D.Top + D.Sm)) {
                Text("Schedion", color = C.Primary, style = T.Brand)
                Spacer(Modifier.height(D.Ml))
                Text("Расписание пар, звонки и новости института.\nРаботает с официальным сервером Schedion.", color = C.Secondary, style = T.ArticleBody)
            }
            Column(
                Modifier.fillMaxWidth().heightIn(min = panelHeight)
                    .shadow(D.FloatingShadow, RoundedCornerShape(topStart = D.PanelRadius, topEnd = D.PanelRadius), spotColor = C.Shadow)
                    .background(C.Sheet, RoundedCornerShape(topStart = D.PanelRadius, topEnd = D.PanelRadius))
                    .padding(start = D.Page, end = D.Page, top = D.Xl, bottom = bottomSpacing() + D.Xxl),
            ) {
                Text("Выберите свою группу или преподавателя", style = T.SheetHeading)
                Spacer(Modifier.height(D.M))
                Text("Позже выбор меняется нажатием на название в шапке расписания", color = C.Muted, style = T.Label)
                Spacer(Modifier.height(D.M))
                SearchField(value = query, onValueChange = { query = it }, placeholder = "Например: 1ИВТ или Петров")
                Spacer(Modifier.height(D.Page))

                PeopleHeading("Группы (${filteredGroups.size})")
                if (filteredGroups.isEmpty()) {
                    Text("Группы не найдены", color = C.Muted, style = T.Body)
                } else {
                    filteredGroups.groupedByCourse().forEach { (course, courseGroups) ->
                        Text(
                            "$course курс",
                            Modifier.padding(top = D.Sm, bottom = D.Xs),
                            C.Muted,
                            style = T.SmallStrong,
                        )
                        MockCard {
                            courseGroups.forEachIndexed { index, group ->
                                if (index > 0) MockDivider()
                                DirectoryRow(
                                    title = group.name,
                                    subtitle = group.description,
                                    onClick = {
                                        viewModel.selectGroup(group)
                                        onGroup()
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(D.M))
                    }
                }

                Spacer(Modifier.height(D.S))
                PeopleHeading("Преподаватели (${filteredTeachers.size})")
                if (filteredTeachers.isEmpty()) {
                    Text("Преподаватели не найдены", color = C.Muted, style = T.Body)
                } else {
                    MockCard {
                        filteredTeachers.forEachIndexed { index, teacher ->
                            if (index > 0) MockDivider()
                            DirectoryRow(
                                title = teacher.shortName,
                                subtitle = null,
                                onClick = {
                                    viewModel.selectTeacher(teacher)
                                    onTeacher()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SchedulePicker(
    viewModel: ScheduleViewModel,
    onGroup: () -> Unit,
    onTeacher: () -> Unit,
    onFavorites: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups by viewModel.availableGroups.collectAsState()
    val teachers by viewModel.availableTeachers.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val selectedTeacher by viewModel.selectedTeacher.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    var query by remember { mutableStateOf("") }

    val filteredGroups = remember(groups, query) {
        if (query.isBlank()) groups
        else groups.filter {
            it.name.contains(query, ignoreCase = true) ||
                (it.description?.contains(query, ignoreCase = true) == true)
        }
    }

    val filteredTeachers = remember(teachers, query) {
        if (query.isBlank()) teachers
        else teachers.filter {
            it.shortName.contains(query, ignoreCase = true)
        }
    }

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = D.Page),
            horizontalArrangement = Arrangement.spacedBy(D.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Чьё расписание", Modifier.weight(1f), style = T.SheetHeading)
            Text("Выбор источника", color = C.Muted, style = T.Label)
        }
        Spacer(Modifier.height(D.Sm))
        SearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Группа или преподаватель",
            modifier = Modifier.padding(horizontal = D.Page),
        )
        Column(Modifier.padding(top = D.M)) {
            if (favorites.isNotEmpty() && query.isBlank()) {
                PeopleHeading("Избранное", Modifier.padding(horizontal = D.Page))
                favorites.forEach { favorite ->
                    val isGroup = favorite.kind == FavoriteKind.Group
                    PickerFavorite(
                        title = favorite.title,
                        subtitle = favorite.subtitle ?: if (isGroup) "Группа" else "Преподаватель",
                        selected = if (isGroup) selectedGroup?.id == favorite.id else selectedTeacher?.id == favorite.id,
                        onClick = {
                            viewModel.openFavorite(favorite)
                            if (isGroup) onGroup() else onTeacher()
                        },
                        favorite = true,
                        onToggleFavorite = { viewModel.removeFavorite(favorite) },
                    )
                }
                Spacer(Modifier.height(D.Ml))
            }

            PeopleHeading(
                "Группы (${filteredGroups.size})",
                Modifier.padding(horizontal = D.Page),
            )
            if (filteredGroups.isEmpty()) {
                Text("Группы не найдены", Modifier.padding(horizontal = D.Page), color = C.Muted, style = T.Body)
            } else {
                filteredGroups.groupedByCourse().forEach { (course, courseGroups) ->
                    Text(
                        "$course курс",
                        Modifier.padding(start = D.Page, top = D.Sm, bottom = D.Xs),
                        C.Muted,
                        style = T.SmallStrong,
                    )
                    courseGroups.favoritesFirst(favorites, FavoriteKind.Group) { it.id }.forEach { group ->
                        PickerFavorite(
                            title = group.name,
                            subtitle = group.description ?: "${group.course} курс",
                            selected = selectedGroup?.id == group.id,
                            onClick = {
                                viewModel.selectGroup(group)
                                onGroup()
                            },
                            favorite = favorites.contains(FavoriteKind.Group, group.id),
                            onToggleFavorite = { viewModel.toggleGroupFavorite(group) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(D.Ml))
            PeopleHeading(
                "Преподаватели (${filteredTeachers.size})",
                Modifier.padding(horizontal = D.Page),
            )
            if (filteredTeachers.isEmpty()) {
                Text("Преподаватели не найдены", Modifier.padding(horizontal = D.Page), color = C.Muted, style = T.Body)
            } else {
                filteredTeachers.favoritesFirst(favorites, FavoriteKind.Teacher) { it.id }.forEach { teacher ->
                    PickerFavorite(
                        title = teacher.shortName,
                        subtitle = "Преподаватель",
                        selected = selectedTeacher?.id == teacher.id,
                        onClick = {
                            viewModel.selectTeacher(teacher)
                            onTeacher()
                        },
                        favorite = favorites.contains(FavoriteKind.Teacher, teacher.id),
                        onToggleFavorite = { viewModel.toggleTeacherFavorite(teacher) },
                    )
                }
            }
        }
        MockDivider()
        MockButton(
            "Изменить избранное",
            Modifier.fillMaxWidth().padding(horizontal = D.Page, vertical = D.M),
            onClick = onFavorites,
        )
    }
}

/** Строка справочника группы или преподавателя без звёздочки избранного. */
@Composable
private fun DirectoryRow(title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = D.PickerRow)
            .clickable(onClick = onClick)
            .padding(horizontal = D.L, vertical = D.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Directory)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    color = C.Muted,
                    style = T.Small,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Строка выбора источника расписания со звёздочкой избранного. */
@Composable
private fun PickerFavorite(
    title: String,
    subtitle: String,
    selected: Boolean = false,
    subtitleColor: Color = C.Muted,
    onClick: (() -> Unit)? = null,
    favorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = D.Sm)
            .clip(RoundedCornerShape(D.ControlRadius)).background(if (selected) C.Cover else Color.Transparent)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(start = D.M, end = D.Xs, top = D.Sm, bottom = D.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(D.Sm),
    ) {
        if (selected) LineIcon(Glyph.Check, tint = C.Primary, size = D.IconSmall)
        else Spacer(Modifier.width(D.IconSmall))
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Title)
            Text(subtitle, color = subtitleColor, style = T.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (onToggleFavorite != null) {
            FavoriteStar(favorite = favorite, onToggle = onToggleFavorite)
        }
    }
}

/** Кнопка-звёздочка, добавляющая источник расписания в избранное или убирающая его оттуда. */
@Composable
private fun FavoriteStar(favorite: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier.size(D.NavIcon + D.Sm).clickable(role = Role.Button, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        LineIcon(
            if (favorite) Glyph.FilledStar else Glyph.Star,
            tint = if (favorite) C.Primary else C.Muted,
            size = D.NavIcon,
        )
    }
}

@Composable
private fun PeopleHeading(label: String, modifier: Modifier = Modifier) {
    Text(label, modifier.padding(bottom = D.S), C.Secondary, style = T.LabelStrong)
}

