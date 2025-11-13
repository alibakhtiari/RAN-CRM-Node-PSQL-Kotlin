package com.ran.crm.utils

import android.content.ContentResolver
import android.content.Context
import android.provider.CallLog
import com.ran.crm.data.local.entity.CallLog
import com.ran.crm.data.repository.CallLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class CallLogReader(
    private val context: Context,
    private val callLogRepository: CallLogRepository
) {

    data class ImportResult(
        val imported: Int,
        val skipped: Int,
        val errors: Int
    )

    suspend fun importDeviceCallLogs(): ImportResult = withContext(Dispatchers.IO) {
        var imported = 0
        var skipped = 0
        var errors = 0

        try {
            val contentResolver = context.contentResolver
            val callLogs = getDeviceCallLogs(contentResolver)

            for (deviceCallLog in callLogs) {
                try {
                    // Check if call log already exists (by timestamp and phone)
                    val existingCallLog = callLogRepository.getCallLogById(deviceCallLog.id)

                    if (existingCallLog == null) {
                        callLogRepository.insertCallLog(deviceCallLog)
                        imported++
                    } else {
                        skipped++
                    }
                } catch (e: Exception) {
                    errors++
                }
            }
        } catch (e: Exception) {
            errors++
        }

        ImportResult(imported, skipped, errors)
    }

    private fun getDeviceCallLogs(contentResolver: ContentResolver): List<CallLog> {
        val callLogs = mutableListOf<CallLog>()

        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE
        )

        // Get calls from last 30 days to avoid importing too much historical data
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val selection = "${CallLog.Calls.DATE} > ?"
        val selectionArgs = arrayOf(thirtyDaysAgo.toString())

        val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${CallLog.Calls.DATE} DESC"
        )

        cursor?.use { c ->
            val numberIndex = c.getColumnIndex(CallLog.Calls.NUMBER)
            val dateIndex = c.getColumnIndex(CallLog.Calls.DATE)
            val durationIndex = c.getColumnIndex(CallLog.Calls.DURATION)
            val typeIndex = c.getColumnIndex(CallLog.Calls.TYPE)

            while (c.moveToNext()) {
                try {
                    val phoneRaw = c.getString(numberIndex) ?: continue
                    val timestamp = c.getLong(dateIndex)
                    val duration = c.getLong(durationIndex)
                    val type = c.getInt(typeIndex)

                    // Normalize phone number
                    val phoneNormalized = PhoneUtils.normalizePhoneNumber(phoneRaw) ?: continue

                    // Convert call type
                    val direction = when (type) {
                        CallLog.Calls.INCOMING_TYPE -> "incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                        CallLog.Calls.MISSED_TYPE -> "missed"
                        else -> continue
                    }

                    // Create unique ID based on timestamp and phone
                    val id = "${timestamp}_${phoneNormalized}"

                    val callLog = CallLog(
                        id = id,
                        userId = "", // Will be set when syncing
                        contactId = null, // Will be matched later
                        direction = direction,
                        durationSeconds = duration.toInt(),
                        timestamp = kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp).toString()
                    )

                    callLogs.add(callLog)
                } catch (e: Exception) {
                    // Skip invalid call logs
                    continue
                }
            }
        }

        return callLogs
    }

    /**
     * Gets the most recent call log timestamp for delta sync
     */
    suspend fun getLastCallLogTimestamp(): Long? = withContext(Dispatchers.IO) {
        // This would query the local database for the most recent call log timestamp
        // For now, return 30 days ago
        System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
    }
}
