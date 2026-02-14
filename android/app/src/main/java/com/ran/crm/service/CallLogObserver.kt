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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CallLogObserver(
        private val context: Context,
        handler: Handler = Handler(Looper.getMainLooper())
) : ContentObserver(handler) {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)

        if (uri == null || uri == CallLog.Calls.CONTENT_URI) {
            SyncLogger.log("CallLogObserver: Change detected in Call Log. Triggering Sync.")

            scope.launch {
                try {
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
            job.cancel("CallLogObserver unregistered")
            SyncLogger.log("CallLogObserver: Unregistered")
        }
    }
}
