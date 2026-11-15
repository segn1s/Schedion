package net.schedion.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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

/** Нижняя панель с фиксированной геометрией и отступами из макета. */
@Composable
internal fun BoxScope.MockSheet(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    tall: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(C.Scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
    )
    val height = if (tall) Modifier.padding(top = D.SheetPickerTop).fillMaxSize() else Modifier.fillMaxWidth()
    Column(
        modifier.align(Alignment.BottomCenter).then(height)
            .background(C.Sheet, RoundedCornerShape(topStart = D.PanelRadius, topEnd = D.PanelRadius))
            // Панель перехватывает касания в пустых областях, чтобы они не закрывали её.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent(PointerEventPass.Final)
                }
            }
            .semantics { paneTitle = if (tall) "Чьё расписание" else "Детали занятия" }
            .padding(top = D.Sm, bottom = if (tall) bottomSpacing() else bottomSpacing() + D.Xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.width(D.SheetHandleWidth).height(D.Xs).background(C.Handle, CircleShape))
        Spacer(Modifier.height(if (tall) D.M else D.Sm))
        content()
    }
}
