package net.schedion.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

/**
 * Диалог-уведомление внизу экрана: вопрос, отказ и основное действие в одной строке.
 *
 * Используется для предложения добавить источник в избранное.
 */
@Composable
internal fun SchedionPrompt(
    title: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SchedionCard(modifier = modifier.fillMaxWidth(), large = true) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = D.Page, vertical = D.Ml),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(D.Ml),
        ) {
            StatusDot(C.Primary)
            Spacer(Modifier.width(D.Sm))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = T.BodySemibold,
            )
            Text(
                text = dismissLabel,
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onDismiss)
                    .padding(horizontal = D.Sm, vertical = D.M),
                color = C.Secondary,
                style = T.BodyStrong,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(D.Tiny))
            SchedionButton(confirmLabel, primary = true, compact = true, onClick = onConfirm)
        }
    }
}
