package com.ran.crm.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.util.Log

object AccountHelper {
    const val ACCOUNT_TYPE = "com.ran.crm"
    const val ACCOUNT_NAME = "RAN CRM"
    const val AUTHORITY = "com.android.contacts"

    fun ensureAccountExists(context: Context) {
        val account = Account(ACCOUNT_NAME, ACCOUNT_TYPE)
        val accountManager = AccountManager.get(context)

        if (accountManager.addAccountExplicitly(account, null, null)) {
            Log.d("AccountHelper", "Account created: $ACCOUNT_NAME")
            
            // Enable sync for this account
            ContentResolver.setIsSyncable(account, AUTHORITY, 1)
            ContentResolver.setSyncAutomatically(account, AUTHORITY, true)
            
            // Trigger an initial sync
            val bundle = Bundle().apply {
                putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
            }
            ContentResolver.requestSync(account, AUTHORITY, bundle)
        } else {
            Log.d("AccountHelper", "Account already exists")
        }
    }
    
    fun getAccount(): Account {
        return Account(ACCOUNT_NAME, ACCOUNT_TYPE)
    }
}
