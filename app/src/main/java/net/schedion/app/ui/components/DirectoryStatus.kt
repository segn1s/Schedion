package net.schedion.app.ui.components

import androidx.compose.runtime.Composable
import net.schedion.app.core.ContentState
import net.schedion.app.domain.model.ScheduleWeek

/** Справочники не выдают отсутствие сети за пустые результаты поиска. */
@Composable
internal fun DirectoryStatus(
    state: ContentState<ScheduleWeek>,
    hasItems: Boolean,
    onRetry: () -> Unit,
    showOfflineBanner: Boolean = true,
) {
    if (state.error != null || (!hasItems && state.data == null)) {
        UiStateContent(state, onRetry, showOfflineBanner = showOfflineBanner, content = {})
    }
}
