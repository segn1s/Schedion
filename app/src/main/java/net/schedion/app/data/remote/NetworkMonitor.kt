package net.schedion.app.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}

/** Следит за интернетом в сети по умолчанию, включая переключение Wi-Fi / мобильной сети. */
class AndroidNetworkMonitor @Inject constructor(@ApplicationContext context: Context) : NetworkMonitor {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    override val isOnline: Flow<Boolean> = callbackFlow {
        val initialNetwork = connectivity.activeNetwork
        trySend(connectivity.getNetworkCapabilities(initialNetwork).hasInternet())
        val callback = object : ConnectivityManager.NetworkCallback() {
            private var defaultNetwork: Network? = null

            override fun onAvailable(network: Network) {
                defaultNetwork = network
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                if (network == defaultNetwork) trySend(capabilities.hasInternet())
            }

            override fun onLost(network: Network) {
                if (network == defaultNetwork) {
                    defaultNetwork = null
                    trySend(false)
                }
            }
        }
        connectivity.registerDefaultNetworkCallback(callback)
        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged().conflate()

    private fun NetworkCapabilities?.hasInternet(): Boolean = this != null &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
