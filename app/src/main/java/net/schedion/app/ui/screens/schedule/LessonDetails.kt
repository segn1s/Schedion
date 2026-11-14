package net.schedion.app.ui.screens.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import net.schedion.app.ui.components.Chip
import net.schedion.app.ui.components.MockButton
import net.schedion.app.ui.components.MockCard
import net.schedion.app.ui.components.ScreenColumn
import net.schedion.app.ui.screens.documents.DetailRow
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

@Composable
internal fun LessonDetails(onClose: () -> Unit, onTeacher: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = D.Page, vertical = D.Sm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(D.Sm)) {
            Chip("Идёт, до 11:20", C.Today, C.Text)
            Text("Лабораторная работа", color = C.Secondary, style = T.Label)
        }
        Spacer(Modifier.height(D.Sm))
        Text("Программирование", style = T.SheetHeading)
        Spacer(Modifier.height(D.Page))
        Column(verticalArrangement = Arrangement.spacedBy(D.Ml)) {
            DetailRow("Когда") { Text("Пн, 09.11 · 2 пара, 10:00 – 11:20", style = T.Body) }
            DetailRow("Аудитория") { Text("305", style = T.Body) }
            DetailRow("Группа") { Text("2ИВТ_ТИИ1, подгруппа 1", style = T.Body) }
            DetailRow("Преподаватель") {
                Text(
                    "Сидорова Е.В.",
                    Modifier.clickable(role = Role.Button, onClick = onTeacher),
                    color = C.Primary, style = T.BodyStrong,
                )
            }
        }
        Spacer(Modifier.height(D.Page + D.Tiny))
        MockButton("Расписание преподавателя", Modifier.fillMaxWidth(), primary = true, onClick = onTeacher)
        Spacer(Modifier.height(D.Sm))
        MockButton("Закрыть", Modifier.fillMaxWidth(), onClick = onClose)
    }
}

/** Приглушённые карточки за панелью деталей повторяют отдельный фон страницы 10. */
@Composable
internal fun LessonPreview(modifier: Modifier = Modifier) {
    ScreenColumn(modifier, withTabs = false) {
        Text("Расписание группы", color = C.Muted, style = T.Label)
        Text("2ИВТ_ТИИ1", style = T.Heading)
        Spacer(Modifier.height(D.L))
        MockCard(color = C.Past, outline = C.Past) { Spacer(Modifier.height(D.DayCell - D.Xs)) }
        repeat(3) { index ->
            Spacer(Modifier.height(D.L))
            val current = index == 1
            val fill = if (current) C.GoldSurface else C.Past
            MockCard(color = fill, outline = if (current) C.GoldOutline else fill) {
                Spacer(Modifier.height(D.DocumentRow + D.Tiny))
            }
        }
    }
}

@Composable
internal fun PickerPreview(teacher: Boolean, offline: Boolean, modifier: Modifier = Modifier) {
    ScreenColumn(modifier, withTabs = false) {
        if (offline) {
            Text("Нет сети", color = C.AmberText, style = T.BodyStrong)
        } else {
            Text(if (teacher) "Расписание преподавателя" else "Расписание группы", color = C.Muted, style = T.Label)
            Text(if (teacher) "Петров Алексей Сергеевич" else "2ИВТ_ТИИ1", style = T.Heading)
        }
    }
}
