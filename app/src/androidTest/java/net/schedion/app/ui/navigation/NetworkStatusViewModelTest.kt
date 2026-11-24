package net.schedion.app.ui.navigation

import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import net.schedion.app.data.remote.NetworkMonitor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NetworkStatusViewModelTest {
    @Test fun shortOutageDoesNotShowTheBanner(): Unit = runBlocking {
        withNetwork { viewModel, online ->
            online.value = false
            viewModel.await(NetworkStatus.Interrupted)
            delay(2_000)
            assertEquals(NetworkStatus.Interrupted, viewModel.status.value)
            online.value = true
            viewModel.await(NetworkStatus.Online)
            delay(8_200)
            assertEquals(NetworkStatus.Online, viewModel.status.value)
        }
    }

    @Test fun sustainedOutageShowsTheBannerUntilRecovery(): Unit = runBlocking {
        withNetwork { viewModel, online ->
            online.value = false
            viewModel.await(NetworkStatus.Interrupted)
            viewModel.await(NetworkStatus.Offline)
            online.value = true
            viewModel.await(NetworkStatus.Online)
        }
    }

    private suspend fun withNetwork(test: suspend (NetworkStatusViewModel, MutableStateFlow<Boolean>) -> Unit) {
        val online = MutableStateFlow(true)
        val monitor = object : NetworkMonitor {
            override val isOnline = online
        }
        val store = ViewModelStore()
        val viewModel = withContext(Dispatchers.Main) {
            NetworkStatusViewModel(monitor).also { store.put("network", it) }
        }
        try {
            viewModel.await(NetworkStatus.Online)
            test(viewModel, online)
        } finally {
            withContext(Dispatchers.Main) { store.clear() }
        }
    }

    private suspend fun NetworkStatusViewModel.await(expected: NetworkStatus) {
        withTimeout(12_000) { status.first { it == expected } }
    }
}
