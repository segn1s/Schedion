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
import androidx.compose.ui.semantics.Role
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
    val discipline = lesson?.discipline ?: "Занятие"
    val kind = lesson?.kind?.ifEmpty { "Занятие" } ?: "Занятие"
    val isCurrent = lesson?.status == LessonStatus.Current
    val isCancelled = lesson?.status == LessonStatus.Cancelled
    val teachers = lesson?.teachers

    Column(modifier.fillMaxWidth().padding(horizontal = D.Page, vertical = D.Sm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(D.Sm)) {
            if (isCurrent) Chip("Идёт сейчас", C.Today, C.Text)
            if (isCancelled) Chip("Отменена", C.RedTint, C.Red)
            Text(kind, color = C.Secondary, style = T.Label)
        }
        Spacer(Modifier.height(D.Sm))
        Text(discipline, style = T.SheetHeading)
        Spacer(Modifier.height(D.Page))
        Column(verticalArrangement = Arrangement.spacedBy(D.Ml)) {
            DetailRow("Когда") {
                Text(
                    "${lesson?.date ?: ""} · ${lesson?.pairNumber ?: 1} пара, ${lesson?.timeStart ?: ""} – ${lesson?.timeEnd ?: ""}",
                    style = T.Body,
                )
            }
            lesson?.room?.let { room ->
                DetailRow("Аудитория") { Text(room, style = T.Body) }
            }
            lesson?.groupName?.let { group ->
                DetailRow("Группа") { Text(group, style = T.Body) }
            }
            lesson?.subgroup?.let { subgroup ->
                DetailRow("Подгруппа") { Text("$subgroup", style = T.Body) }
            }
            val teachers = lesson?.teachers
            if (!teachers.isNullOrEmpty()) {
                DetailRow("Преподаватель") {
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
            MockButton("Расписание преподавателя", Modifier.fillMaxWidth(), primary = true, onClick = onTeacher)
            Spacer(Modifier.height(D.Sm))
        }
        MockButton("Закрыть", Modifier.fillMaxWidth(), onClick = onClose)
    }
}

