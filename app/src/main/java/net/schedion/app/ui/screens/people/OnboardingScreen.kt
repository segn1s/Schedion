package net.schedion.app.ui.screens.people

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import net.schedion.app.R
import net.schedion.app.ui.components.DirectoryRow
import net.schedion.app.ui.components.MockCard
import net.schedion.app.ui.components.MockDivider
import net.schedion.app.ui.components.DirectoryStatus
import net.schedion.app.ui.components.PeopleHeading
import net.schedion.app.ui.components.SearchField
import net.schedion.app.ui.components.bottomSpacing
import net.schedion.app.ui.screens.schedule.ScheduleViewModel
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

@Composable
internal fun OnboardingScreen(
    viewModel: ScheduleViewModel,
    onGroup: () -> Unit,
    onTeacher: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.directoryState.collectAsState()
    val knownGroups by viewModel.availableGroups.collectAsState()
    val knownTeachers by viewModel.availableTeachers.collectAsState()
    val groups = state.data?.groups ?: knownGroups
    val teachers = state.data?.teachers ?: knownTeachers
    val showGroups = state.data != null || groups.isNotEmpty()
    val showTeachers = state.data != null || teachers.isNotEmpty()
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
                Text(stringResource(R.string.app_name), color = C.Primary, style = T.Brand)
                Spacer(Modifier.height(D.Ml))
                Text(stringResource(R.string.search_onboarding_description), color = C.Secondary, style = T.ArticleBody)
            }
            Column(
                Modifier.fillMaxWidth().heightIn(min = panelHeight)
                    .shadow(D.FloatingShadow, RoundedCornerShape(topStart = D.PanelRadius, topEnd = D.PanelRadius), spotColor = C.Shadow)
                    .background(C.Sheet, RoundedCornerShape(topStart = D.PanelRadius, topEnd = D.PanelRadius))
                    .padding(start = D.Page, end = D.Page, top = D.Xl, bottom = bottomSpacing() + D.Xxl),
            ) {
                Text(stringResource(R.string.search_onboarding_title), style = T.SheetHeading)
                Spacer(Modifier.height(D.M))
                Text(stringResource(R.string.search_onboarding_hint), color = C.Muted, style = T.Label)
                Spacer(Modifier.height(D.M))
                SearchField(value = query, onValueChange = { query = it }, placeholder = stringResource(R.string.search_onboarding_query_hint))
                Spacer(Modifier.height(D.Page))

                DirectoryStatus(state, onRetry = { viewModel.refreshDirectories(refreshAllWeeks = true) })
                if (showGroups) PeopleHeading(stringResource(R.string.search_groups_count, filteredGroups.size))
                if (showGroups && filteredGroups.isEmpty()) {
                    Text(stringResource(R.string.search_groups_empty), color = C.Muted, style = T.Body)
                } else {
                    filteredGroups.groupedByCourse().forEach { (course, courseGroups) ->
                        Text(
                            stringResource(R.string.common_course, course),
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
                if (showTeachers) PeopleHeading(stringResource(R.string.search_teachers_count, filteredTeachers.size))
                if (showTeachers && filteredTeachers.isEmpty()) {
                    Text(stringResource(R.string.search_teachers_empty), color = C.Muted, style = T.Body)
                } else if (filteredTeachers.isNotEmpty()) {
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
