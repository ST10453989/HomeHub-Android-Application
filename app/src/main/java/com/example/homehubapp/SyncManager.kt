package com.example.homehubapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.homehubapp.network.ApiClient
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

// Sends requests that were saved offline to the API once the phone is connected again
object SyncManager {

    private val syncing = AtomicBoolean(false)

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // onSynced is called on the main thread with the number of requests that were sent
    fun syncPending(context: Context, onSynced: ((Int) -> Unit)? = null) {
        val appContext = context.applicationContext
        val db = LocalDb.get(appContext)
        if (db.getPending().isEmpty() || !isOnline(appContext)) return
        if (!syncing.compareAndSet(false, true)) return

        Thread {
            var synced = 0
            try {
                for (pending in db.getPending()) {
                    try {
                        val response = ApiClient.service.createServiceRequest(pending.request).execute()
                        if (response.isSuccessful) {
                            db.deletePending(pending.localId)
                            synced++
                            Log.i("HomeHub", "Synced offline request ${pending.localId}")
                        } else if (response.code() in 400..499) {
                            // The server rejected it for good (e.g. date now in the past), so drop it
                            db.deletePending(pending.localId)
                            Log.w("HomeHub", "Offline request ${pending.localId} rejected: ${response.code()}")
                        }
                    } catch (e: IOException) {
                        Log.w("HomeHub", "Sync stopped, connection lost", e)
                        break
                    }
                }
            } finally {
                syncing.set(false)
            }

            if (synced > 0) {
                Handler(Looper.getMainLooper()).post {
                    if (SettingsStore.notificationsEnabled(appContext)) {
                        NotificationHelper.show(
                            appContext,
                            appContext.getString(R.string.notif_synced_title),
                            appContext.getString(R.string.msg_synced, synced)
                        )
                    }
                    onSynced?.invoke(synced)
                }
            }
        }.start()
    }
}