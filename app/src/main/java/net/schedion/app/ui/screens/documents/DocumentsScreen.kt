package net.schedion.app.ui.screens.documents

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import net.schedion.app.R
import net.schedion.app.ui.components.BackBar
import net.schedion.app.ui.components.Chip
import net.schedion.app.ui.components.Glyph
import net.schedion.app.ui.components.LineIcon
import net.schedion.app.ui.components.MockButton
import net.schedion.app.ui.components.MockCard
import net.schedion.app.ui.components.MockDivider
import net.schedion.app.ui.components.ScreenColumn
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

internal enum class FileType(@param:StringRes @field:StringRes val labelRes: Int, val color: Color) {
    Pdf(R.string.docs_format_pdf, C.Pdf), Xlsx(R.string.docs_format_xlsx, C.Xlsx), Docx(R.string.docs_format_docx, C.Docx),
}

internal data class Document(
    @param:StringRes @field:StringRes val titleRes: Int,
    @param:StringRes @field:StringRes val metaRes: Int,
    val type: FileType,
)

private val calendar = Document(R.string.docs_sample_calendar_title, R.string.docs_sample_calendar_meta, FileType.Pdf)
private val academicDocuments = listOf(
    calendar,
    Document(R.string.docs_sample_consultations_title, R.string.docs_sample_consultations_meta, FileType.Xlsx),
    Document(R.string.docs_sample_assessment_title, R.string.docs_sample_assessment_meta, FileType.Pdf),
)
private val forms = listOf(
    Document(R.string.docs_sample_leave_title, R.string.docs_sample_leave_meta, FileType.Docx),
    Document(R.string.docs_sample_transfer_title, R.string.docs_sample_transfer_meta, FileType.Docx),
)

@Composable
internal fun DocumentsScreen(onOpenCalendar: () -> Unit, modifier: Modifier = Modifier) {
    ScreenColumn(modifier) {
        Text(stringResource(R.string.docs_title), style = T.Heading)
        Spacer(Modifier.height(D.L))
        Row(horizontalArrangement = Arrangement.spacedBy(D.Sm)) {
            listOf(stringResource(R.string.docs_category_all), stringResource(R.string.docs_category_academic), stringResource(R.string.docs_category_forms)).forEachIndexed { index, label ->
                val active = index == 0
                Chip(
                    label,
                    if (active) C.Primary else C.Surface,
                    if (active) C.White else C.Primary,
                    outline = if (active) C.Primary else C.Outline,
                    padding = PaddingValues(horizontal = D.L, vertical = D.Sm),
                )
            }
        }
        Spacer(Modifier.height(D.L))
        DocumentSection(stringResource(R.string.docs_category_academic), academicDocuments, onOpenCalendar)
        Spacer(Modifier.height(D.L))
        DocumentSection(stringResource(R.string.docs_category_forms), forms, onOpenCalendar)
    }
}

@Composable
private fun DocumentSection(label: String, documents: List<Document>, onOpenCalendar: () -> Unit) {
    Text(label, color = C.Secondary, style = T.LabelStrong)
    Spacer(Modifier.height(D.Sm))
    MockCard {
        documents.forEachIndexed { index, document ->
            if (index > 0) MockDivider()
            DocumentRow(document, onClick = onOpenCalendar.takeIf { document == calendar })
        }
    }
}

@Composable
internal fun DocumentRow(
    document: Document,
    modifier: Modifier = Modifier,
    attachment: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = if (attachment) D.FileHeight + D.Xxl else D.DocumentRow)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(horizontal = D.L, vertical = D.M),
        horizontalArrangement = Arrangement.spacedBy(D.Ml),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FileBadge(document.type)
        Column(Modifier.weight(1f)) {
            Text(stringResource(document.titleRes), style = T.Title)
            Spacer(Modifier.height(D.Tiny))
            Text(stringResource(document.metaRes), color = C.Muted, style = T.Label)
        }
        if (attachment) {
            Text(stringResource(R.string.common_download), color = C.Primary, style = T.LabelStrong)
        } else {
            LineIcon(Glyph.Download, tint = C.Primary)
        }
    }
}

@Composable
private fun FileBadge(type: FileType, large: Boolean = false) {
    val shape = RoundedCornerShape(
        topStart = if (large) D.S else D.Xs,
        topEnd = if (large) D.Xl else D.Ml,
        bottomStart = if (large) D.S else D.Xs,
        bottomEnd = if (large) D.S else D.Xs,
    )
    Box(
        Modifier.size(
            width = if (large) D.LargeFileWidth else D.FileWidth,
            height = if (large) D.LargeFileHeight else D.FileHeight,
        ).background(type.color, shape).padding(bottom = if (large) D.Ml else D.S),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(stringResource(type.labelRes), color = C.White, style = if (large) T.LargeFile else T.File)
    }
}

@Composable
internal fun DocumentDetails(onBack: () -> Unit, modifier: Modifier = Modifier) {
    ScreenColumn(modifier) {
        BackBar(stringResource(R.string.docs_title), onBack)
        Spacer(Modifier.height(D.S))
        MockCard(large = true) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = D.DocumentHero).padding(D.Xxl),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FileBadge(FileType.Pdf, large = true)
                Spacer(Modifier.height(D.L))
                Text(stringResource(calendar.titleRes), style = T.SheetHeading, textAlign = TextAlign.Center)
                Spacer(Modifier.height(D.Page))
                Text(
                    stringResource(R.string.docs_sample_calendar_description),
                    color = C.Secondary, style = T.Body, textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(D.Page))
        MockCard {
            Column(Modifier.padding(D.Page), verticalArrangement = Arrangement.spacedBy(D.Ml)) {
                DetailRow(stringResource(R.string.docs_audience)) {
                    Column {
                        Chip(stringResource(R.string.docs_audience_students), C.PaleBlue, C.Primary)
                        Chip(stringResource(R.string.docs_audience_teachers), C.PaleBlue, C.Primary)
                    }
                }
                DetailRow(stringResource(R.string.docs_file)) { Text(stringResource(R.string.docs_sample_file_name), style = T.Body) }
                DetailRow(stringResource(R.string.docs_size)) { Text(stringResource(calendar.metaRes), style = T.Body) }
                DetailRow(stringResource(R.string.docs_updated)) { Text(stringResource(R.string.docs_sample_updated), style = T.Body) }
            }
        }
        Spacer(Modifier.height(D.Page))
        MockButton(stringResource(R.string.common_download), Modifier.fillMaxWidth(), primary = true, icon = Glyph.Download)
        Spacer(Modifier.height(D.Sm))
        MockButton(stringResource(R.string.docs_share_link), Modifier.fillMaxWidth())
        Spacer(Modifier.height(D.Page))
        Text(
            stringResource(R.string.docs_permanent_link_hint),
            modifier = Modifier.fillMaxWidth(),
            color = C.Muted, style = T.Label, textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun DetailRow(label: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, Modifier.width(D.DetailsLabel), C.Muted, style = T.Body)
        Column(Modifier.weight(1f)) { content() }
    }
}
