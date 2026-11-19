package net.schedion.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import android.text.format.Formatter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.schedion.app.R
import net.schedion.app.data.repository.DownloadProgress
import net.schedion.app.data.repository.DownloadStatus
import net.schedion.app.domain.model.ContentFile
import net.schedion.app.ui.theme.SchedionColors as C
import net.schedion.app.ui.theme.SchedionDimens as D
import net.schedion.app.ui.theme.SchedionType as T

/** Одинаковые действия файлов для документов и вложений новостей. */
@Composable
internal fun ContentFileCard(
    file: ContentFile,
    progress: DownloadProgress?,
    onDownload: () -> Unit,
    onOpen: () -> Unit,
    onShareFile: () -> Unit,
    onShareLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MockCard(modifier) {
        Column(Modifier.padding(D.Page), verticalArrangement = Arrangement.spacedBy(D.Sm)) {
            Text(file.fileName, style = T.Title)
            Text(stringResource(R.string.files_metadata, fileLabel(file), Formatter.formatShortFileSize(LocalContext.current, file.size)), color = C.Muted, style = T.Small)
            when (progress?.status) {
                DownloadStatus.Queued, DownloadStatus.Running -> {
                    val percent = progress.percent
                    Text(if (percent == null) stringResource(R.string.files_waiting) else stringResource(R.string.files_progress, percent), style = T.Label)
                    if (percent == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                    else LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
                }
                DownloadStatus.Complete -> {
                    MockButton(stringResource(R.string.files_open), Modifier.fillMaxWidth(), primary = true, onClick = onOpen)
                    MockButton(stringResource(R.string.files_share), Modifier.fillMaxWidth(), onClick = onShareFile)
                }
                else -> {
                    if (progress?.status == DownloadStatus.Failed) Text(stringResource(R.string.files_download_error), color = C.Red, style = T.Label)
                    MockButton(stringResource(if (progress?.status == DownloadStatus.Failed) R.string.common_retry else R.string.common_download),
                        Modifier.fillMaxWidth(), primary = true, icon = Glyph.Download, onClick = onDownload)
                }
            }
            MockButton(stringResource(R.string.docs_share_link), Modifier.fillMaxWidth(), onClick = onShareLink)
        }
    }
}

private fun fileLabel(file: ContentFile): String = file.fileName.substringAfterLast('.', "").uppercase()
    .ifBlank { file.mimeType }
