package com.ran.crm.service

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import com.ran.crm.sync.SyncManager
import com.ran.crm.utils.SyncLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallLogObserver(
        private val context: Context,
        handler: Handler = Handler(Looper.getMainLooper())
) : ContentObserver(handler) {

    private val syncManager = SyncManager(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)

        // Filter for specific URI if needed, but CallLog.Calls.CONTENT_URI is usually what we want
        if (uri == null || uri == CallLog.Calls.CONTENT_URI) {
            SyncLogger.log("CallLogObserver: Change detected in Call Log. Triggering Sync.")

            scope.launch {
                // Trigger delta sync
                // We could also use ContentResolver.requestSync but calling SyncManager directly
                // gives us more control and immediate execution if the app is alive.
                // Ideally, we should use WorkManager or SyncAdapter for reliability.
                // But for "immediate" reaction while app is running, this is fine.
                // For background, we rely on periodic sync.

                try {
                    syncManager.performDeltaSync()
                } catch (e: Exception) {
                    SyncLogger.log("CallLogObserver: Failed to trigger sync", e)
                }
            }
        }
    }

    private var isRegistered = false

    fun register() {
        if (!isRegistered) {
            context.contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, this)
            isRegistered = true
            SyncLogger.log("CallLogObserver: Registered")
        }
    }

    fun unregister() {
        if (isRegistered) {
            context.contentResolver.unregisterContentObserver(this)
            isRegistered = false
            SyncLogger.log("CallLogObserver: Unregistered")
        }
    }
}
