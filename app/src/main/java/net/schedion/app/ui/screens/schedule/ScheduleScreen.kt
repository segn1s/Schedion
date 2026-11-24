package net.schedion.app.ui.screens.schedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import java.time.LocalDate
import net.schedion.app.R
import net.schedion.app.domain.model.BreakStatus
import net.schedion.app.domain.model.CurrentPairStatus
import net.schedion.app.domain.model.LessonItem
import net.schedion.app.domain.model.LessonStatus
import net.schedion.app.domain.model.ScheduleDay
import net.schedion.app.domain.model.ScheduleWeek
import net.schedion.app.domain.model.WeekInfo
import net.schedion.app.ui.components.Chip
import net.schedion.app.ui.components.EmptyContent
import net.schedion.app.ui.components.Glyph
import net.schedion.app.ui.components.LineIcon
import net.schedion.app.ui.components.OfflineBanner
import net.schedion.app.ui.components.rememberRefreshHandle
import net.schedion.app.ui.components.SchedionButton
import net.schedion.app.ui.components.SchedionCard
import net.schedion.app.ui.components.ScheduleSkeleton
import net.schedion.app.ui.components.ScreenColumn
import net.schedion.app.ui.components.Segments
import net.schedion.app.ui.components.StatusDot
import net.schedion.app.ui.components.UiStateContent
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

@Composable
internal fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onChoose: () -> Unit,
    onLessonDetails: (LessonItem) -> Unit,
    isOffline: Boolean,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.contentState.collectAsState()
    val selectedDayIdx by viewModel.selectedDayIndex.collectAsState()
    val weekly by viewModel.isWeeklyMode.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val selectedTeacher by viewModel.selectedTeacher.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val breakStatus by viewModel.breakStatus.collectAsState()
    val currentPairStatus by viewModel.currentPairStatus.collectAsState()
    val weeks by viewModel.weeks.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()

    val isTeacher = selectedTeacher != null
    val titleText = selectedTeacher?.shortName ?: selectedGroup?.name ?: stringResource(R.string.schedule_title)
    val subtitleText = if (isTeacher) stringResource(R.string.schedule_teacher) else stringResource(R.string.schedule_group)

    // Индикатор обновления не должен исчезать раньше, чем пользователь увидит анимацию
    val refresh = rememberRefreshHandle { viewModel.refreshSchedule() }

    PullToRefreshBox(
        isRefreshing = refresh.holding || isRefreshing,
        onRefresh = refresh.onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        ScreenColumn(Modifier.animateContentSize(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing))) {
            // Плашка офлайн-режима выше шапки и переключателя вида
            if (isOffline) {
                OfflineBanner(state.savedAt, onRefresh = refresh.onRefresh)
                Spacer(Modifier.height(D.L))
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

            UiStateContent(
                state = state,
                onRetry = refresh.onRefresh,
                showOfflineBanner = false,
                loading = { ScheduleSkeleton() },
            ) { schedule ->
                if (selectedGroup == null && selectedTeacher == null) {
                    EmptyContent(R.string.state_no_source)
                    SchedionButton(stringResource(R.string.schedule_choose_source), onClick = onChoose)
                } else {
                    AnimatedContent(
                        targetState = weekly,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                        label = "schedule_view_mode",
                    ) { isWeeklyView ->
                        if (isWeeklyView) {
                            WeeklySchedule(
                                schedule = schedule,
                                weeks = weeks,
                                selectedWeekStart = currentWeek,
                                isTeacher = isTeacher,
                                currentPairStatus = currentPairStatus,
                                onSelectWeek = viewModel::selectWeek,
                                onLessonDetails = onLessonDetails,
                            )
                        } else {
                            DailyScheduleView(
                                schedule = schedule,
                                selectedDayIndex = selectedDayIdx,
                                onSelectDay = viewModel::selectDay,
                                onLessonDetails = onLessonDetails,
                                breakStatus = breakStatus,
                                currentPairStatus = currentPairStatus,
                                weekNumber = weeks.firstOrNull { it.startDate == schedule.weekStart }?.number,
                                isTeacher = isTeacher,
                                savedAt = state.savedAt,
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
    breakStatus: BreakStatus? = null,
    currentPairStatus: CurrentPairStatus? = null,
    weekNumber: Int? = null,
    isTeacher: Boolean = false,
    savedAt: Long? = null,
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
                    text = formatFullDate(currentDay.date),
                    modifier = Modifier.weight(1f),
                    style = T.Title,
                )
                if (weekNumber != null) {
                    Text(
                        text = stringResource(R.string.schedule_week_start, weekNumber.toString()),
                        color = C.Muted,
                        style = T.Label,
                    )
                }
            }
            Spacer(Modifier.height(D.L))

            val currentBreak = if (currentDay.isToday) breakStatus else null
            if (currentBreak != null) {
                SchedionCard(color = C.Surface, outline = C.Outline) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = D.Page, vertical = D.S),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(D.S),
                    ) {
                        LineIcon(Glyph.Bell, tint = C.Primary, size = D.IconSmall)
                        Text(
                            stringResource(R.string.schedule_break_until, currentBreak.nextPairStart),
                            style = T.BodyStrong,
                            color = C.Primary,
                        )
                    }
                }
                Spacer(Modifier.height(D.M))
            }

            if (currentDay.lessons.isEmpty()) {
                // Ближайший день с парами: из пустого дня можно сразу на него перейти
                val nextIndex = schedule.days.indices.firstOrNull { index ->
                    index > selectedDayIndex && schedule.days[index].lessons.isNotEmpty()
                }
                val nextDay = nextIndex?.let { schedule.days[it] }
                EmptyDay(
                    title = stringResource(R.string.state_schedule_day_empty),
                    subtitle = nextDay?.let {
                        stringResource(R.string.state_next_lessons, weekdayIn(it.date), formatShortDate(it.date))
                    },
                    buttonLabel = nextDay?.let { stringResource(R.string.state_open_date, formatShortDate(it.date)) },
                    onOpen = nextIndex?.let { index -> { onSelectDay(index) } },
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(D.M)) {
                    currentDay.lessons.forEach { lesson ->
                        RealLessonCard(
                            lesson = lesson,
                            isTeacher = isTeacher,
                            livePair = if (currentDay.isToday) {
                                currentPairStatus?.takeIf { it.pairNumber == lesson.pairNumber }
                            } else {
                                null
                            },
                            onClick = { onLessonDetails(lesson) },
                        )
                    }
                }
                if (savedAt != null) {
                    Spacer(Modifier.height(D.Page))
                    Text(
                        text = stringResource(R.string.schedule_updated_at, formatUpdateTime(savedAt)),
                        modifier = Modifier.fillMaxWidth(),
                        color = C.Muted,
                        style = T.Small,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            EmptyDay(
                title = stringResource(R.string.state_schedule_week_empty),
                subtitle = stringResource(R.string.state_schedule_week_empty_hint),
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
private fun RealLessonCard(
    lesson: LessonItem,
    isTeacher: Boolean,
    onClick: () -> Unit,
    livePair: CurrentPairStatus? = null,
) {
    val status = lesson.status
    val isCancelled = status == LessonStatus.Cancelled
    // Статус из снимка расписания устаревает: текущую пару уточняет расчёт по времени (#38)
    val isCurrent = !isCancelled && (status == LessonStatus.Current || livePair != null)
    val isPast = !isCurrent && status == LessonStatus.Past
    val distant = isDistantRoom(lesson.room)

    val color = when {
        isPast -> C.Past
        isCurrent -> C.GoldSurface
        else -> C.Surface
    }
    val outline = when {
        isPast -> C.Divider
        isCurrent -> C.GoldOutline
        isCancelled -> C.RedOutline
        else -> C.Outline
    }
    val textDecor = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None
    val shape = RoundedCornerShape(D.CardRadius)

    SchedionCard(
        modifier = if (isCurrent) Modifier.border(D.ThickOutline, outline, shape) else Modifier,
        color = color,
        outline = outline,
        onClick = onClick,
    ) {
        Row(Modifier.padding(horizontal = D.Page, vertical = if (isCancelled) D.L else D.Ml)) {
            Column(Modifier.width(D.ScheduleTime), horizontalAlignment = Alignment.Start) {
                Text(
                    stringResource(R.string.schedule_pair_number, lesson.pairNumber),
                    color = if (isCurrent) C.GoldText else if (isPast) C.Faded else C.Muted,
                    style = if (isCurrent) T.SmallStrong else T.Small,
                )
                Text(
                    lesson.timeStart.ifEmpty { stringResource(R.string.common_time_unknown) },
                    color = if (isCancelled) C.Muted else if (isPast) C.Secondary else C.Text,
                    style = T.BodyStrong,
                    textDecoration = textDecor,
                )
                Text(
                    lesson.timeEnd.ifEmpty { stringResource(R.string.common_time_unknown) },
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
                        lesson.kind.ifEmpty { stringResource(R.string.schedule_lesson) },
                        Modifier.weight(1f),
                        color = if (isPast) C.Faded else C.Secondary,
                        style = T.Small,
                    )
                    // Занятие может проводиться только для одной подгруппы
                    lesson.subgroup?.let { subgroup ->
                        Chip(stringResource(R.string.schedule_subgroup_short, subgroup), C.Cover, C.Primary)
                    }
                    // Дистанционную пару помечаем так же, как и подгруппу
                    if (distant) Chip(stringResource(R.string.schedule_distant), C.Cover, C.Primary)
                    if (isCurrent) {
                        val end = livePair?.timeEnd ?: lesson.timeEnd
                        val badgeText = if (end.isNotBlank()) {
                            stringResource(R.string.schedule_status_current_until, end)
                        } else {
                            stringResource(R.string.schedule_status_current)
                        }
                        Chip(badgeText, C.Today, C.Text)
                    }
                    if (isCancelled) Chip(stringResource(R.string.schedule_status_cancelled), C.RedTint, C.Red)
                }
                Spacer(Modifier.height(D.Tiny))
                Text(
                    lesson.discipline,
                    color = if (isPast || isCancelled) C.Secondary else C.Text,
                    style = T.Title,
                    textDecoration = textDecor,
                )
                Spacer(Modifier.height(D.Tiny))
                if (isCancelled && !lesson.cancellationReason.isNullOrBlank()) {
                    Text(
                        lesson.cancellationReason,
                        color = C.Red,
                        style = T.Small,
                    )
                    Spacer(Modifier.height(D.Tiny))
                }
                // В расписании группы — преподаватели, в расписании преподавателя — группа
                val detailsText = buildList {
                    if (!distant) {
                        lesson.room?.let { add(stringResource(R.string.schedule_room_short, it)) }
                    }
                    if (isTeacher) {
                        lesson.groupName?.let { add(it) }
                    } else {
                        lesson.teachers.takeIf { it.isNotEmpty() }?.let { add(it) }
                    }
                }.joinToString(stringResource(R.string.common_detail_separator))
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
    schedule: ScheduleWeek,
    weeks: List<WeekInfo>,
    selectedWeekStart: String?,
    isTeacher: Boolean,
    currentPairStatus: CurrentPairStatus?,
    onSelectWeek: (WeekInfo) -> Unit,
    onLessonDetails: (LessonItem) -> Unit,
) {
    // Текущая неделя — последняя доступная: листать в будущее нельзя
    val currentWeekStart = weeks.firstOrNull { it.isCurrent }?.startDate ?: weeks.lastOrNull()?.startDate
    val visibleWeeks = remember(weeks, currentWeekStart) {
        if (currentWeekStart == null) weeks
        else weeks.filter { it.startDate <= currentWeekStart }
    }
    val activeStart = selectedWeekStart ?: currentWeekStart ?: schedule.weekStart
    val activeIndex = visibleWeeks.indexOfFirst { it.startDate == activeStart }
    val activeWeek = visibleWeeks.getOrNull(activeIndex)

    Column(verticalArrangement = Arrangement.spacedBy(D.Ml)) {
        SchedionCard(color = C.Surface, outline = C.Outline) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = D.S, vertical = D.S),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(D.S),
            ) {
                if (activeIndex > 0) {
                    WeekArrow(Glyph.Back) { onSelectWeek(visibleWeeks[activeIndex - 1]) }
                } else {
                    Spacer(Modifier.width(D.ButtonSmall))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    activeWeek?.let { week ->
                        Text(
                            stringResource(R.string.schedule_week_start, week.number.toString()),
                            style = T.BodyStrong,
                        )
                    }
                    Text(
                        text = formatWeekRange(
                            activeWeek?.startDate ?: schedule.weekStart,
                            activeWeek?.endDate ?: schedule.weekEnd,
                        ),
                        color = C.Muted,
                        style = T.Label,
                    )
                }
                if (activeIndex in 0 until visibleWeeks.lastIndex) {
                    WeekArrow(Glyph.Next) { onSelectWeek(visibleWeeks[activeIndex + 1]) }
                } else {
                    Spacer(Modifier.width(D.ButtonSmall))
                }
            }
        }

        schedule.days.forEach { day ->
            WeeklyDayHeader(day)
            if (day.lessons.isEmpty()) {
                EmptyWeekDay()
            } else {
                SchedionCard {
                    day.lessons.forEach { lesson ->
                        WeeklyLessonRow(
                            lesson = lesson,
                            isTeacher = isTeacher,
                            isCurrent = day.isToday && currentPairStatus?.pairNumber == lesson.pairNumber,
                            onClick = { onLessonDetails(lesson) },
                        )
                    }
                }
            }
        }
    }
}

/** Заголовок дня недели: «Пн, 09.11»; сегодняшний день отличается только чипом «Сегодня». */
@Composable
private fun WeeklyDayHeader(day: ScheduleDay) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(D.Sm),
    ) {
        Text(
            text = stringResource(R.string.schedule_day_date, day.dayOfWeek, formatShortDate(day.date)),
            style = T.BodyStrong,
        )
        if (day.isToday) Chip(stringResource(R.string.schedule_today), C.Today, C.White)
    }
}

/**
 * Строка пары в расписании на неделю: номер, время и занятие с аудиторией в одну строку.
 * Текущая пара выделена золотым фоном, отменённая показана красным с пометкой «отменена».
 */
@Composable
private fun WeeklyLessonRow(
    lesson: LessonItem,
    isTeacher: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val cancelled = lesson.status == LessonStatus.Cancelled
    val distant = isDistantRoom(lesson.room)
    val details = if (cancelled) {
        listOf(stringResource(R.string.schedule_status_cancelled_inline))
    } else {
        buildList {
            if (!distant) lesson.room?.takeIf { it.isNotBlank() }?.let { add(it) }
            if (isTeacher) lesson.groupName?.let { add(it) }
            if (distant) add(stringResource(R.string.schedule_distant))
            lesson.subgroup?.let { add(stringResource(R.string.schedule_subgroup_short, it)) }
        }
    }
    val accent = when {
        cancelled -> C.Red
        isCurrent -> C.AmberText
        else -> C.Muted
    }
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (isCurrent) Modifier.background(C.GoldTint) else Modifier)
            .clickable { onClick() }
            .padding(horizontal = D.Page, vertical = D.Ml),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            lesson.pairNumber.toString(),
            Modifier.width(D.WeekPairColumn),
            color = accent,
            style = T.LabelStrong,
        )
        Text(
            lesson.timeStart.ifEmpty { stringResource(R.string.common_time_unknown) },
            Modifier.width(D.WeekTimeColumn),
            color = if (cancelled) C.Red else if (isCurrent) C.AmberText else C.Text,
            style = if (isCurrent) T.BodyStrong else T.Body,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(D.Tiny)) {
            Text(
                (listOf(lesson.discipline) + details).joinToString(stringResource(R.string.common_detail_separator)),
                color = if (cancelled) C.Red else C.Text,
                style = if (isCurrent) T.BodyStrong else T.Body,
            )
            if (!cancelled) {
                lesson.associationName?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = C.Purple, style = T.Small)
                }
            }
        }
    }
}

/** День без пар: красная карточка с причиной-заглушкой. */
@Composable
private fun EmptyWeekDay() {
    SchedionCard(color = C.RedTint, outline = C.RedOutline) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = D.Page, vertical = D.Ml),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(D.Ml),
        ) {
            StatusDot(C.RedDot)
            Column(verticalArrangement = Arrangement.spacedBy(D.Tiny)) {
                Text(stringResource(R.string.state_no_lessons), color = C.Red, style = T.BodyStrong)
                Text(stringResource(R.string.state_no_lessons_hint), color = C.Secondary, style = T.Small)
            }
        }
    }
}

@Composable
private fun WeekArrow(glyph: Glyph, onClick: () -> Unit) {
    val shape = RoundedCornerShape(D.ControlRadius)
    Box(
        Modifier
            .size(D.ButtonSmall)
            .clip(shape)
            .background(C.Past, shape)
            .border(D.Hairline, C.Outline, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        LineIcon(glyph, tint = C.Primary, size = D.IconSmall)
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
        Segments(
            listOf(stringResource(R.string.schedule_day), stringResource(R.string.schedule_week)),
            active = if (weekly) 1 else 0,
            edgePadding = D.Xl,
        ) { index ->
            if (index == 0 && weekly) onDay()
            if (index == 1 && !weekly) onWeek()
        }
    }
}

/** Пустой день: иконка, причина и переход к ближайшему дню с парами. */
@Composable
private fun EmptyDay(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    buttonLabel: String? = null,
    onOpen: (() -> Unit)? = null,
) {
    SchedionCard(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = D.Xl, vertical = D.Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LineIcon(Glyph.EmptyCalendar, tint = C.Muted, size = D.HeroIcon)
            Spacer(Modifier.height(D.Ml))
            Text(title, style = T.Lead, textAlign = TextAlign.Center)
            if (subtitle != null) {
                Spacer(Modifier.height(D.Sm))
                Text(subtitle, color = C.Secondary, style = T.Body, textAlign = TextAlign.Center)
            }
            if (buttonLabel != null && onOpen != null) {
                Spacer(Modifier.height(D.Page))
                SchedionButton(buttonLabel, primary = true, compact = true, onClick = onOpen)
            }
        }
    }
}

/** Название дня недели для фразы «Ближайшие занятия в понедельник». */
@Composable
private fun weekdayIn(date: String): String {
    val names = stringArrayResource(R.array.weekday_names_in)
    val index = runCatching { LocalDate.parse(date).dayOfWeek.value - 1 }.getOrDefault(0)
    return names[index.coerceIn(0, names.lastIndex)]
}
