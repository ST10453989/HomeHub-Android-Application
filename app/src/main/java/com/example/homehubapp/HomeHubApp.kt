package com.example.homehubapp

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class HomeHubApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Re-apply the language the user chose in Settings (en / zu / xh)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(SettingsStore.language(this))
        )

        NotificationHelper.createChannel(this)

        // Whenever the phone gets a working connection, send any offline requests
        val cm = getSystemService(ConnectivityManager::class.java)
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    Log.d("HomeHub", "Connection available - checking for offline requests")
                    SyncManager.syncPending(applicationContext)
                }
            }
        })
    }
}