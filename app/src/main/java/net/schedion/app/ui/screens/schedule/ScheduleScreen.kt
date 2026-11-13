package net.schedion.app.ui.screens.schedule

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.schedion.app.ui.components.PlaceholderContent

/**
 * Экран расписания занятий.
 *
 * @param onNavigateBack вызывается по кнопке возврата на предыдущий экран.
 */
@Composable
fun ScheduleScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderContent(
        title = "Расписание",
        description = "Здесь появится неделя с занятиями, звонками и отменами.",
        modifier = modifier,
    ) {
        OutlinedButton(onClick = onNavigateBack) { Text(text = "Назад") }
    }
}
