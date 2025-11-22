package com.ran.crm

import android.app.Application
import com.ran.crm.data.local.CrmDatabase

class CrmApplication : Application() {

    val database: CrmDatabase by lazy { CrmDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Initialize any application-wide components here
    }

    companion object {
        lateinit var instance: CrmApplication
            private set
    }
}
