package com.ran.crm.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder

class SyncService : Service() {
    
    companion object {
        private var sSyncAdapter: SyncAdapter? = null
        private val sSyncAdapterLock = Any()
    }

    override fun onCreate() {
        synchronized(sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = SyncAdapter(applicationContext, true)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return sSyncAdapter?.syncAdapterBinder
    }
}
