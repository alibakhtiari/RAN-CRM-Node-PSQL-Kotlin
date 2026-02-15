package com.ran.crm.account

import android.app.Service
import android.content.Intent
import android.os.IBinder

/** Bound service that exposes [CrmAccountAuthenticator] to the Android account framework. */
class CrmAuthenticatorService : Service() {

    private lateinit var authenticator: CrmAccountAuthenticator

    override fun onCreate() {
        super.onCreate()
        authenticator = CrmAccountAuthenticator(this)
    }

    override fun onBind(intent: Intent?): IBinder = authenticator.iBinder
}
