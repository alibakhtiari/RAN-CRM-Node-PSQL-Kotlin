package com.ran.crm

import android.app.Application
import com.ran.crm.data.local.CrmDatabase
import com.ran.crm.data.local.PreferenceManager
import com.ran.crm.work.SyncNotificationHelper
import com.ran.crm.work.SyncWorker

class CrmApplication : Application() {

    val database: CrmDatabase by lazy { CrmDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Create notification channel early so foreground workers can use it
        SyncNotificationHelper.createChannel(this)

        // Auto-schedule periodic sync if the user has a saved session.
        // This runs even when MainActivity hasn't been opened yet (e.g. after
        // device reboot or app update) so sync survives Activity restarts.
        val prefs = PreferenceManager(this)
        if (prefs.authToken != null) {
            val interval = prefs.syncIntervalMinutes
            if (interval > 0) {
                SyncWorker.schedulePeriodicSync(this, interval)
            }
        }
    }

    companion object {
        lateinit var instance: CrmApplication
            private set
    }
}
