package net.schedion.app.ui.screens.schedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import net.schedion.app.core.Resource
import net.schedion.app.domain.model.LessonItem
import net.schedion.app.domain.model.LessonStatus
import net.schedion.app.domain.model.ScheduleDay
import net.schedion.app.domain.model.ScheduleWeek
import net.schedion.app.ui.components.Chip
import net.schedion.app.ui.components.Glyph
import net.schedion.app.ui.components.LineIcon
import net.schedion.app.ui.components.MockButton
import net.schedion.app.ui.components.MockCard
import net.schedion.app.ui.components.MockDivider
import net.schedion.app.ui.components.ScheduleSkeleton
import net.schedion.app.ui.components.ScreenColumn
import net.schedion.app.ui.components.Segments
import net.schedion.app.ui.components.StatusDot
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

@Composable
internal fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onChoose: () -> Unit,
    onLessonDetails: (LessonItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.scheduleState.collectAsState()
    val selectedDayIdx by viewModel.selectedDayIndex.collectAsState()
    val weekly by viewModel.isWeeklyMode.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val selectedTeacher by viewModel.selectedTeacher.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val isTeacher = selectedTeacher != null
    val titleText = selectedTeacher?.fullName ?: selectedGroup?.name ?: "Расписание"
    val subtitleText = if (isTeacher) "Расписание преподавателя" else "Расписание группы"

    val isOffline = (state as? Resource.Success)?.data?.isFromCache == true
    val cachedTime = (state as? Resource.Success)?.data?.cachedTimestamp ?: "недавно"

    // Обновление жестом «потянуть вниз», как в браузере
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshSchedule() },
        modifier = modifier.fillMaxSize(),
    ) {
        ScreenColumn(Modifier.animateContentSize(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing))) {
            // Офлайн-баннер появляется только при реальном fallback на кэш
            AnimatedVisibility(
                visible = isOffline,
                enter = expandVertically(animationSpec = tween(240)) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(160)),
            ) {
                Column {
                    OfflineBanner(timestamp = cachedTime, onRefresh = { viewModel.refreshSchedule() })
                    Spacer(Modifier.height(D.L))
                }
            }

            // Шапка расписания с реальным выбранным источником
            ScheduleHeader(
                title = titleText,
                subtitle = subtitleText,
                teacher = isTeacher,
                weekly = weekly,
                onChoose = onChoose,
                onDay = { viewModel.setWeeklyMode(false) },
                onWeek = { viewModel.setWeeklyMode(true) },
            )

            Spacer(Modifier.height(D.L))

            // Отображение состояния загрузки, ошибки или расписания
            when (val resource = state) {
                is Resource.Loading -> {
                    ScheduleSkeleton()
                }
                is Resource.Error -> {
                    ErrorCard(
                        message = resource.message,
                        onRetry = { viewModel.refreshSchedule() },
                    )
                }
                is Resource.Success -> {
                    val schedule = resource.data
                    AnimatedContent(
                        targetState = weekly,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) togetherWith
                                fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing))
                        },
                        label = "schedule_view_mode",
                    ) { isWeeklyView ->
                        if (isWeeklyView) {
                            WeeklySchedule(
                                days = schedule.days,
                                onLessonDetails = onLessonDetails,
                            )
                        } else {
                            DailyScheduleView(
                                schedule = schedule,
                                selectedDayIndex = selectedDayIdx,
                                onSelectDay = { viewModel.selectDay(it) },
                                onLessonDetails = onLessonDetails,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyScheduleView(
    schedule: ScheduleWeek,
    selectedDayIndex: Int,
    onSelectDay: (Int) -> Unit,
    onLessonDetails: (LessonItem) -> Unit,
) {
    Column {
        if (schedule.days.isNotEmpty()) {
            RealDayStrip(
                days = schedule.days,
                selectedIndex = selectedDayIndex,
                onSelect = onSelectDay,
            )
            Spacer(Modifier.height(D.L))

            val currentDay = schedule.days.getOrNull(selectedDayIndex) ?: schedule.days.first()

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentDay.date,
                    modifier = Modifier.weight(1f),
                    style = T.Title,
                )
                Text(
                    text = "Нед. ${schedule.weekStart}",
                    color = C.Muted,
                    style = T.Label,
                )
            }
            Spacer(Modifier.height(D.L))

            if (currentDay.lessons.isEmpty()) {
                EmptyDay(
                    title = "В этот день пар нет",
                    subtitle = "Наслаждайтесь свободным временем!",
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(D.M)) {
                    currentDay.lessons.forEach { lesson ->
                        RealLessonCard(
                            lesson = lesson,
                            onClick = { onLessonDetails(lesson) },
                        )
                    }
                }
            }
        } else {
            EmptyDay(
                title = "Занятий на этой неделе нет",
                subtitle = "Попробуйте обновить страницу или выбрать другую неделю",
            )
        }

    }
}

@Composable
private fun RealDayStrip(
    days: List<ScheduleDay>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(D.S)) {
        days.take(6).forEachIndexed { index, day ->
            val isSelected = index == selectedIndex
            val isToday = day.isToday

            val targetFill = when {
                isToday && isSelected -> C.Today
                isSelected -> C.Primary
                isToday -> C.GoldSurface
                else -> C.Surface
            }
            val targetOutline = when {
                isSelected -> targetFill
                isToday -> C.GoldOutline
                else -> C.Outline
            }
            val targetDayColor = when {
                isSelected && !isToday -> C.White
                isToday && isSelected -> C.Text
                isToday -> C.GoldText
                else -> C.Muted
            }

            val fill by animateColorAsState(targetFill, animationSpec = tween(220), label = "day_fill_$index")
            val outline by animateColorAsState(targetOutline, animationSpec = tween(220), label = "day_outline_$index")
            val dayColor by animateColorAsState(targetDayColor, animationSpec = tween(220), label = "day_color_$index")

            val shape = RoundedCornerShape(D.ControlRadius)
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(min = D.DayCell)
                    .clip(shape)
                    .background(fill)
                    .border(if (isToday && !isSelected) D.ThickOutline else D.Hairline, outline, shape)
                    .semantics { selected = isSelected }
                    .clickable(role = Role.Button) { onSelect(index) }
                    .padding(vertical = D.Xs),
                verticalArrangement = Arrangement.spacedBy(D.Xs, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(day.dayOfWeek, color = dayColor, style = if (isSelected || isToday) T.SmallStrong else T.Small)
                Text(day.dayNumber, color = if (!isToday && isSelected) C.White else C.Text, style = T.DayNumber)
            }
        }
    }
}

@Composable
private fun RealLessonCard(lesson: LessonItem, onClick: () -> Unit) {
    val status = lesson.status
    val isPast = status == LessonStatus.Past
    val isCurrent = status == LessonStatus.Current
    val isCancelled = status == LessonStatus.Cancelled

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
    val textDecor = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None
    val shape = RoundedCornerShape(D.CardRadius)

    MockCard(
        modifier = if (isCurrent) Modifier.border(D.ThickOutline, outline, shape) else Modifier,
        color = color,
        outline = outline,
        onClick = onClick,
    ) {
        Row(Modifier.padding(horizontal = D.Page, vertical = if (isCancelled) D.L else D.Ml)) {
            Column(Modifier.width(D.ScheduleTime), horizontalAlignment = Alignment.Start) {
                Text(
                    "${lesson.pairNumber} пара",
                    color = if (isCurrent) C.GoldText else if (isPast) C.Faded else C.Muted,
                    style = if (isCurrent) T.SmallStrong else T.Small,
                )
                Text(
                    lesson.timeStart.ifEmpty { "--:--" },
                    color = if (isCancelled) C.Muted else if (isPast) C.Secondary else C.Text,
                    style = T.BodyStrong,
                    textDecoration = textDecor,
                )
                Text(
                    lesson.timeEnd.ifEmpty { "--:--" },
                    color = if (isPast) C.Faded else C.Muted,
                    style = T.Small,
                )
            }
            Spacer(Modifier.width(D.M))
            Column(Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(D.Sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        lesson.kind.ifEmpty { "Занятие" },
                        Modifier.weight(1f),
                        color = if (isPast) C.Faded else C.Secondary,
                        style = T.Small,
                    )
                    // Занятие может проводиться только для одной подгруппы
                    lesson.subgroup?.let { subgroup ->
                        Chip("подгр. $subgroup", C.Cover, C.Primary)
                    }
                    if (isCurrent) Chip("Идёт сейчас", C.Today, C.Text)
                    if (isCancelled) Chip("Отменена", C.RedTint, C.Red)
                }
                Spacer(Modifier.height(D.Tiny))
                Text(
                    lesson.discipline,
                    color = if (isPast || isCancelled) C.Secondary else C.Text,
                    style = T.Title,
                    textDecoration = textDecor,
                )
                Spacer(Modifier.height(D.Tiny))
                val detailsText = listOfNotNull(
                    lesson.room?.let { "ауд. $it" },
                    lesson.teachers.takeIf { it.isNotEmpty() },
                    lesson.groupName,
                ).joinToString(" · ")
                Text(
                    detailsText,
                    color = when {
                        isCancelled -> C.Red
                        isPast -> C.Muted
                        lesson.associationName != null -> C.Purple
                        else -> C.Secondary
                    },
                    style = T.Label,
                )
            }
        }
    }
}

@Composable
private fun WeeklySchedule(
    days: List<ScheduleDay>,
    onLessonDetails: (LessonItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(D.Ml)) {
        days.forEach { day ->
            Text("${day.dayOfWeek}, ${day.date}", style = T.BodyStrong)
            if (day.lessons.isEmpty()) {
                MockCard(color = C.Past) {
                    Text(
                        "Занятий нет",
                        Modifier.padding(D.Ml),
                        color = C.Muted,
                        style = T.Label,
                    )
                }
            } else {
                MockCard {
                    day.lessons.forEachIndexed { index, lesson ->
                        if (index > 0) MockDivider()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onLessonDetails(lesson) }
                                .padding(horizontal = D.Ml, vertical = D.Sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(D.M),
                        ) {
                            Text("${lesson.pairNumber}", color = C.Muted, style = T.LabelStrong)
                            Text(lesson.timeStart.ifEmpty { "--:--" }, color = C.Muted, style = T.Small)
                            Column(Modifier.weight(1f)) {
                                Text(lesson.discipline, style = T.BodySemibold)
                                Text(
                                    listOfNotNull(
                                        lesson.room?.let { "ауд. $it" },
                                        lesson.teachers.takeIf { it.isNotEmpty() },
                                        lesson.subgroup?.let { "подгр. $it" },
                                    ).joinToString(" · "),
                                    color = C.Muted,
                                    style = T.Small,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleHeader(
    title: String,
    subtitle: String,
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
        Column(
            Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClick = onChoose),
        ) {
            Text(subtitle, color = C.Muted, style = T.Label)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(D.Xs)) {
                Text(
                    title,
                    modifier = if (teacher) Modifier.weight(1f, fill = false) else Modifier,
                    style = if (teacher) T.SheetHeading else T.Heading,
                )
                LineIcon(Glyph.Down, tint = C.Muted, size = D.IconSmall)
            }
        }
        Segments(listOf("День", "Неделя"), active = if (weekly) 1 else 0) { index ->
            if (index == 0 && weekly) onDay()
            if (index == 1 && !weekly) onWeek()
        }
    }
}

@Composable
private fun OfflineBanner(timestamp: String, onRefresh: () -> Unit) {
    MockCard(outline = C.GoldPale) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = D.OfflineCard)
                .padding(horizontal = D.L, vertical = D.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(D.S),
        ) {
            StatusDot(C.Today)
            Column(Modifier.weight(1f)) {
                Text("Нет сети", color = C.AmberText, style = T.BodyStrong)
                Text("Показано сохранённое расписание от $timestamp", color = C.Secondary, style = T.Label)
            }
            MockButton("Обновить", compact = true, onClick = onRefresh)
        }
    }
}

@Composable
private fun EmptyDay(title: String, subtitle: String) {
    MockCard(color = C.Past) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .padding(D.Page),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LineIcon(Glyph.EmptyCalendar, tint = C.Muted, size = D.HeroIcon)
            Spacer(Modifier.height(D.M))
            Text(title, style = T.Lead, textAlign = TextAlign.Center)
            Spacer(Modifier.height(D.Xs))
            Text(subtitle, color = C.Secondary, style = T.Body, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    MockCard(outline = C.RedOutline) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(D.Page),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Ошибка загрузки", color = C.Red, style = T.Title)
            Spacer(Modifier.height(D.Xs))
            Text(message, color = C.Secondary, style = T.Body, textAlign = TextAlign.Center)
            Spacer(Modifier.height(D.M))
            MockButton("Повторить", primary = true, compact = true, onClick = onRetry)
        }
    }
}
