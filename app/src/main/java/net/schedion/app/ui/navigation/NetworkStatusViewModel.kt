package net.schedion.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import net.schedion.app.data.remote.NetworkMonitor

internal enum class NetworkStatus {
    Checking, Online, Interrupted, Offline,
}

@HiltViewModel
internal class NetworkStatusViewModel @Inject constructor(networkMonitor: NetworkMonitor) : ViewModel() {
    private val _status = MutableStateFlow(NetworkStatus.Checking)
    val status = _status.asStateFlow()

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.distinctUntilChanged().collectLatest { online ->
                if (online) {
                    _status.value = NetworkStatus.Online
                } else {
                    _status.value = NetworkStatus.Interrupted
                    delay(OfflineDelayMillis)
                    _status.value = NetworkStatus.Offline
                }
            }
        }
    }

    private companion object {
        const val OfflineDelayMillis = 10_000L
    }
}
