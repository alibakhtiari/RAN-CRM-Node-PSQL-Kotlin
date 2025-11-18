package com.ran.crm.utils

import android.content.ContentResolver
import android.content.Context
import android.provider.CallLog as SystemCallLog
import com.ran.crm.data.local.entity.CallLog
import com.ran.crm.data.repository.CallLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.time.ExperimentalTime

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

    @OptIn(ExperimentalTime::class)
    private fun getDeviceCallLogs(contentResolver: ContentResolver): List<CallLog> {
        val callLogs = mutableListOf<CallLog>()

        val projection = arrayOf(
            SystemCallLog.Calls._ID,
            SystemCallLog.Calls.NUMBER,
            SystemCallLog.Calls.DATE,
            SystemCallLog.Calls.DURATION,
            SystemCallLog.Calls.TYPE
        )

        // Get calls from last 30 days to avoid importing too much historical data
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val selection = "${SystemCallLog.Calls.DATE} > ?"
        val selectionArgs = arrayOf(thirtyDaysAgo.toString())

        val cursor = contentResolver.query(
            SystemCallLog.Calls.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${SystemCallLog.Calls.DATE} DESC"
        )

        cursor?.use { c ->
            val numberIndex = c.getColumnIndex(SystemCallLog.Calls.NUMBER)
            val dateIndex = c.getColumnIndex(SystemCallLog.Calls.DATE)
            val durationIndex = c.getColumnIndex(SystemCallLog.Calls.DURATION)
            val typeIndex = c.getColumnIndex(SystemCallLog.Calls.TYPE)

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
                        SystemCallLog.Calls.INCOMING_TYPE -> "incoming"
                        SystemCallLog.Calls.OUTGOING_TYPE -> "outgoing"
                        SystemCallLog.Calls.MISSED_TYPE -> "missed"
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
