package net.schedion.app.data.files

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import net.schedion.app.domain.model.articleLink

/** Открывает системные обработчики; доступ к файлу выдаётся только на content:// URI DownloadManager. */
@Singleton
class FileActions @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun openFile(uri: Uri, mimeType: String): Boolean = launch(
        Intent(Intent.ACTION_VIEW).setDataAndType(uri, mimeType)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
    )

    fun shareFile(uri: Uri, mimeType: String): Boolean = launchChooser(
        Intent(Intent.ACTION_SEND).setType(mimeType).putExtra(Intent.EXTRA_STREAM, uri)
            .apply { clipData = ClipData.newRawUri("", uri) }
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
    )

    fun shareText(text: String): Boolean = launchChooser(
        Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text),
    )

    fun openLink(value: String): Boolean {
        val url = articleLink(value) ?: return false
        return launch(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    private fun launchChooser(intent: Intent): Boolean = launch(Intent.createChooser(intent, null))
    private fun launch(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: android.content.ActivityNotFoundException) { false
    } catch (_: SecurityException) { false
    } catch (_: IllegalArgumentException) { false }
}
