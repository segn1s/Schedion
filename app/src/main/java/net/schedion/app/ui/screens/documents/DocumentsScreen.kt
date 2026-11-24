package net.schedion.app.ui.screens.documents

import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import net.schedion.app.R
import net.schedion.app.core.ContentState
import net.schedion.app.ui.components.*
import net.schedion.app.ui.screens.files.FilesViewModel
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

@Composable
internal fun DocumentsScreen(
    viewModel: DocumentsViewModel,
    files: FilesViewModel,
    onOpenDocument: (String) -> Unit,
    isOffline: Boolean,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.documents.collectAsState()
    val selected by viewModel.category.collectAsState()
    val downloads by files.downloads.collectAsState()
    val bottom = bottomSpacing() + D.NavHeight + D.Ml
    val context = LocalContext.current
    // Индикатор обновления держится не меньше трёх секунд, как и на расписании
    val refresh = rememberRefreshHandle { viewModel.refresh() }
    PullToRefreshBox(refresh.holding || state.isRefreshing, refresh.onRefresh, modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(start = D.Page, end = D.Page, top = topSpacing()), verticalArrangement = Arrangement.spacedBy(D.L)) {
            Text(stringResource(R.string.docs_title), style = T.Heading)
            if (isOffline) OfflineBanner(state.savedAt, onRefresh = refresh.onRefresh)
            UiStateContent(state, refresh.onRefresh, Modifier.weight(1f),
                scrollFallback = true,
                showOfflineBanner = false,
                emptyTitle = R.string.state_documents_empty, isEmpty = { it.isEmpty() }) { categories ->
                Column(verticalArrangement = Arrangement.spacedBy(D.Ml)) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(D.Sm)) {
                        CategoryChip(stringResource(R.string.docs_category_all), selected == null) { viewModel.selectCategory(null) }
                        categories.forEach { CategoryChip(it.title, it.title == selected) { viewModel.selectCategory(it.title) } }
                    }
                    val visible = categories.filter { selected == null || it.title == selected }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(D.Ml), contentPadding = PaddingValues(bottom = bottom)) {
                        visible.forEachIndexed { index, category ->
                            item(key = "category:$index") { Text(category.title, color = C.Secondary, style = T.LabelStrong) }
                            if (category.documents.isEmpty()) item { EmptyContent(R.string.state_documents_category_empty) }
                            items(category.documents, key = { "${category.title}:${it.slug}" }) { document ->
                                SchedionCard(onClick = { onOpenDocument(document.slug) }) {
                                    Column(Modifier.padding(D.Page), verticalArrangement = Arrangement.spacedBy(D.Sm)) {
                                        Text(document.title, style = T.Title)
                                        document.description?.takeIf { it.isNotBlank() }?.let { Text(it, color = C.Secondary, style = T.Body) }
                                        Text(stringResource(R.string.files_metadata, document.format, Formatter.formatShortFileSize(context, document.file.size)),
                                            color = C.Muted, style = T.Label)
                                        Text(stringResource(R.string.docs_updated_at, document.updatedAt), color = C.Muted, style = T.Small)
                                        document.audiences.forEach { audience ->
                                            Text(stringResource(if (audience == "STUDENTS") R.string.docs_audience_students else R.string.docs_audience_teachers),
                                                color = C.Secondary, style = T.Small)
                                        }
                                        val progress = downloads[document.file.key]
                                        if (progress != null) Text(stringResource(when (progress.status) {
                                            net.schedion.app.data.repository.DownloadStatus.Complete -> R.string.files_downloaded
                                            net.schedion.app.data.repository.DownloadStatus.Failed -> R.string.files_download_error
                                            else -> R.string.files_waiting
                                        }), color = C.Primary, style = T.Small)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Chip(label, if (selected) C.Primary else C.Surface, if (selected) C.White else C.Primary,
        modifier = Modifier.clickable(onClick = onClick), outline = C.Outline,
        padding = PaddingValues(horizontal = D.L, vertical = D.Sm))
}

@Composable
internal fun DocumentDetails(
    slug: String,
    viewModel: DocumentsViewModel,
    files: FilesViewModel,
    onBack: () -> Unit,
    isOffline: Boolean,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(slug) { viewModel.openDocument(slug) }
    val state by viewModel.document.collectAsState()
    val downloads by files.downloads.collectAsState()
    val shown = if (state.data == null || state.data?.slug == slug) state else ContentState()
    val bottom = bottomSpacing() + D.NavHeight + D.Page
    // Индикатор обновления держится не меньше трёх секунд, как и на расписании
    val refresh = rememberRefreshHandle { viewModel.refreshDocument() }
    PullToRefreshBox(refresh.holding || shown.isRefreshing, refresh.onRefresh, modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(start = D.Page, end = D.Page, top = topSpacing()), verticalArrangement = Arrangement.spacedBy(D.Ml)) {
            BackBar(stringResource(R.string.docs_title), onBack)
            if (isOffline) OfflineBanner(shown.savedAt, onRefresh = refresh.onRefresh)
            UiStateContent(shown, refresh.onRefresh, Modifier.weight(1f), scrollFallback = true,
                showOfflineBanner = false) { document ->
                LazyColumn(verticalArrangement = Arrangement.spacedBy(D.L), contentPadding = PaddingValues(bottom = bottom)) {
                    item { Text(document.title, style = T.Heading) }
                    document.description?.takeIf { it.isNotBlank() }?.let { description -> item { Text(description, style = T.ArticleBody) } }
                    item {
                        SchedionCard {
                            Column(Modifier.padding(D.Page), verticalArrangement = Arrangement.spacedBy(D.Ml)) {
                                if (document.audiences.isNotEmpty()) DetailRow(stringResource(R.string.docs_audience)) {
                                    document.audiences.forEach { audience ->
                                        Text(stringResource(if (audience == "STUDENTS") R.string.docs_audience_students else R.string.docs_audience_teachers), style = T.Body)
                                    }
                                }
                                DetailRow(stringResource(R.string.docs_updated)) { Text(document.updatedAt, style = T.Body) }
                            }
                        }
                    }
                    item { ContentFileCard(document.file, downloads[document.file.key],
                        { files.download(document.file) }, { files.open(document.file) },
                        { files.shareFile(document.file) }, { files.shareLink(document.file.url) }) }
                }
            }
        }
    }
}

@Composable
internal fun DetailRow(label: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.width(D.DetailsLabel), color = C.Muted, style = T.Body)
        Column(Modifier.weight(1f)) { content() }
    }
}
