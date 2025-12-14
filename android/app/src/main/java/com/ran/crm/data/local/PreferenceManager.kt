package com.ran.crm.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "crm_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_LAST_SYNC_CONTACTS = "last_sync_contacts"
        private const val KEY_LAST_SYNC_CALLS = "last_sync_calls"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_SYNC_INTERVAL_MINUTES = "sync_interval_minutes"
        private const val KEY_IS_ADMIN = "is_admin"
    }

    var isAdmin: Boolean
        get() = prefs.getBoolean(KEY_IS_ADMIN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_ADMIN, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var authToken: String?
        get() = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()

    var lastSyncContacts: Long
        get() = prefs.getLong(KEY_LAST_SYNC_CONTACTS, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_CONTACTS, value).apply()

    var lastSyncCalls: Long
        get() = prefs.getLong(KEY_LAST_SYNC_CALLS, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_CALLS, value).apply()

    var appTheme: String
        get() = prefs.getString(KEY_APP_THEME, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_APP_THEME, value).apply()

    var fontScale: Float
        get() = prefs.getFloat(KEY_FONT_SCALE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_FONT_SCALE, value).apply()

    var syncIntervalMinutes: Int
        get() = prefs.getInt(KEY_SYNC_INTERVAL_MINUTES, 15)
        set(value) = prefs.edit().putInt(KEY_SYNC_INTERVAL_MINUTES, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun clearSession() {
        authToken = null
        // We might want to keep other prefs like last sync time, or clear everything.
        // For now, let's just clear auth token as that's what "session" implies.
        // If we want to full logout, we can call clear()
        // But based on MainActivity usage, let's make it clear auth token.
        authToken = null
    }
}
