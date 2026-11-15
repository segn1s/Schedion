package net.schedion.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import net.schedion.app.ui.components.bottomSpacing
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D

/** Доля высоты экрана, которую может занимать панель боттом-шита. */
private const val PanelMaxHeightFraction = 0.85f

/**
 * Нативный модальный боттом-шит Schedion: затемнение фона и панель снизу с плавной анимацией.
 *
 * Панель прижата к нижней границе экрана, по высоте идёт от содержимого и не превышает 85% экрана;
 * длинное содержимое прокручивается внутри панели, поэтому шит никогда не раскрывается на весь экран.
 *
 * @param visibleState состояние показа шита: хост держит шит в композиции, пока проигрывается
 * анимация закрытия, и убирает его после её завершения.
 */
@Composable
internal fun SchedionBottomSheet(
    visibleState: MutableTransitionState<Boolean>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    tall: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Затемнение повторяет состояние панели, но анимируется отдельной переходной моделью,
    // чтобы две анимации не перепирали друг другу currentState.
    val scrimVisible = remember { MutableTransitionState(visibleState.currentState) }
    LaunchedEffect(visibleState.targetState) {
        scrimVisible.targetState = visibleState.targetState
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val maxPanelHeight = maxHeight * PanelMaxHeightFraction

        // Полупрозрачный фон (scrim)
        AnimatedVisibility(
            visibleState = scrimVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = fadeOut(animationSpec = tween(durationMillis = 150)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(C.Scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose,
                    ),
            )
        }

        // Панель боттом-шита
        AnimatedVisibility(
            visibleState = visibleState,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            ) + fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            ) + fadeOut(animationSpec = tween(durationMillis = 150)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxPanelHeight)
                    .background(C.Sheet, RoundedCornerShape(topStart = D.PanelRadius, topEnd = D.PanelRadius))
                    // Перехват касаний внутри шита, чтобы клик по пустому месту не закрывал его
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent(PointerEventPass.Final)
                        }
                    }
                    .semantics { paneTitle = if (tall) "Чьё расписание" else "Детали занятия" }
                    .padding(top = D.Sm, bottom = bottomSpacing() + D.Sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.width(D.SheetHandleWidth).height(D.Xs).background(C.Handle, CircleShape))
                Spacer(Modifier.height(if (tall) D.M else D.Sm))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    content = content,
                )
            }
        }
    }
}
