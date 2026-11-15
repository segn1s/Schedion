package net.schedion.app.ui.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import net.schedion.app.ui.components.Chip
import net.schedion.app.ui.components.Glyph
import net.schedion.app.ui.components.LineIcon
import net.schedion.app.ui.components.MockButton
import net.schedion.app.ui.components.MockCard
import net.schedion.app.ui.components.MockDivider
import net.schedion.app.ui.components.ScreenColumn
import net.schedion.app.ui.components.Segments
import net.schedion.app.ui.components.StatusDot
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

internal enum class SchedulePage { GroupDay, GroupWeek, Offline, TeacherDay }

private enum class LessonStatus { Past, Current, Cancelled, Upcoming }

private data class Lesson(
    val number: Int,
    val start: String,
    val end: String,
    val kind: String,
    val title: String,
    val details: String,
    val status: LessonStatus = LessonStatus.Upcoming,
    val association: Boolean = false,
)

private val groupLessons = listOf(
    Lesson(1, "08:30", "09:50", "Лекция", "Математический анализ", "ауд. 214 · Петров А.С.", LessonStatus.Past),
    Lesson(2, "10:00", "11:20", "Лабораторная работа · подгр. 1", "Программирование", "ауд. 305 · Сидорова Е.В.", LessonStatus.Current),
    Lesson(3, "11:40", "13:00", "Практическое занятие", "Базы данных", "Преподаватель на конференции", LessonStatus.Cancelled),
    Lesson(4, "13:30", "14:50", "Семинар", "Философия", "ауд. 118 · Кузнецова М.А."),
)

private val teacherLessons = listOf(
    groupLessons[0].copy(details = "ауд. 214 · 2ИВТ_ТИИ1"),
    Lesson(3, "11:40", "13:00", "Практическое занятие · подгр. 2", "Математический анализ", "ауд. 214 · 1ИВТ_ТИИ2"),
    Lesson(5, "15:00", "16:20", "Лекция", "Высшая математика", "ауд. 101 · ассоциация «Математика, 1 курс»", association = true),
)

@Composable
internal fun ScheduleScreen(
    page: SchedulePage,
    onDay: () -> Unit,
    onWeek: () -> Unit,
    onOffline: () -> Unit,
    onChoose: () -> Unit,
    onLessonDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val teacher = page == SchedulePage.TeacherDay
    val offline = page == SchedulePage.Offline
    val weekly = page == SchedulePage.GroupWeek

    ScreenColumn(modifier) {
        if (offline) {
            OfflineBanner()
            Spacer(Modifier.height(D.L))
        }
        ScheduleHeader(teacher, weekly, onChoose, onDay, onWeek)
        if (weekly) {
            Spacer(Modifier.height(D.Ml))
            WeeklySchedule(onLessonDetails)
        } else {
            Spacer(Modifier.height(D.L))
            DayStrip(offline, teacher, onDay, onOffline)
            Spacer(Modifier.height(D.L))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (offline) "Суббота, 14.11.2026" else "Понедельник, 09.11.2026",
                    modifier = Modifier.weight(1f),
                    style = T.Title,
                )
                Text(
                    if (teacher) "Неделя" else "Нед. 11",
                    modifier = if (teacher) Modifier.clickable(onClick = onWeek) else Modifier,
                    color = if (teacher) C.Primary else C.Muted,
                    style = if (teacher) T.LabelStrong else T.Label,
                )
            }
            Spacer(Modifier.height(D.L))
            if (offline) {
                EmptyDay(onDay)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(D.M)) {
                    val lessons = if (teacher) teacherLessons else groupLessons
                    lessons.forEach { lesson ->
                        LessonCard(lesson, onClick = onLessonDetails.takeIf { lesson.status == LessonStatus.Current })
                    }
                }
                if (!teacher) {
                    Spacer(Modifier.height(D.L))
                    Text("Обновлено в 10:42", Modifier.align(Alignment.CenterHorizontally), C.Muted, style = T.Small)
                }
            }
        }
    }
}

@Composable
private fun ScheduleHeader(
    teacher: Boolean,
    weekly: Boolean,
    onChoose: () -> Unit,
    onDay: () -> Unit,
    onWeek: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(D.Sm),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f).clickable(role = Role.Button, onClick = onChoose)) {
            Text(if (teacher) "Расписание преподавателя" else "Расписание группы", color = C.Muted, style = T.Label)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(D.Xs)) {
                Text(
                    if (teacher) "Петров Алексей\nСергеевич" else "2ИВТ_ТИИ1",
                    modifier = if (teacher) Modifier.weight(1f) else Modifier,
                    style = if (teacher) T.SheetHeading else T.Heading,
                )
                LineIcon(Glyph.Down, tint = C.Muted, size = D.IconSmall)
            }
        }
        if (teacher) {
            val shape = RoundedCornerShape(D.ControlRadius)
            Box(
                Modifier.size(D.Button).background(C.Surface, shape).border(D.Hairline, C.Outline, shape),
                contentAlignment = Alignment.Center,
            ) {
                LineIcon(Glyph.Star, tint = C.Primary, size = D.NavIcon)
            }
        } else {
            Segments(listOf("День", "Неделя"), active = if (weekly) 1 else 0) { index ->
                if (index == 0 && weekly) onDay()
                if (index == 1 && !weekly) onWeek()
            }
        }
    }
}

private val weekdays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб")

@Composable
private fun DayStrip(offline: Boolean, teacher: Boolean, onDay: () -> Unit, onOffline: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(D.S)) {
        weekdays.forEachIndexed { index, day ->
            val today = index == 0
            val selectedDay = index == if (offline) weekdays.lastIndex else 0
            val fill = when {
                today && selectedDay -> C.Today
                selectedDay -> C.Primary
                today -> C.GoldSurface
                else -> C.Surface
            }
            val outline = when {
                selectedDay -> fill
                today -> C.GoldOutline
                else -> C.Outline
            }
            val dayColor = when {
                selectedDay && !today -> C.White
                today && selectedDay -> C.Text
                today -> C.GoldText
                else -> C.Muted
            }
            val navigate: (() -> Unit)? = when {
                index == 0 && offline -> onDay
                index == weekdays.lastIndex && !offline && !teacher -> onOffline
                else -> null
            }
            val shape = RoundedCornerShape(D.ControlRadius)
            Column(
                Modifier.weight(1f).heightIn(min = D.DayCell).clip(shape).background(fill)
                    .border(if (today && !selectedDay) D.ThickOutline else D.Hairline, outline, shape)
                    .semantics { selected = selectedDay }
                    .then(if (navigate == null) Modifier else Modifier.clickable(role = Role.Button, onClick = navigate))
                    .padding(vertical = D.Xs),
                verticalArrangement = Arrangement.spacedBy(D.Xs, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(day, color = dayColor, style = if (selectedDay || today) T.SmallStrong else T.Small)
                Text((9 + index).toString(), color = if (!today && selectedDay) C.White else C.Text, style = T.DayNumber)
            }
        }
    }
}

@Composable
private fun LessonCard(lesson: Lesson, onClick: (() -> Unit)?) {
    val status = lesson.status
    val past = status == LessonStatus.Past
    val current = status == LessonStatus.Current
    val cancelled = status == LessonStatus.Cancelled
    val color = when (status) {
        LessonStatus.Past -> C.Past
        LessonStatus.Current -> C.GoldSurface
        else -> C.Surface
    }
    val outline = when (status) {
        LessonStatus.Past -> C.Divider
        LessonStatus.Current -> C.GoldOutline
        LessonStatus.Cancelled -> C.RedOutline
        else -> C.Outline
    }
    val accent = if (cancelled) TextDecoration.LineThrough else TextDecoration.None
    val shape = RoundedCornerShape(D.CardRadius)
    MockCard(
        modifier = if (current) Modifier.border(D.ThickOutline, outline, shape) else Modifier,
        color = color,
        outline = outline,
        onClick = onClick,
    ) {
        Row(Modifier.padding(horizontal = D.Page, vertical = if (cancelled) D.L else D.Ml)) {
            Column(Modifier.width(D.ScheduleTime), horizontalAlignment = Alignment.Start) {
                Text("${lesson.number} пара", color = if (current) C.GoldText else if (past) C.Faded else C.Muted, style = if (current) T.SmallStrong else T.Small)
                Text(lesson.start, color = if (cancelled) C.Muted else if (past) C.Secondary else C.Text, style = T.BodyStrong, textDecoration = accent)
                Text(lesson.end, color = if (past) C.Faded else C.Muted, style = T.Small)
            }
            Spacer(Modifier.width(D.M))
            Column(Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(D.Sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(lesson.kind, Modifier.weight(1f), color = if (past) C.Faded else C.Secondary, style = T.Small)
                    if (current) Chip("Идёт, до 11:20", C.Today, C.Text)
                    if (cancelled) Chip("Отменена", C.RedTint, C.Red)
                }
                Spacer(Modifier.height(D.Tiny))
                Text(lesson.title, color = if (past || cancelled) C.Secondary else C.Text, style = T.Title, textDecoration = accent)
                Spacer(Modifier.height(D.Tiny))
                Text(
                    lesson.details,
                    color = when {
                        cancelled -> C.Red
                        past -> C.Muted
                        lesson.association -> C.Purple
                        else -> C.Secondary
                    },
                    style = T.Label,
                )
            }
        }
    }
}

@Composable
private fun OfflineBanner() {
    MockCard(outline = C.GoldPale) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = D.OfflineCard).padding(horizontal = D.L, vertical = D.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(D.S),
        ) {
            StatusDot(C.Today)
            Column(Modifier.weight(1f)) {
                Text("Нет сети", color = C.AmberText, style = T.BodyStrong)
                Text("Показано сохранённое\nрасписание от 09.11, 10:42", color = C.Secondary, style = T.Label)
            }
            MockButton("Обновить", compact = true)
        }
    }
}

@Composable
private fun EmptyDay(onDay: () -> Unit) {
    MockCard(color = C.Past) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = D.EmptyCard).padding(D.Page),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LineIcon(Glyph.EmptyCalendar, tint = C.Muted, size = D.HeroIcon)
            Spacer(Modifier.height(D.Ml))
            Text("В этот день пар нет", style = T.Lead, textAlign = TextAlign.Center)
            Spacer(Modifier.height(D.Sm))
            Text("Ближайшие занятия в понедельник, 16.11", color = C.Secondary, style = T.Body, textAlign = TextAlign.Center)
            Spacer(Modifier.height(D.Ml))
            MockButton("Открыть 16.11", Modifier.width(D.DetailsLabel), primary = true, compact = true, onClick = onDay)
        }
    }
}

@Composable
internal fun TeacherPrompt(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(D.CardRadius)
    Row(
        modifier.fillMaxWidth().heightIn(min = D.TeacherPrompt).shadow(D.FloatingShadow, shape, spotColor = C.Shadow)
            .background(C.Surface, shape).padding(horizontal = D.Ml, vertical = D.S),
        horizontalArrangement = Arrangement.spacedBy(D.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(C.Primary)
        Text("Добавить в\nизбранное?", Modifier.weight(1f), style = T.BodySemibold)
        Text("Не\nсейчас", Modifier.padding(horizontal = D.L), C.Secondary, textAlign = TextAlign.Center, style = T.LabelStrong)
        MockButton("Добавить", primary = true, compact = true)
    }
}

private data class WeekLesson(
    val number: Int,
    val time: String,
    val title: String,
    val details: String = "",
    val status: LessonStatus = LessonStatus.Upcoming,
    val association: String? = null,
)

@Composable
private fun WeeklySchedule(onLessonDetails: () -> Unit) {
    MockCard {
        Row(
            Modifier.fillMaxWidth().heightIn(min = D.WeekPicker).padding(horizontal = D.Xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LineIcon(Glyph.Back, tint = C.Primary)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Нед. 11", style = T.BodyStrong)
                Text("09.11 – 15.11.2026", color = C.Muted, style = T.Small)
            }
            LineIcon(Glyph.Next, tint = C.Primary)
        }
    }
    Spacer(Modifier.height(D.Ml))
    Row(horizontalArrangement = Arrangement.spacedBy(D.Sm), verticalAlignment = Alignment.CenterVertically) {
        Text("Пн, 09.11", color = C.GoldText, style = T.BodyStrong)
        Chip("Сегодня", C.Today, C.Text)
    }
    Spacer(Modifier.height(D.S))
    WeekTable(
        listOf(
            WeekLesson(1, "08:30", "Математический анализ", "214"),
            WeekLesson(2, "10:00", "Программирование", "305 · подгр. 1", LessonStatus.Current),
            WeekLesson(3, "11:40", "Базы данных", "отменена", LessonStatus.Cancelled),
            WeekLesson(4, "13:30", "Философия", "118"),
        ),
        onLessonDetails,
    )
    WeekHeading("Вт, 10.11")
    WeekTable(
        listOf(
            WeekLesson(2, "10:00", "Английский язык", "402"),
            WeekLesson(3, "11:40", "Физическая культура", "спортзал", association = "Ассоциация «Физкультура, 2 курс»"),
            WeekLesson(4, "13:30", "Базы данных", "307"),
        ),
        onLessonDetails,
    )
    WeekHeading("Ср, 11.11")
    MockCard(color = C.CancelledDay, outline = C.RedOutline) {
        Row(Modifier.padding(D.Ml), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(D.S)) {
            StatusDot(C.RedDot)
            Column {
                Text("Занятий нет", color = C.Red, style = T.BodyStrong)
                Text("День здоровья, все группы", color = C.Secondary, style = T.Label)
            }
        }
    }
    WeekHeading("Чт, 12.11")
    WeekTable(listOf(WeekLesson(1, "08:30", "Математический анализ", "214")), onLessonDetails)
}

@Composable
private fun WeekHeading(label: String) {
    Spacer(Modifier.height(D.Ml))
    Text(label, style = T.BodyStrong)
    Spacer(Modifier.height(D.S))
}

@Composable
private fun WeekTable(lessons: List<WeekLesson>, onLessonDetails: () -> Unit) {
    MockCard {
        lessons.forEachIndexed { index, lesson ->
            if (index > 0) MockDivider()
            val current = lesson.status == LessonStatus.Current
            val cancelled = lesson.status == LessonStatus.Cancelled
            val meta = if (current) C.GoldText else C.Muted
            Row(
                Modifier.fillMaxWidth().heightIn(min = D.ButtonSmall)
                    .then(if (current) Modifier.background(C.GoldTint).clickable(onClick = onLessonDetails) else Modifier)
                    .padding(horizontal = D.Ml, vertical = D.Sm),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(D.M),
            ) {
                Text(lesson.number.toString(), Modifier.width(D.WeekNumber), color = meta, style = T.LabelStrong)
                Text(lesson.time, Modifier.width(D.WeekTime - D.M), color = meta, style = if (current) T.LabelStrong else T.Label)
                Column {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(textDecoration = if (cancelled) TextDecoration.LineThrough else null)) {
                                append(lesson.title)
                            }
                            withStyle(SpanStyle(color = if (cancelled) C.Red else C.Muted, fontWeight = FontWeight.Normal)) {
                                append(" · ${lesson.details}")
                            }
                        },
                        color = if (cancelled) C.Red else C.Text,
                        style = if (current) T.BodyStrong else T.BodySemibold,
                    )
                    if (lesson.association != null) Text(lesson.association, color = C.Purple, style = T.Small)
                }
            }
        }
    }
}
