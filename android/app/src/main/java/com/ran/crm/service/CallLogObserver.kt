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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CallLogObserver(
        private val context: Context,
        handler: Handler = Handler(Looper.getMainLooper())
) : ContentObserver(handler) {

    private val parentJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + parentJob)

    /** Tracks the pending debounced sync so rapid changes coalesce into one. */
    private var pendingSyncJob: Job? = null

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)

        if (uri == null || uri == CallLog.Calls.CONTENT_URI) {
            SyncLogger.log("CallLogObserver: Change detected — debouncing sync")

            // Cancel any pending sync and restart the 5-second timer.
            // This ensures rapid successive changes (e.g. incoming call + voicemail)
            // only trigger a single sync after the dust settles.
            pendingSyncJob?.cancel()
            pendingSyncJob =
                    scope.launch {
                        delay(DEBOUNCE_MS)
                        try {
                            SyncLogger.log(
                                    "CallLogObserver: Debounce elapsed — triggering delta sync"
                            )
                            val syncManager = SyncManager.getInstance(context)
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
            pendingSyncJob?.cancel()
            parentJob.cancel("CallLogObserver unregistered")
            SyncLogger.log("CallLogObserver: Unregistered")
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 5_000L
    }
}
