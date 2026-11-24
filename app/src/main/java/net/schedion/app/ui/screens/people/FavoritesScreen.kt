package net.schedion.app.ui.screens.people

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import net.schedion.app.R
import net.schedion.app.domain.model.FavoriteKind
import net.schedion.app.domain.model.FavoriteSource
import net.schedion.app.ui.components.Glyph
import net.schedion.app.ui.components.LineIcon
import net.schedion.app.ui.components.SchedionCard
import net.schedion.app.ui.components.SchedionDivider
import net.schedion.app.ui.components.bottomSpacing
import net.schedion.app.ui.components.topSpacing
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

@Composable
internal fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onBack: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenFavorite: (FavoriteSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    val draftFavorites by viewModel.draftFavorites.collectAsState()
    val lastRemoved by viewModel.lastRemoved.collectAsState()

    // Черновик пересобирается при каждом открытии экрана: «Отмена» не должна оставлять
    // в памяти правки прошлого захода.
    LaunchedEffect(Unit) { viewModel.resetDraft() }

    var showUndoSnackbar by remember { mutableStateOf(false) }
    var currentRemoved by remember { mutableStateOf<RemovedFavorite?>(null) }

    LaunchedEffect(lastRemoved) {
        if (lastRemoved != null) {
            currentRemoved = lastRemoved
            showUndoSnackbar = true
        }
    }

    // Каждое удаление перезапускает отчёт 4,5 секунды на отмену
    LaunchedEffect(showUndoSnackbar, currentRemoved) {
        if (showUndoSnackbar) {
            delay(4500)
            showUndoSnackbar = false
            viewModel.clearLastRemoved()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        FavoritesList(
            favorites = draftFavorites,
            limit = viewModel.favoritesLimit,
            onBack = onBack,
            onSave = {
                viewModel.saveFavorites()
                onBack()
            },
            onOpen = { favorite ->
                onOpenFavorite(favorite)
                onOpenSchedule()
            },
            onRemove = { favorite ->
                viewModel.removeFavorite(favorite)
            },
            onMove = viewModel::moveFavorite,
            onUndoRemove = if (showUndoSnackbar && currentRemoved != null) ({
                viewModel.undoRemove()
                showUndoSnackbar = false
            }) else null,
            removedItem = if (showUndoSnackbar) currentRemoved?.favorite else null,
        )
    }
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

/** Положение строки избранного в координатах списка. */
private data class FavoriteRowBounds(val top: Float, val height: Float)

private fun FavoriteSource.listKey(): String = "$kind:$id"

/**
 * Ждёт долгое нажатие, не обращая внимания на потребление down-события кликабельными строками.
 *
 * Стандартный [androidx.compose.foundation.gestures.awaitLongPressOrCancellation] отменяется,
 * как только дочерний клик помечает событие использованным, поэтому перетаскивание карточки
 * не начиналось. Здесь down читается на проходе [PointerEventPass.Initial], а жест отменяется
 * только при движении или отпускании пальца.
 */
private suspend fun AwaitPointerEventScope.awaitInitialLongPress(down: PointerInputChange): PointerInputChange? {
    var current = down
    return withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.all { !it.pressed }) return@withTimeoutOrNull null
            val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull null
            if (!change.pressed) return@withTimeoutOrNull null
            if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                return@withTimeoutOrNull null
            }
            current = change
        }
        @Suppress("UNREACHABLE_CODE")
        current
    } ?: current
}

@Composable
internal fun FavoritesList(
    favorites: List<FavoriteSource>,
    limit: Int,
    onBack: () -> Unit,
    onOpen: (FavoriteSource) -> Unit,
    onRemove: (FavoriteSource) -> Unit,
    onMove: (FavoriteSource, Int) -> Unit,
    onSave: (() -> Unit)? = null,
    onUndoRemove: (() -> Unit)? = null,
    removedItem: FavoriteSource? = null,
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
    // Строки лежат внутри одного элемента LazyColumn, поэтому их геометрию измеряем сами:
    // без этого жест не может понять, какую карточку тянут и куда её переместить.
    val bounds = remember { mutableStateMapOf<String, FavoriteRowBounds>() }
    var listTop by remember { mutableStateOf(0f) }

    LaunchedEffect(favorites) {
        if (dragged == null || favorites.toSet() != displayed.toSet()) {
            dragged = null
            displayed = favorites
        }
    }

    fun movePreview(source: FavoriteSource, destination: Int) {
        val from = displayed.indexOf(source)
        if (from < 0 || from == destination) return
        displayed = displayed.toMutableList().apply { add(destination, removeAt(from)) }
    }

    fun saveMove(source: FavoriteSource, destination: Int) {
        movePreview(source, destination)
        onMove(source, destination)
    }

    /** Индекс, на который встанет перетаскиваемая карточка по её текущему центру. */
    fun destinationFor(active: FavoriteDrag): Int {
        val others = displayed.filterNot { it == active.source }
        others.forEachIndexed { index, favorite ->
            val row = bounds[favorite.listKey()] ?: return@forEachIndexed
            if (active.center < row.top + row.height / 2f) return index
        }
        return others.size
    }

    fun updatePreview(active: FavoriteDrag) {
        val destination = destinationFor(active)
        if (displayed.indexOf(active.source) != destination) movePreview(active.source, destination)
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
        val maxSpeed = with(density) { D.FavoritesDragScrollStep.toPx() }
        while (true) {
            val fraction = currentScrollFraction
            if (fraction == 0f) break
            listState.scrollBy(fraction * maxSpeed)
            dragged?.let(::updatePreview)
            withFrameNanos { }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .padding(horizontal = D.Page)
            .clipToBounds()
            .onGloballyPositioned { listTop = it.positionInRoot().y }
            .pointerInput(listState, isRtl) {
                val handleWidth = (D.FavoritesReorderHandle + D.Xs).toPx()
                val removeArea = (D.Page + D.NavIcon + D.Sm * 2).toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (displayed.size < 2) return@awaitEachGesture
                    val fromStart = if (isRtl) size.width - down.position.x else down.position.x
                    if (fromStart < removeArea) return@awaitEachGesture
                    val onHandle = size.width - fromStart <= handleWidth

                    val hit = displayed.firstOrNull { favorite ->
                        val row = bounds[favorite.listKey()] ?: return@firstOrNull false
                        down.position.y >= row.top && down.position.y < row.top + row.height
                    } ?: return@awaitEachGesture
                    val hitRow = bounds[hit.listKey()] ?: return@awaitEachGesture
                    val pressed = (if (onHandle) down else awaitInitialLongPress(down))
                        ?: return@awaitEachGesture
                    val initialOrder = displayed
                    dragged = FavoriteDrag(
                        source = hit,
                        height = hitRow.height.roundToInt(),
                        grabOffset = pressed.position.y - hitRow.top,
                        pointerY = pressed.position.y,
                    )
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    var completed = false
                    try {
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
                            val moved = active.copy(pointerY = change.position.y, hasMoved = true)
                            dragged = moved
                            updatePreview(moved)
                        }
                        if (completed) {
                            val destination = displayed.indexOf(hit)
                            if (destination != initialOrder.indexOf(hit)) currentOnMove(hit, destination)
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
            contentPadding = PaddingValues(bottom = bottom + 80.dp),
        ) {
            item {
                FavoritesHeader(
                    onBack = onBack,
                    onSave = onSave ?: onBack,
                    modifier = Modifier.padding(top = top),
                )
            }

            if (displayed.isEmpty()) {
                item { EmptyFavorites() }
            } else {
                item {
                    SchedionCard(
                        color = C.Surface,
                        outline = C.Outline,
                    ) {
                        displayed.forEachIndexed { index, source ->
                            if (index > 0) SchedionDivider()
                            val active = dragged
                            if (active?.source == source) {
                                Spacer(Modifier.fillMaxWidth().height(with(density) { active.height.toDp() }))
                            } else {
                                FavoriteRow(
                                    favorite = source,
                                    modifier = Modifier.onGloballyPositioned { coordinates ->
                                        bounds[source.listKey()] = FavoriteRowBounds(
                                            top = coordinates.positionInRoot().y - listTop,
                                            height = coordinates.size.height.toFloat(),
                                        )
                                    },
                                    onOpen = { onOpen(source) },
                                    onRemove = { onRemove(source) },
                                    onMoveUp = if (index > 0) ({ saveMove(source, index - 1) }) else null,
                                    onMoveDown = if (index < displayed.lastIndex) ({ saveMove(source, index + 1) }) else null,
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(D.M))
                    Text(
                        text = stringResource(R.string.favorites_count, displayed.size, limit),
                        color = C.Muted,
                        style = T.Label,
                    )
                }
            }
        }

        // Перетаскиваемая плашка поверх списка
        val active = dragged
        if (active != null) {
            FavoriteRow(
                favorite = active.source,
                isMoving = true,
                modifier = Modifier
                    .offset { IntOffset(x = 0, y = active.top.roundToInt()) }
                    .shadow(D.Sm, RoundedCornerShape(D.CardRadius))
                    .clearAndSetSemantics {},
            )
        }

        // Всплывающий снекбар отмены удаления
        AnimatedVisibility(
            visible = removedItem != null && onUndoRemove != null,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(220)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(180)) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottom + D.M),
        ) {
            if (removedItem != null && onUndoRemove != null) {
                UndoRemovalSnackbar(
                    title = removedItem.title,
                    onUndo = onUndoRemove,
                )
            }
        }
    }
}

@Composable
private fun FavoritesHeader(
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(bottom = D.L)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.common_cancel),
                modifier = Modifier.clickable(role = Role.Button, onClick = onBack),
                color = C.Secondary,
                style = T.BodyStrong,
            )
            Text(
                text = stringResource(R.string.common_done),
                modifier = Modifier.clickable(role = Role.Button, onClick = onSave),
                color = C.Primary,
                style = T.BodyStrong,
            )
        }
        Spacer(Modifier.height(D.L))
        Text(
            text = stringResource(R.string.favorites_title),
            style = T.Heading,
        )
        Spacer(Modifier.height(D.Xs))
        Text(
            text = stringResource(R.string.favorites_hint),
            color = C.Secondary,
            style = T.Body,
        )
    }
}

@Composable
private fun EmptyFavorites() {
    SchedionCard(color = C.Past) {
        Column(
            Modifier.fillMaxWidth().padding(D.Page),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LineIcon(Glyph.Star, tint = C.Muted, size = D.HeroIcon)
            Spacer(Modifier.height(D.M))
            Text(stringResource(R.string.favorites_empty_title), style = T.Lead, textAlign = TextAlign.Center)
            Spacer(Modifier.height(D.Xs))
            Text(
                stringResource(R.string.favorites_empty_hint),
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
    isMoving: Boolean = false,
    onOpen: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    val removeLabel = stringResource(R.string.favorites_remove_source, favorite.title)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = D.FavoritesRow)
            .background(C.Surface)
            .padding(horizontal = D.Page, vertical = D.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(D.NavIcon + D.Sm)
                .then(
                    if (onRemove != null) {
                        Modifier
                            .semantics { contentDescription = removeLabel }
                            .clickable(role = Role.Button, onClick = onRemove)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            LineIcon(Glyph.Remove, tint = C.Red)
        }

        Spacer(Modifier.width(D.S))

        Column(
            Modifier
                .weight(1f)
                .then(if (onOpen != null) Modifier.clickable(role = Role.Button, onClick = onOpen) else Modifier)
                .wrapContentHeight(),
        ) {
            Text(favorite.title, style = T.BodyStrong)
            val kindLabel = if (favorite.kind == FavoriteKind.Group) {
                stringResource(R.string.common_group)
            } else {
                stringResource(R.string.common_teacher)
            }
            // Без перетаскивания вторая строка — только тип источника
            val subtitleText = if (isMoving) {
                stringResource(R.string.favorites_moving, kindLabel)
            } else {
                kindLabel
            }
            Text(
                text = subtitleText,
                color = C.Muted,
                style = T.Small,
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

@Composable
private fun UndoRemovalSnackbar(
    title: String,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(D.FloatingShadow, shape, spotColor = C.Shadow)
            .background(C.Surface, shape)
            .padding(horizontal = D.Page, vertical = D.Ml),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Зелёная точка с полупрозрачным ореолом, как в макете
        Box(
            Modifier
                .size(28.dp)
                .background(C.Success.copy(alpha = .14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(10.dp).background(C.Success, CircleShape))
        }
        Spacer(Modifier.width(D.S))
        Text(
            text = stringResource(R.string.favorites_removed, title),
            modifier = Modifier.weight(1f),
            style = T.Body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(D.S))
        Text(
            text = stringResource(R.string.favorites_undo),
            color = C.Primary,
            style = T.BodyStrong,
            modifier = Modifier.clickable(role = Role.Button, onClick = onUndo),
        )
    }
}
