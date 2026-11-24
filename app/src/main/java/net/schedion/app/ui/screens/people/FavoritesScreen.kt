package net.schedion.app.ui.screens.people

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt
import net.schedion.app.R
import net.schedion.app.domain.model.FavoriteKind
import net.schedion.app.domain.model.FavoriteSource
import net.schedion.app.ui.components.Glyph
import net.schedion.app.ui.components.LineIcon
import net.schedion.app.ui.components.MockCard
import net.schedion.app.ui.components.bottomSpacing
import net.schedion.app.ui.components.topSpacing
import net.schedion.app.ui.screens.schedule.ScheduleViewModel
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

@Composable
internal fun FavoritesScreen(
    viewModel: ScheduleViewModel,
    onBack: () -> Unit,
    onOpenSchedule: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val favorites by viewModel.favorites.collectAsState()

    FavoritesList(
        favorites = favorites,
        limit = viewModel.favoritesLimit,
        onBack = onBack,
        onOpen = { favorite ->
            viewModel.openFavorite(favorite)
            onOpenSchedule()
        },
        onRemove = viewModel::removeFavorite,
        onMove = viewModel::moveFavorite,
        modifier = modifier,
    )
}

private data class FavoriteDrag(
    val source: FavoriteSource,
    val height: Int,
    val grabOffset: Float,
    val pointerY: Float,
    val hasMoved: Boolean = false,
) {
    val top: Float get() = pointerY - grabOffset
    val center: Float get() = top + height / 2f
}

private fun FavoriteSource.listKey(): String = "$kind:$id"

@Composable
internal fun FavoritesList(
    favorites: List<FavoriteSource>,
    limit: Int,
    onBack: () -> Unit,
    onOpen: (FavoriteSource) -> Unit,
    onRemove: (FavoriteSource) -> Unit,
    onMove: (FavoriteSource, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val currentOnMove by rememberUpdatedState(onMove)
    val haptics by rememberUpdatedState(LocalHapticFeedback.current)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    val top = topSpacing()
    val bottom = bottomSpacing()
    var displayed by remember { mutableStateOf(favorites) }
    var dragged by remember { mutableStateOf<FavoriteDrag?>(null) }

    LaunchedEffect(favorites) {
        // Подтверждение предыдущего порядка из Room не должно прерывать новый жест.
        if (dragged == null || favorites.toSet() != displayed.toSet()) {
            dragged = null
            displayed = favorites
        }
    }

    fun movePreview(source: FavoriteSource, destination: Int) {
        val from = displayed.indexOf(source)
        if (from < 0 || from == destination) return
        // Удерживаем видимую позицию экрана, когда первая строка сама перемещается.
        listState.requestScrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        displayed = displayed.toMutableList().apply { add(destination, removeAt(from)) }
    }

    fun saveMove(source: FavoriteSource, destination: Int) {
        movePreview(source, destination)
        onMove(source, destination)
    }

    // Центры видимых строк задают место вставки даже при прокрутке и разной высоте строк.
    fun updatePreview(active: FavoriteDrag) {
        val key = active.source.listKey()
        val neighbors = listState.layoutInfo.visibleItemsInfo.filter { it.key is String && it.key != key }
        val next = neighbors.firstOrNull { active.center < it.offset + it.size / 2f }
        val anchor = next ?: neighbors.lastOrNull() ?: return
        val otherRows = displayed.filterNot { it == active.source }
        val anchorIndex = otherRows.indexOfFirst { it.listKey() == anchor.key }
        if (anchorIndex < 0) return
        movePreview(active.source, if (next == null) anchorIndex + 1 else anchorIndex)
    }

    val height by remember(listState) { derivedStateOf { listState.layoutInfo.viewportSize.height } }
    val edge = minOf(with(density) { D.FavoritesDragEdge.toPx() }, height / 2f)
    val pointerY = dragged?.takeIf { it.hasMoved }?.pointerY
    val scrollFraction = when {
        edge <= 0f || pointerY == null -> 0f
        pointerY < edge -> (pointerY - edge) / edge
        pointerY > height - edge -> (pointerY - height + edge) / edge
        else -> 0f
    }.coerceIn(-1f, 1f)
    val currentScrollFraction by rememberUpdatedState(scrollFraction)
    val scrollDirection = scrollFraction.compareTo(0f)
    LaunchedEffect(scrollDirection, density) {
        if (scrollDirection == 0) return@LaunchedEffect
        val maxStep = with(density) { D.FavoritesDragScrollStep.toPx() }
        while (dragged != null) {
            withFrameNanos { }
            if (listState.scrollBy(currentScrollFraction * maxStep) == 0f) break
            dragged?.let(::updatePreview)
        }
    }

    Box(
        modifier.fillMaxSize().padding(horizontal = D.Page).clipToBounds()
            .pointerInput(listState, isRtl) {
                val handleWidth = (D.FavoritesReorderHandle + D.Xs).toPx()
                val removeArea = (D.Page + D.NavIcon + D.Sm * 2).toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (displayed.size < 2) return@awaitEachGesture
                    val fromStart = if (isRtl) size.width - down.position.x else down.position.x
                    if (fromStart < removeArea) return@awaitEachGesture
                    val onHandle = size.width - fromStart <= handleWidth

                    val item = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                        down.position.y >= it.offset && down.position.y < it.offset + it.size
                    } ?: return@awaitEachGesture
                    val source = displayed.firstOrNull { it.listKey() == item.key } ?: return@awaitEachGesture
                    val pressed = (if (onHandle) down else awaitLongPressOrCancellation(down.id))
                        ?: return@awaitEachGesture
                    val row = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == item.key }
                        ?: return@awaitEachGesture
                    val initialOrder = displayed
                    dragged = FavoriteDrag(source, row.size, pressed.position.y - row.offset, pressed.position.y)
                    if (!onHandle) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    var completed = false
                    try {
                        // Захваченная строка получает движение раньше обработчика прокрутки списка.
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == pressed.id } ?: break
                            val active = dragged ?: break
                            if (change.isConsumed) break
                            change.consume()
                            if (!change.pressed) {
                                completed = change.previousPressed
                                break
                            }
                            if (change.position.y == active.pointerY) continue
                            if (onHandle && !active.hasMoved) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            val moved = active.copy(pointerY = change.position.y, hasMoved = true)
                            dragged = moved
                            updatePreview(moved)
                        }
                        if (completed) {
                            val destination = displayed.indexOf(source)
                            if (destination != initialOrder.indexOf(source)) currentOnMove(source, destination)
                        }
                    } finally {
                        if (!completed && dragged != null) displayed = initialOrder
                        dragged = null
                    }
                }
            },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottom + D.Ml),
            verticalArrangement = Arrangement.spacedBy(D.Xs),
        ) {
            item {
                FavoritesHeader(favorites.size, limit, onBack, Modifier.padding(top = top))
            }
            if (displayed.isEmpty()) item { EmptyFavorites() }
            itemsIndexed(displayed, key = { _, source -> source.listKey() }) { index, source ->
                val active = dragged
                if (active?.source == source) {
                    Spacer(Modifier.fillMaxWidth().height(with(density) { active.height.toDp() }))
                } else {
                    FavoriteRow(
                        favorite = source,
                        modifier = Modifier.animateItem(),
                        onOpen = { onOpen(source) },
                        onRemove = { onRemove(source) },
                        onMoveUp = if (index > 0) ({ saveMove(source, index - 1) }) else null,
                        onMoveDown = if (index < displayed.lastIndex) ({ saveMove(source, index + 1) }) else null,
                    )
                }
            }
        }

        val active = dragged
        if (active != null) {
            FavoriteRow(
                favorite = active.source,
                modifier = Modifier
                    .offset { IntOffset(x = 0, y = active.top.roundToInt()) }
                    .shadow(D.Sm, RoundedCornerShape(D.CardRadius))
                    .clearAndSetSemantics {},
            )
        }
    }
}

@Composable
private fun FavoritesHeader(count: Int, limit: Int, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(bottom = D.M)) {
        Row(Modifier.fillMaxWidth()) {
            Text("Отмена", Modifier.clickable(role = Role.Button, onClick = onBack), C.Secondary, style = T.BodyStrong)
            Spacer(Modifier.weight(1f))
            Text("Готово", Modifier.clickable(role = Role.Button, onClick = onBack).padding(end = D.Xs), C.Primary, style = T.BodyStrong)
        }
        Spacer(Modifier.height(D.Xl))
        Text("Избранное", style = T.Heading)
        Spacer(Modifier.height(D.Xs))
        Text(stringResource(R.string.favorites_count, count, limit), color = C.Muted, style = T.Label)
    }
}

@Composable
private fun EmptyFavorites() {
    MockCard(color = C.Past) {
        Column(
            Modifier.fillMaxWidth().padding(D.Page),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LineIcon(Glyph.Star, tint = C.Muted, size = D.HeroIcon)
            Spacer(Modifier.height(D.M))
            Text("В избранном пока пусто", style = T.Lead, textAlign = TextAlign.Center)
            Spacer(Modifier.height(D.Xs))
            Text(
                "Отметьте группу или преподавателя звёздочкой в поиске или в выборе расписания.",
                color = C.Secondary,
                style = T.Body,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FavoriteRow(
    favorite: FavoriteSource,
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = D.FavoritesRow)
            .clip(RoundedCornerShape(D.CardRadius)).background(C.Surface)
            .padding(start = D.Page, end = D.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(D.NavIcon + D.Sm)
                .then(if (onRemove != null) Modifier.clickable(role = Role.Button, onClick = onRemove) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            LineIcon(Glyph.Remove, tint = C.Red)
        }
        Column(
            Modifier.weight(1f).heightIn(min = D.FavoritesRow)
                .then(if (onOpen != null) Modifier.clickable(role = Role.Button, onClick = onOpen) else Modifier)
                .wrapContentHeight().padding(start = D.Page, top = D.S, bottom = D.S),
        ) {
            Text(favorite.title, style = T.Title)
            Text(
                favoriteLine(favorite),
                color = C.Muted,
                style = T.Label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val reorderLabel = stringResource(R.string.favorites_reorder, favorite.title)
        val moveUpLabel = stringResource(R.string.favorites_move_up)
        val moveDownLabel = stringResource(R.string.favorites_move_down)
        val moveActions = buildList {
            if (onMoveUp != null) add(CustomAccessibilityAction(moveUpLabel) {
                onMoveUp()
                true
            })
            if (onMoveDown != null) add(CustomAccessibilityAction(moveDownLabel) {
                onMoveDown()
                true
            })
        }
        Box(
            Modifier.size(D.FavoritesReorderHandle).semantics {
                if (moveActions.isNotEmpty()) {
                    contentDescription = reorderLabel
                    customActions = moveActions
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            LineIcon(Glyph.Reorder, tint = C.Muted)
        }
    }
}

/** Подпись строки избранного: тип источника и его уточнение. */
private fun favoriteLine(favorite: FavoriteSource): String {
    val kindLabel = if (favorite.kind == FavoriteKind.Group) "Группа" else "Преподаватель"
    val subtitle = favorite.subtitle
    return if (subtitle.isNullOrBlank()) kindLabel else "$kindLabel · $subtitle"
}
