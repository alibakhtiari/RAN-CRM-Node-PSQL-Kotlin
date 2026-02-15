package com.ran.crm.account

import android.app.Service
import android.content.Intent
import android.os.IBinder

/** Bound service that exposes [CrmSyncAdapter] to the Android sync framework. */
class CrmSyncService : Service() {

    companion object {
        private var syncAdapter: CrmSyncAdapter? = null
        private val lock = Object()
    }

    override fun onCreate() {
        super.onCreate()
        synchronized(lock) {
            if (syncAdapter == null) {
                syncAdapter = CrmSyncAdapter(applicationContext, true)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = syncAdapter!!.syncAdapterBinder
}
