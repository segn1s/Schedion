package net.schedion.app.ui.screens.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import net.schedion.app.R
import net.schedion.app.core.Resource
import net.schedion.app.ui.components.BackBar
import net.schedion.app.ui.components.Glyph
import net.schedion.app.ui.components.LineIcon
import net.schedion.app.ui.components.MockButton
import net.schedion.app.ui.components.MockCard
import net.schedion.app.ui.components.MockDivider
import net.schedion.app.ui.components.ScreenColumn
import net.schedion.app.ui.components.SkeletonBox
import net.schedion.app.ui.components.topSpacing
import net.schedion.app.ui.screens.documents.Document
import net.schedion.app.ui.screens.documents.DocumentRow
import net.schedion.app.ui.screens.documents.FileType
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

@Composable
internal fun NewsScreen(
    viewModel: NewsViewModel,
    onOpenArticle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.newsFeed.collectAsState()

    ScreenColumn(modifier) {
        Text(stringResource(R.string.news_title), style = T.Heading)
        Spacer(Modifier.height(D.L))

        when (val res = state) {
            is Resource.Loading -> {
                MockCard(large = true) {
                    SkeletonBox(height = D.NewsCover, shapeRadius = D.CardRadius)
                    Column(Modifier.padding(D.Page), verticalArrangement = Arrangement.spacedBy(D.Sm)) {
                        SkeletonBox(width = D.DetailsLabel, height = D.M)
                        SkeletonBox(height = D.Icon)
                        SkeletonBox(height = D.L)
                    }
                }
            }
            is Resource.Error -> {
                MockCard(outline = C.RedOutline) {
                    Column(Modifier.padding(D.Page), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.state_news_load_error), color = C.Red, style = T.Title)
                        Spacer(Modifier.height(D.M))
                        MockButton(stringResource(R.string.common_retry), compact = true, onClick = { viewModel.loadNews() })
                    }
                }
            }
            is Resource.Success -> {
                val items = res.data.items
                if (items.isNotEmpty()) {
                    val first = items.first()
                    MockCard(large = true, onClick = onOpenArticle) {
                        NewsCover(D.NewsCover, Modifier.padding(start = D.Ml, end = D.Ml, top = D.Ml))
                        Column(Modifier.padding(start = D.Page, end = D.Page, top = D.M, bottom = D.Xl - D.Tiny)) {
                            Text(first.date, color = C.Muted, style = T.Small)
                            Spacer(Modifier.height(D.Xs))
                            Text(first.title, style = T.ArticleHeading)
                            Spacer(Modifier.height(D.Xs))
                            Text(first.summary, color = C.Secondary, style = T.Body)
                        }
                    }

                    if (items.size > 1) {
                        Spacer(Modifier.height(D.L))
                        MockCard {
                            items.drop(1).forEachIndexed { index, news ->
                                if (index > 0) MockDivider()
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = D.Ml, vertical = D.Ml - D.Hairline),
                                    horizontalArrangement = Arrangement.spacedBy(D.Ml),
                                ) {
                                    Box(Modifier.size(D.NewsThumb).background(C.Cover, RoundedCornerShape(D.ControlRadius)))
                                    Column(Modifier.weight(1f)) {
                                        Text(news.date, color = C.Muted, style = T.Small)
                                        Spacer(Modifier.height(D.Xs))
                                        Text(news.title, style = T.Title)
                                        Spacer(Modifier.height(D.Tiny))
                                        Text(news.summary, color = C.Secondary, style = T.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(stringResource(R.string.state_news_empty), color = C.Muted, style = T.Body)
                }
            }
        }
    }
}

@Composable
internal fun NewsDetails(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
        Box(Modifier.fillMaxWidth().height(topSpacing() + D.Toolbar).background(C.Sheet))
        ScreenColumn(withTabs = false) {
            BackBar(stringResource(R.string.news_title), onBack) { LineIcon(Glyph.Share, tint = C.Primary, size = D.NavIcon) }
            Spacer(Modifier.height(D.Sm))
            NewsCover(D.ArticleCover)
            Spacer(Modifier.height(D.L))
            Text(stringResource(R.string.news_sample_date), color = C.Muted, style = T.Label)
            Spacer(Modifier.height(D.L))
            Text(stringResource(R.string.news_sample_title), style = T.Heading)
            Spacer(Modifier.height(D.L))
            Text(stringResource(R.string.news_sample_summary), style = T.Lead)
            Spacer(Modifier.height(D.L))
            Text(
                stringResource(R.string.news_sample_body),
                color = C.Secondary, style = T.ArticleBody,
            )
            Spacer(Modifier.height(D.Xl))
            Text(stringResource(R.string.news_sample_section), style = T.ArticleHeading)
            Spacer(Modifier.height(D.L))
            MockCard {
                DocumentRow(Document(R.string.news_sample_attachment_title, R.string.news_sample_attachment_meta, FileType.Pdf), attachment = true)
            }
            Spacer(Modifier.height(D.Ml))
            Row(horizontalArrangement = Arrangement.spacedBy(D.Sm), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.news_sample_external_link), color = C.Primary, style = T.BodyStrong)
                LineIcon(Glyph.External, tint = C.Primary, size = D.IconSmall)
            }
        }
    }
}

@Composable
private fun NewsCover(height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().height(height).background(C.Cover, RoundedCornerShape(D.CardRadius)),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.news_cover_placeholder), color = C.Primary, style = T.LabelStrong)
    }
}
