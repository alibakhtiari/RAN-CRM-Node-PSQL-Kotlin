package com.ran.crm.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AuthenticationService : Service() {
    private lateinit var authenticator: Authenticator

    override fun onCreate() {
        authenticator = Authenticator(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return authenticator.iBinder
    }
}

class Authenticator(context: android.content.Context) : android.accounts.AbstractAccountAuthenticator(context) {
    override fun editProperties(response: android.accounts.AccountAuthenticatorResponse?, accountType: String?) = null
    override fun addAccount(response: android.accounts.AccountAuthenticatorResponse?, accountType: String?, authTokenType: String?, requiredFeatures: Array<out String>?, options: android.os.Bundle?) = null
    override fun confirmCredentials(response: android.accounts.AccountAuthenticatorResponse?, account: android.accounts.Account?, options: android.os.Bundle?) = null
    override fun getAuthToken(response: android.accounts.AccountAuthenticatorResponse?, account: android.accounts.Account?, authTokenType: String?, options: android.os.Bundle?) = null
    override fun getAuthTokenLabel(authTokenType: String?) = null
    override fun updateCredentials(response: android.accounts.AccountAuthenticatorResponse?, account: android.accounts.Account?, authTokenType: String?, options: android.os.Bundle?) = null
    override fun hasFeatures(response: android.accounts.AccountAuthenticatorResponse?, account: android.accounts.Account?, features: Array<out String>?) = null
}
