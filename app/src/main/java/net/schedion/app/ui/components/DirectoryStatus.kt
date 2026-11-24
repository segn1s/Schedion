package net.schedion.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.schedion.app.R
import net.schedion.app.core.ContentState
import net.schedion.app.data.remote.dto.ScheduleFiltersDto
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

/** Показывает состояние загрузки справочников. */
@Composable
internal fun DirectoryStatus(
    state: ContentState<ScheduleFiltersDto>,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    showOfflineBanner: Boolean = true,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(D.Sm)) {
        if (state.data == null && (state.isRefreshing || state.error == null)) {
            Text(stringResource(R.string.search_directory_loading), style = T.Body)
            ContentSkeleton()
        } else if (!state.isRefreshing && state.error != null) {
            UiStateContent(state, onRetry, showOfflineBanner = showOfflineBanner, content = {})
        }
    }
}
