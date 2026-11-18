package net.schedion.app.ui.screens.people

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.schedion.app.R
import net.schedion.app.domain.model.FavoriteKind
import net.schedion.app.ui.components.MockButton
import net.schedion.app.ui.components.MockDivider
import net.schedion.app.ui.components.PeopleHeading
import net.schedion.app.ui.components.PickerFavorite
import net.schedion.app.ui.components.SearchField
import net.schedion.app.ui.screens.schedule.ScheduleViewModel
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

@Composable
internal fun SchedulePickerSheet(
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
            Text(stringResource(R.string.schedule_picker_title), Modifier.weight(1f), style = T.SheetHeading)
            Text(stringResource(R.string.schedule_picker_subtitle), color = C.Muted, style = T.Label)
        }
        Spacer(Modifier.height(D.Sm))
        SearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.search_source_hint),
            modifier = Modifier.padding(horizontal = D.Page),
        )
        Column(Modifier.padding(top = D.M)) {
            if (favorites.isNotEmpty() && query.isBlank()) {
                PeopleHeading(stringResource(R.string.favorites_title), Modifier.padding(horizontal = D.Page))
                favorites.forEach { favorite ->
                    val isGroup = favorite.kind == FavoriteKind.Group
                    PickerFavorite(
                        title = favorite.title,
                        subtitle = favorite.subtitle ?: if (isGroup) stringResource(R.string.common_group) else stringResource(R.string.common_teacher),
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
                stringResource(R.string.search_groups_count, filteredGroups.size),
                Modifier.padding(horizontal = D.Page),
            )
            if (filteredGroups.isEmpty()) {
                Text(stringResource(R.string.search_groups_empty), Modifier.padding(horizontal = D.Page), color = C.Muted, style = T.Body)
            } else {
                filteredGroups.groupedByCourse().forEach { (course, courseGroups) ->
                    Text(
                        stringResource(R.string.common_course, course),
                        Modifier.padding(start = D.Page, top = D.Sm, bottom = D.Xs),
                        C.Muted,
                        style = T.SmallStrong,
                    )
                    courseGroups.favoritesFirst(favorites, FavoriteKind.Group) { it.id }.forEach { group ->
                        PickerFavorite(
                            title = group.name,
                            subtitle = group.description ?: stringResource(R.string.common_course, group.course),
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
                stringResource(R.string.search_teachers_count, filteredTeachers.size),
                Modifier.padding(horizontal = D.Page),
            )
            if (filteredTeachers.isEmpty()) {
                Text(stringResource(R.string.search_teachers_empty), Modifier.padding(horizontal = D.Page), color = C.Muted, style = T.Body)
            } else {
                filteredTeachers.favoritesFirst(favorites, FavoriteKind.Teacher) { it.id }.forEach { teacher ->
                    PickerFavorite(
                        title = teacher.shortName,
                        subtitle = stringResource(R.string.common_teacher),
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
            stringResource(R.string.favorites_edit),
            Modifier.fillMaxWidth().padding(horizontal = D.Page, vertical = D.M),
            onClick = onFavorites,
        )
    }
}
