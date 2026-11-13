package net.schedion.app.ui.screens.documents

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.schedion.app.ui.components.PlaceholderContent

/**
 * Экран документов и графиков учебного процесса.
 *
 * @param onNavigateBack вызывается по кнопке возврата на предыдущий экран.
 */
@Composable
fun DocumentsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderContent(
        title = "Документы",
        description = "Здесь появятся документы, сгруппированные по категориям.",
        modifier = modifier,
    ) {
        OutlinedButton(onClick = onNavigateBack) { Text(text = "Назад") }
    }
}
