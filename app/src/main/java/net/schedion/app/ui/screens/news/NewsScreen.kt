package net.schedion.app.ui.screens.news

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.flow.distinctUntilChanged
import net.schedion.app.R
import net.schedion.app.ui.components.*
import net.schedion.app.ui.screens.files.FilesViewModel
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

@Composable
internal fun NewsScreen(
    viewModel: NewsViewModel,
    files: FilesViewModel,
    onOpenArticle: (String) -> Unit,
    isOffline: Boolean,
    modifier: Modifier = Modifier,
) {
    val feed by viewModel.feed.collectAsState()
    val listState = rememberLazyListState()
    val items = feed.content.data?.items.orEmpty()
    val bottom = bottomSpacing() + D.NavHeight + D.Ml
    if (feed.hasMore && !feed.isLoadingMore && !feed.content.isRefreshing && feed.pageError == null && !feed.content.isOffline) {
        LaunchedEffect(items.size) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
                .distinctUntilChanged().collect { index ->
                    if (index >= items.size - 2) viewModel.loadMore()
                }
        }
    }
    PullToRefreshBox(isRefreshing = feed.content.isRefreshing, onRefresh = viewModel::refresh, modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(start = D.Page, end = D.Page, top = topSpacing()), verticalArrangement = Arrangement.spacedBy(D.L)) {
            Text(stringResource(R.string.news_title), style = T.Heading)
            if (isOffline) OfflineBanner(feed.content.savedAt)
            UiStateContent(
                state = feed.content,
                onRetry = viewModel::refresh,
                modifier = Modifier.weight(1f),
                scrollFallback = true,
                showOfflineBanner = false,
                emptyTitle = R.string.state_news_empty,
                isEmpty = { it.items.isEmpty() },
            ) { page ->
                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(D.Ml), contentPadding = PaddingValues(bottom = bottom)) {
                    itemsIndexed(page.items, key = { _, item -> item.slug }) { index, news ->
                        MockCard(large = index == 0, onClick = { onOpenArticle(news.slug) }) {
                            if (index == 0) {
                                news.coverUrl?.let { url ->
                                    RemoteImage(
                                        url, news.title, files.images,
                                        Modifier.padding(start = D.Ml, end = D.Ml, top = D.Ml)
                                            .fillMaxWidth().height(D.NewsCover).clip(RoundedCornerShape(D.CardRadius)),
                                        scale = ContentScale.Crop,
                                        alignment = BiasAlignment(0f, D.NewsCoverVerticalBias),
                                    )
                                }
                                Column(Modifier.padding(D.Page), verticalArrangement = Arrangement.spacedBy(D.Sm)) {
                                    Text(news.date, color = C.Muted, style = T.Small)
                                    Text(news.title, style = T.ArticleHeading)
                                    Text(news.summary, color = C.Secondary, style = T.Body, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                            } else {
                                Row(
                                    Modifier.fillMaxWidth().padding(D.Ml).heightIn(min = D.NewsThumb),
                                    horizontalArrangement = Arrangement.spacedBy(D.Ml),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    news.coverUrl?.let { url -> RemoteImage(url, news.title, files.images,
                                        Modifier.size(D.NewsThumb).clip(RoundedCornerShape(D.ControlRadius))) }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(D.Xs)) {
                                        Text(news.date, color = C.Muted, style = T.Small)
                                        Text(news.title, style = T.Title, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text(news.summary, color = C.Secondary, style = T.Label,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                    if (feed.isLoadingMore) item { ContentSkeleton() }
                    else if (feed.pageError != null) item {
                        MockButton(stringResource(R.string.news_retry_page), Modifier.fillMaxWidth(), onClick = { viewModel.loadMore(retry = true) })
                    }
                    else if (feed.hasMore) item {
                        MockButton(stringResource(R.string.news_load_more), Modifier.fillMaxWidth(), onClick = { viewModel.loadMore() })
                    }
                }
            }
        }
    }
}
