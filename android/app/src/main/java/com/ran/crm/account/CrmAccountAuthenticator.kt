package com.ran.crm.account

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.content.Context
import android.os.Bundle

/**
 * Stub authenticator for the Android account system.
 *
 * We don't use Android's built-in account authentication flow — the app handles login/token
 * management itself via [com.ran.crm.data.remote.ApiClient]. This class exists solely so we can
 * register a system account type and attach a SyncAdapter to it.
 */
class CrmAccountAuthenticator(context: Context) : AbstractAccountAuthenticator(context) {

    override fun editProperties(
            response: AccountAuthenticatorResponse?,
            accountType: String?
    ): Bundle? = null

    override fun addAccount(
            response: AccountAuthenticatorResponse?,
            accountType: String?,
            authTokenType: String?,
            requiredFeatures: Array<out String>?,
            options: Bundle?
    ): Bundle? = null

    override fun confirmCredentials(
            response: AccountAuthenticatorResponse?,
            account: Account?,
            options: Bundle?
    ): Bundle? = null

    override fun getAuthToken(
            response: AccountAuthenticatorResponse?,
            account: Account?,
            authTokenType: String?,
            options: Bundle?
    ): Bundle? = null

    override fun getAuthTokenLabel(authTokenType: String?): String? = null

    override fun updateCredentials(
            response: AccountAuthenticatorResponse?,
            account: Account?,
            authTokenType: String?,
            options: Bundle?
    ): Bundle? = null

    override fun hasFeatures(
            response: AccountAuthenticatorResponse?,
            account: Account?,
            features: Array<out String>?
    ): Bundle? = null
}
