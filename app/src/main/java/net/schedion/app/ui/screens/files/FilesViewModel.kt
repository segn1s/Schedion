package net.schedion.app.ui.screens.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.schedion.app.R
import net.schedion.app.data.files.FileActions
import net.schedion.app.data.remote.ContentImageLoader
import net.schedion.app.data.repository.DownloadsRepository
import net.schedion.app.domain.model.ContentFile

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val repository: DownloadsRepository,
    private val actions: FileActions,
    val images: ContentImageLoader,
) : ViewModel() {
    val downloads = repository.downloads
    private val messages = Channel<Int>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()

    fun download(file: ContentFile) { viewModelScope.launch {
        try {
            repository.download(file)
            messages.send(R.string.files_queued)
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) { messages.send(R.string.files_download_error) }
    } }

    fun open(file: ContentFile) { viewModelScope.launch {
        val uri = repository.downloadedUri(file)
        if (uri == null) messages.send(R.string.files_missing)
        else if (!actions.openFile(uri, file.mimeType)) messages.send(R.string.files_no_viewer)
    } }

    fun shareFile(file: ContentFile) { viewModelScope.launch {
        val uri = repository.downloadedUri(file)
        if (uri == null) messages.send(R.string.files_missing)
        else if (!actions.shareFile(uri, file.mimeType)) messages.send(R.string.files_action_error)
    } }

    fun shareLink(url: String) { if (!actions.shareText(url)) messages.trySend(R.string.files_action_error) }
    fun shareText(text: String) { if (!actions.shareText(text)) messages.trySend(R.string.files_action_error) }
    fun openLink(url: String) { if (!actions.openLink(url)) messages.trySend(R.string.files_link_error) }
}
