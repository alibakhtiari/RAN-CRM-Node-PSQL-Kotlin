package com.ran.crm.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "crm_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_LAST_SYNC_CONTACTS = "last_sync_contacts"
        private const val KEY_LAST_SYNC_CALLS = "last_sync_calls"
    }

    var authToken: String?
        get() = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()

    var lastSyncContacts: String?
        get() = prefs.getString(KEY_LAST_SYNC_CONTACTS, null)
        set(value) = prefs.edit().putString(KEY_LAST_SYNC_CONTACTS, value).apply()

    var lastSyncCalls: String?
        get() = prefs.getString(KEY_LAST_SYNC_CALLS, null)
        set(value) = prefs.edit().putString(KEY_LAST_SYNC_CALLS, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
