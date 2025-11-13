package com.ran.crm.data.repository

import com.ran.crm.data.local.dao.CallLogDao
import com.ran.crm.data.local.entity.CallLog
import com.ran.crm.data.remote.ApiClient
import com.ran.crm.data.remote.model.CallData
import com.ran.crm.data.remote.model.CallUploadRequest
import com.ran.crm.data.remote.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CallLogRepository(
    private val callLogDao: CallLogDao
) {

    fun getAllCallLogs(): Flow<List<CallLog>> = callLogDao.getAllCallLogs()

    fun getCallLogsForContact(contactId: String): Flow<List<CallLog>> =
        callLogDao.getCallLogsForContact(contactId)

    suspend fun getCallLogById(id: String): CallLog? = callLogDao.getCallLogById(id)

    suspend fun insertCallLogs(callLogs: List<CallLog>) = callLogDao.insertCallLogs(callLogs)

    suspend fun insertCallLog(callLog: CallLog) = callLogDao.insertCallLog(callLog)

    suspend fun updateCallLog(callLog: CallLog) = callLogDao.updateCallLog(callLog)

    suspend fun deleteCallLog(callLog: CallLog) = callLogDao.deleteCallLog(callLog)

    suspend fun getCallLogsUpdatedSince(since: String): List<CallLog> =
        callLogDao.getCallLogsUpdatedSince(since)

    suspend fun syncCallLogs() {
        // Get local changes since last sync
        val lastSync = getLastSyncTime()
        val localCallLogs = if (lastSync != null) {
            getCallLogsUpdatedSince(lastSync)
        } else {
            // Initial sync - get recent call logs (last 30 days)
            val thirtyDaysAgo = kotlinx.datetime.Clock.System.now()
                .minus(kotlinx.datetime.DateTimePeriod(days = 30))
                .toString()
            getCallLogsUpdatedSince(thirtyDaysAgo)
        }

        // Upload local changes in batches
        if (localCallLogs.isNotEmpty()) {
            uploadCallLogsBatch(localCallLogs)
        }

        // Download remote changes
        downloadCallLogs(lastSync)
    }

    private suspend fun uploadCallLogsBatch(callLogs: List<CallLog>) {
        // Split into batches of 1000 as per requirements
        val batches = callLogs.chunked(1000)

        for (batch in batches) {
            val callData = batch.map { callLog ->
                CallData(
                    contact_id = callLog.contactId,
                    direction = callLog.direction,
                    duration_seconds = callLog.durationSeconds,
                    timestamp = callLog.timestamp,
                    phone_normalized = null // Will be matched by server if contact_id is null
                )
            }

            val uploadRequest = CallUploadRequest(callData)
            val result = safeApiCall {
                ApiClient.apiService.uploadCalls(uploadRequest)
            }

            when (result) {
                is com.ran.crm.data.remote.ApiResult.Success -> {
                    // Call logs uploaded successfully
                    // Update local call logs with server IDs if needed
                    val response = result.data
                    // The response contains the uploaded call logs with IDs
                }
                is com.ran.crm.data.remote.ApiResult.Error -> {
                    // Handle error - could implement retry logic
                    throw Exception("Failed to upload call logs: ${result.message}")
                }
            }
        }
    }

    private suspend fun downloadCallLogs(since: String?) {
        val result = safeApiCall {
            if (since != null) {
                ApiClient.apiService.getCalls(updatedSince = since)
            } else {
                ApiClient.apiService.getCalls()
            }
        }

        when (result) {
            is com.ran.crm.data.remote.ApiResult.Success -> {
                val callLogs = result.data.data
                if (callLogs.isNotEmpty()) {
                    insertCallLogs(callLogs)
                }
                // Update last sync time
                updateLastSyncTime()
            }
            is com.ran.crm.data.remote.ApiResult.Error -> {
                // Handle error
                throw Exception("Failed to download call logs: ${result.message}")
            }
        }
    }

    suspend fun createCallLog(
        userId: String,
        contactId: String?,
        direction: String,
        durationSeconds: Int,
        timestamp: String
    ): CallLog {
        val callLog = CallLog(
            id = java.util.UUID.randomUUID().toString(),
            userId = userId,
            contactId = contactId,
            direction = direction,
            durationSeconds = durationSeconds,
            timestamp = timestamp
        )

        insertCallLog(callLog)
        return callLog
    }

    private suspend fun getLastSyncTime(): String? {
        // This would typically be stored in SharedPreferences or database
        // For now, return null to force full sync
        return null
    }

    private suspend fun updateLastSyncTime() {
        // Update last sync timestamp
        // This would typically be stored in SharedPreferences
    }
}
