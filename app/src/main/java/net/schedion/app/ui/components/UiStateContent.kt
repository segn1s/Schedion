package net.schedion.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import net.schedion.app.R
import net.schedion.app.core.ContentState
import net.schedion.app.core.Resource
import retrofit2.HttpException
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

/** Сохраняет контент при обновлении и показывает способ повторить неудачную загрузку. */
@Composable
internal fun <T> UiStateContent(
    state: ContentState<T>,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
    scrollFallback: Boolean = false,
    showOfflineBanner: Boolean = true,
    @StringRes emptyTitle: Int = R.string.state_content_empty,
    isEmpty: (T) -> Boolean = { false },
    loading: @Composable () -> Unit = { ContentSkeleton() },
    content: @Composable (T) -> Unit,
) {
    val data = state.data
    val fallbackScroll = rememberScrollState()
    val stateModifier = if (scrollFallback && (data == null || isEmpty(data))) {
        modifier.fillMaxSize().verticalScroll(fallbackScroll)
    } else modifier
    Column(stateModifier, verticalArrangement = Arrangement.spacedBy(D.Ml)) {
        when {
            data != null -> {
                when {
                    state.isOffline && showOfflineBanner -> OfflineBanner(state.savedAt)
                    state.error != null && !state.isOffline -> ContentError(state, onRetry, compact = true)
                }
                if (isEmpty(data)) EmptyContent(emptyTitle) else content(data)
            }
            state.resource is Resource.Error -> {
                if (state.isOffline && !showOfflineBanner) {
                    Text(stringResource(R.string.state_load_error), style = T.Title)
                    if (onRetry != null) {
                        MockButton(stringResource(R.string.common_retry), compact = true, onClick = onRetry)
                    }
                } else ContentError(state, onRetry)
            }
            else -> loading()
        }
    }
}

@Composable
private fun <T> ContentError(state: ContentState<T>, onRetry: (() -> Unit)?, compact: Boolean = false) {
    MockCard(outline = if (state.isOffline) C.GoldOutline else C.RedOutline) {
        Column(Modifier.padding(D.Page), verticalArrangement = Arrangement.spacedBy(D.Sm)) {
            Text(stringResource(if (state.isOffline) R.string.state_offline else R.string.state_load_error), style = T.Title)
            val savedAt = state.savedAt
            Text(
                when {
                    compact && savedAt != null -> stringResource(R.string.state_saved_at, cacheTime(savedAt))
                    (state.error as? HttpException)?.code() == 404 -> stringResource(R.string.state_not_found)
                    state.isOffline -> stringResource(R.string.state_network_error)
                    else -> stringResource(R.string.state_server_error)
                }, color = C.Secondary, style = T.Body,
            )
            if (onRetry != null) {
                MockButton(stringResource(if (compact) R.string.common_refresh else R.string.common_retry), compact = true, onClick = onRetry)
            }
        }
    }
}

private fun cacheTime(time: Long): String = DateTimeFormatter.ofPattern("dd.MM, HH:mm")
    .format(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()))

@Composable
internal fun EmptyContent(@StringRes title: Int, modifier: Modifier = Modifier) {
    MockCard(modifier = modifier, color = C.Past) {
        Text(stringResource(title), Modifier.padding(D.Page), color = C.Muted, style = T.Body)
    }
}

@Composable
internal fun ContentSkeleton(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(D.Ml)) {
        repeat(3) {
            MockCard {
                Column(Modifier.fillMaxWidth().padding(D.Page), verticalArrangement = Arrangement.spacedBy(D.Sm)) {
                    SkeletonBox(height = D.Section)
                    SkeletonBox(height = D.Icon)
                    SkeletonBox(height = D.M)
                }
            }
        }
    }
}
