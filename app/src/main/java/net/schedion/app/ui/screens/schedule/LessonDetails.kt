package net.schedion.app.ui.screens.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import net.schedion.app.R
import net.schedion.app.domain.model.LessonItem
import net.schedion.app.domain.model.LessonStatus
import net.schedion.app.ui.components.Chip
import net.schedion.app.ui.components.MockButton
import net.schedion.app.ui.screens.documents.DetailRow
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

@Composable
internal fun LessonDetails(
    lesson: LessonItem?,
    onClose: () -> Unit,
    onTeacher: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val discipline = lesson?.discipline ?: stringResource(R.string.schedule_lesson)
    val kind = lesson?.kind?.ifEmpty { stringResource(R.string.schedule_lesson) } ?: stringResource(R.string.schedule_lesson)
    val isCurrent = lesson?.status == LessonStatus.Current
    val isCancelled = lesson?.status == LessonStatus.Cancelled
    val teachers = lesson?.teachers

    Column(modifier.fillMaxWidth().padding(horizontal = D.Page, vertical = D.Sm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(D.Sm)) {
            if (isCurrent) Chip(stringResource(R.string.schedule_status_current), C.Today, C.Text)
            if (isCancelled) Chip(stringResource(R.string.schedule_status_cancelled), C.RedTint, C.Red)
            Text(kind, color = C.Secondary, style = T.Label)
        }
        Spacer(Modifier.height(D.Sm))
        Text(discipline, style = T.SheetHeading)
        Spacer(Modifier.height(D.Page))
        Column(verticalArrangement = Arrangement.spacedBy(D.Ml)) {
            DetailRow(stringResource(R.string.schedule_when)) {
                Text(
                    stringResource(
                        R.string.schedule_lesson_datetime,
                        lesson?.date.orEmpty(),
                        lesson?.pairNumber ?: 1,
                        lesson?.timeStart.orEmpty(),
                        lesson?.timeEnd.orEmpty(),
                    ),
                    style = T.Body,
                )
            }
            lesson?.room?.let { room ->
                DetailRow(stringResource(R.string.schedule_room)) { Text(room, style = T.Body) }
            }
            lesson?.groupName?.let { group ->
                DetailRow(stringResource(R.string.common_group)) { Text(group, style = T.Body) }
            }
            lesson?.subgroup?.let { subgroup ->
                DetailRow(stringResource(R.string.schedule_subgroup)) { Text(subgroup.toString(), style = T.Body) }
            }
            val teachers = lesson?.teachers
            if (!teachers.isNullOrEmpty()) {
                DetailRow(stringResource(R.string.common_teacher)) {
                    Text(
                        teachers,
                        Modifier.clickable(role = Role.Button, onClick = onTeacher),
                        color = C.Primary,
                        style = T.BodyStrong,
                    )
                }
            }
        }
        Spacer(Modifier.height(D.Page + D.Tiny))
        if (!teachers.isNullOrEmpty()) {
            MockButton(stringResource(R.string.schedule_teacher), Modifier.fillMaxWidth(), primary = true, onClick = onTeacher)
            Spacer(Modifier.height(D.Sm))
        }
        MockButton(stringResource(R.string.common_close), Modifier.fillMaxWidth(), onClick = onClose)
    }
}

