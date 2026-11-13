package net.schedion.app.ui.screens.home

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.schedion.app.ui.components.PlaceholderContent

/**
 * Главный экран: пока показывает только переходы в основные разделы.
 *
 * @param onOpenSchedule вызывается при переходе к расписанию.
 * @param onOpenDocuments вызывается при переходе к документам.
 */
@Composable
fun HomeScreen(
    onOpenSchedule: () -> Unit,
    onOpenDocuments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderContent(
        title = "Главная",
        description = "Здесь появятся текущая неделя, ближайшие события и документы.",
        modifier = modifier,
    ) {
        OutlinedButton(onClick = onOpenSchedule) { Text(text = "Расписание") }
        OutlinedButton(onClick = onOpenDocuments) { Text(text = "Документы") }
    }
}
