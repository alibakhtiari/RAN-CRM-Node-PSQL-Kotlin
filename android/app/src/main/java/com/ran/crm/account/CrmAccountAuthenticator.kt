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
class CrmAccountAuthenticator(private val mContext: Context) :
        AbstractAccountAuthenticator(mContext) {

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
        ): Bundle {
                val intent = android.content.Intent(mContext, com.ran.crm.MainActivity::class.java)
                intent.putExtra(
                        android.accounts.AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                        response
                )
                val bundle = Bundle()
                bundle.putParcelable(android.accounts.AccountManager.KEY_INTENT, intent)
                return bundle
        }

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
