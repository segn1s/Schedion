package net.schedion.app.ui.screens.news

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import net.schedion.app.R
import net.schedion.app.core.ContentState
import net.schedion.app.ui.components.*
import net.schedion.app.ui.screens.files.FilesViewModel
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

@Composable
internal fun NewsDetails(
    slug: String,
    viewModel: NewsViewModel,
    files: FilesViewModel,
    onBack: () -> Unit,
    isOffline: Boolean,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(slug) { viewModel.openArticle(slug) }
    val article by viewModel.article.collectAsState()
    val blocks by viewModel.blocks.collectAsState()
    val downloads by files.downloads.collectAsState()
    val shown = if (article.data == null || article.data?.slug == slug) article else ContentState()
    val bottom = bottomSpacing() + D.Page
    PullToRefreshBox(shown.isRefreshing, viewModel::refreshArticle, modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(start = D.Page, end = D.Page, top = topSpacing()), verticalArrangement = Arrangement.spacedBy(D.Ml)) {
            BackBar(stringResource(R.string.news_title), onBack)
            if (isOffline) OfflineBanner(shown.savedAt, onRefresh = viewModel::refreshArticle)
            UiStateContent(shown, viewModel::refreshArticle, Modifier.weight(1f), scrollFallback = true,
                showOfflineBanner = false) { news ->
                val renderedImages = blocks.filterIsInstance<ArticleBlock.Image>().map { it.url }.toSet() + news.coverUrl
                val remainingImages = news.files.filter { it.mimeType.startsWith("image/") && it.url !in renderedImages }
                val attachments = news.files.filterNot { it.mimeType.startsWith("image/") }
                val firstParagraph = blocks.filterIsInstance<ArticleBlock.Text>().firstOrNull()
                    ?.runs?.joinToString("") { it.text }.orEmpty()
                val summaryIsInBody = firstParagraph.trim() == news.summary.trim()
                LazyColumn(verticalArrangement = Arrangement.spacedBy(D.L), contentPadding = PaddingValues(bottom = bottom)) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(D.L)) {
                            news.coverUrl?.let { url -> RemoteImage(url, news.title, files.images,
                                Modifier.fillMaxWidth().height(D.ArticleCover).clickable { files.openLink(url) }) }
                            Text(news.date, color = C.Muted, style = T.Label)
                            Text(news.title, style = T.Heading)
                            if (news.summary.isNotBlank() && !summaryIsInBody) Text(news.summary, style = T.Lead)
                        }
                    }
                    itemsIndexed(blocks) { index, block ->
                        when (block) {
                            is ArticleBlock.Text -> ArticleParagraph(block, files::openLink)
                            is ArticleBlock.Image -> if (block.url != news.coverUrl) RemoteImage(
                                block.url, stringResource(R.string.news_article_image, index + 1), files.images,
                                Modifier.fillMaxWidth().height(D.ArticleCover).clickable { files.openLink(block.url) }, ContentScale.Fit,
                            )
                        }
                    }
                    itemsIndexed(remainingImages, key = { _, image -> image.key }) { index, image ->
                        RemoteImage(image.url, stringResource(R.string.news_article_image, index + 1), files.images,
                            Modifier.fillMaxWidth().height(D.ArticleCover).clickable { files.openLink(image.url) }, ContentScale.Fit)
                    }
                    itemsIndexed(attachments, key = { _, file -> file.key }) { _, file ->
                        ContentFileCard(file, downloads[file.key], { files.download(file) }, { files.open(file) },
                            { files.shareFile(file) }, { files.shareLink(file.url) })
                    }
                    if (news.sourceUrl != null) item {
                        SchedionButton(stringResource(R.string.news_source), Modifier.fillMaxWidth(), icon = Glyph.External,
                            onClick = { files.openLink(news.sourceUrl) })
                    }
                    item {
                        SchedionButton(stringResource(R.string.news_share), Modifier.fillMaxWidth(), icon = Glyph.Share,
                            onClick = { files.shareText(listOfNotNull(news.title, news.summary, news.sourceUrl).joinToString("\n\n")) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleParagraph(block: ArticleBlock.Text, onLink: (String) -> Unit) {
    val bullet = stringResource(R.string.common_bullet)
    val text = buildAnnotatedString {
        if (block.bullet) append(bullet)
        block.runs.forEach { run ->
            val style = SpanStyle(fontWeight = if (run.bold) FontWeight.Bold else null,
                fontStyle = if (run.italic) FontStyle.Italic else null)
            if (run.link != null) {
                withLink(LinkAnnotation.Url(run.link,
                    styles = TextLinkStyles(style = SpanStyle(color = C.Primary, textDecoration = TextDecoration.Underline)),
                    linkInteractionListener = LinkInteractionListener { onLink(run.link) })) {
                    withStyle(style) { append(run.text) }
                }
            } else withStyle(style) { append(run.text) }
        }
    }
    Text(text,
        modifier = if (block.quote) Modifier.fillMaxWidth().background(C.Past).padding(D.Page) else Modifier,
        color = C.Text, style = if (block.heading) T.ArticleHeading else T.ArticleBody)
}
