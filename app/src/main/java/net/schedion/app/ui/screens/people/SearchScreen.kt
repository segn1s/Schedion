package net.schedion.app.ui.screens.people

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import net.schedion.app.R
import net.schedion.app.domain.model.FavoriteKind
import net.schedion.app.ui.components.FavoriteStar
import net.schedion.app.ui.components.MockCard
import net.schedion.app.ui.components.MockDivider
import net.schedion.app.ui.components.DirectoryStatus
import net.schedion.app.ui.components.OfflineBanner
import net.schedion.app.ui.components.PeopleHeading
import net.schedion.app.ui.components.ScreenColumn
import net.schedion.app.ui.components.SearchField
import net.schedion.app.ui.components.Segments
import net.schedion.app.ui.screens.schedule.ScheduleViewModel
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

@Composable
internal fun SearchScreen(
    viewModel: ScheduleViewModel,
    onGroup: () -> Unit,
    isOffline: Boolean,
    modifier: Modifier = Modifier,
) {
    val query by viewModel.searchQuery.collectAsState()
    val tabIndex by viewModel.searchTab.collectAsState()
    val state by viewModel.contentState.collectAsState()
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
        Text(stringResource(R.string.search_title), style = T.Heading)
        Spacer(Modifier.height(D.L))

        if (isOffline) {
            val directorySavedAt = if (tabIndex == 0) viewModel.groupsSavedAt else viewModel.teachersSavedAt
            OfflineBanner(savedAt = directorySavedAt.collectAsState().value)
            Spacer(Modifier.height(D.L))
        }

        SearchField(
            value = query,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = stringResource(R.string.search_source_hint),
        )
        Spacer(Modifier.height(D.L))

        Segments(
            labels = listOf(
                stringResource(R.string.search_groups_count, filteredGroups.size),
                stringResource(R.string.search_teachers_count, filteredTeachers.size),
            ),
            active = tabIndex,
            onSelect = { viewModel.setSearchTab(it) },
        )
        Spacer(Modifier.height(D.L))

        DirectoryStatus(state, groups.isNotEmpty() || teachers.isNotEmpty(), viewModel::refreshSchedule,
            showOfflineBanner = false)
        if (tabIndex == 0) {
            PeopleHeading(stringResource(R.string.search_groups))
            if (filteredGroups.isEmpty() && !state.isRefreshing) {
                Text(stringResource(R.string.search_groups_empty), color = C.Muted, style = T.Body)
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
                                    group.description ?: stringResource(R.string.common_course, group.course),
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
            PeopleHeading(stringResource(R.string.search_teachers))
            if (filteredTeachers.isEmpty() && !state.isRefreshing) {
                Text(stringResource(R.string.search_teachers_empty), color = C.Muted, style = T.Body)
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
                                    stringResource(R.string.common_teacher),
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
