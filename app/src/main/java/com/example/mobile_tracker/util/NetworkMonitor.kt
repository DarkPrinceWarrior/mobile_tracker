package com.example.mobile_tracker.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager

    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                caps: NetworkCapabilities,
            ) {
                val hasInternet = caps.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET,
                )
                trySend(hasInternet)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET,
            )
            .build()

        connectivityManager.registerNetworkCallback(
            request,
            callback,
        )

        val currentNetwork =
            connectivityManager.activeNetwork
        val currentCaps = currentNetwork?.let {
            connectivityManager.getNetworkCapabilities(it)
        }
        val isConnected = currentCaps?.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET,
        ) == true
        trySend(isConnected)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(
                callback,
            )
        }
    }
}
