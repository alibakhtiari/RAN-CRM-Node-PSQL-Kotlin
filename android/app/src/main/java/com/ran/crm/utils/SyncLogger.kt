package com.ran.crm.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A centralized logger for sync operations.
 * Logs to Logcat and appends to a local log file for debugging.
 */
object SyncLogger {
    private const val TAG = "RanCrmSync"
    private const val LOG_FILE_NAME = "sync_logs.txt"
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun log(message: String, error: Throwable? = null) {
        // 1. Log to Logcat
        if (error != null) {
            Log.e(TAG, message, error)
        } else {
            Log.d(TAG, message)
        }

        // 2. Log to File (Async)
        // Note: In a real production app, we might use a proper logging library or database.
        // For now, a simple text file is sufficient for debugging.
        try {
            val context = com.ran.crm.CrmApplication.instance
            GlobalScope.launch(Dispatchers.IO) {
                appendLogToFile(context, message, error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log to file", e)
        }
    }

    private fun appendLogToFile(context: Context, message: String, error: Throwable?) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            val timestamp = dateFormat.format(Date())
            val logEntry = buildString {
                append("[$timestamp] $message")
                if (error != null) {
                    append("\nSTACKTRACE: ${Log.getStackTraceString(error)}")
                }
                append("\n")
            }
            file.appendText(logEntry)
            
            // Rotate logs if too large (e.g., > 5MB)
            if (file.length() > 5 * 1024 * 1024) {
                file.writeText("") // Clear for now, or rename to .old
                file.appendText("[$timestamp] Log rotated\n")
            }
        } catch (e: Exception) {
            // Ignore file errors
        }
    }
    
    fun getLogs(context: Context): String {
        return try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.readText() else "No logs found."
        } catch (e: Exception) {
            "Failed to read logs: ${e.message}"
        }
    }
    
    fun clearLogs(context: Context) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.writeText("")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear logs", e)
        }
    }
}
