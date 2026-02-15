package com.ran.crm.data.repository

import com.ran.crm.data.local.PreferenceManager
import com.ran.crm.data.local.dao.CallLogDao
import com.ran.crm.data.local.entity.CallLog
import com.ran.crm.data.remote.ApiClient
import com.ran.crm.data.remote.model.CallData
import com.ran.crm.data.remote.model.CallUploadRequest
import com.ran.crm.data.remote.safeApiCall
import com.ran.crm.utils.DateUtils
import com.ran.crm.utils.SyncLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CallLogRepository(
        private val callLogDao: CallLogDao,
        private val preferenceManager: PreferenceManager
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

    // syncCallLogs removed. Logic moved to SyncManager.

    // Legacy methods removed. Logic moved to SyncManager and new sync methods.

    suspend fun createCallLog(
            userId: String,
            contactId: String?,
            direction: String,
            durationSeconds: Int,
            timestamp: String,
            phoneNumber: String?,
            contactName: String?
    ): CallLog {
        val callLog =
                CallLog(
                        id = java.util.UUID.randomUUID().toString(),
                        userId = userId,
                        contactId = contactId,
                        direction = direction,
                        durationSeconds = durationSeconds,
                        timestamp = timestamp,
                        phoneNumber = phoneNumber,
                        contactName = contactName
                )

        insertCallLog(callLog)
        return callLog
    }

    /**
     * Uploads pending call logs to the server. Only uploads logs with temporary IDs (not yet on
     * server).
     */
    suspend fun uploadCallLogsToServer() {
        SyncLogger.log("CallLogRepo: Starting Upload")

        // Get all local call logs
        val localLogs = callLogDao.getAllCallLogs().first()

        // Filter logs that need uploading (temporary IDs contain underscore)
        val logsToUpload = localLogs.filter { it.id.contains("_") }

        if (logsToUpload.isEmpty()) {
            SyncLogger.log("CallLogRepo: No logs to upload")
            return
        }

        SyncLogger.log("CallLogRepo: Uploading ${logsToUpload.size} logs")

        // Convert to CallData format
        val uploadData =
                logsToUpload.map { log ->
                    CallData(
                            contact_id = log.contactId,
                            direction = log.direction,
                            duration_seconds = log.durationSeconds,
                            timestamp = log.timestamp,
                            phone_normalized = log.phoneNumber
                    )
                }

        // Upload in batches
        val batchSize = 100
        var uploadedCount = 0

        uploadData.chunked(batchSize).forEachIndexed { index, batch ->
            val result = safeApiCall {
                ApiClient.apiService.uploadCalls(CallUploadRequest(calls = batch))
            }

            when (result) {
                is com.ran.crm.data.remote.ApiResult.Success -> {
                    SyncLogger.log("CallLogRepo: Uploaded batch of ${batch.size}")

                    // Delete uploaded logs (they'll be downloaded with server UUIDs)
                    val logsInThisBatch = logsToUpload.drop(index * batchSize).take(batch.size)
                    logsInThisBatch.forEach { callLogDao.deleteCallLog(it) }
                    uploadedCount += batch.size
                }
                is com.ran.crm.data.remote.ApiResult.Error -> {
                    SyncLogger.log(
                            "CallLogRepo: Upload failed: ${result.message}"
                    )
                }
            }
        }

        SyncLogger.log(
                "CallLogRepo: Upload complete. Uploaded and deleted $uploadedCount temporary logs"
        )
    }

    /**
     * Performs a full sync download for Call Logs. Fetches all logs from server and merges with
     * local logs to avoid duplicates. Deletes local logs that don't exist on server (Server is
     * Truth).
     */
    suspend fun performFullSyncDownload() {
        var page = 1
        val limit = 50
        var hasMore = true
        val serverLogIds = mutableSetOf<String>()

        SyncLogger.log("CallLogRepo: Starting Full Download")

        while (hasMore) {
            val result = safeApiCall { ApiClient.apiService.getCalls(page = page, limit = limit) }

            when (result) {
                is com.ran.crm.data.remote.ApiResult.Success -> {
                    val response = result.data
                    val serverLogs = response.data

                    SyncLogger.log(
                            "CallLogRepo: Fetched page $page, count: ${serverLogs.size}"
                    )

                    if (serverLogs.isNotEmpty()) {
                        // Track server IDs for deletion logic
                        serverLogs.forEach { serverLogIds.add(it.id) }
                        mergeCallLogs(serverLogs)
                    }

                    if (serverLogs.size < limit) {
                        hasMore = false
                    } else {
                        page++
                    }
                }
                is com.ran.crm.data.remote.ApiResult.Error -> {
                    throw Exception("Failed to download call logs page $page: ${result.message}")
                }
            }
        }

        // Delete local logs that are not on the server
        val localLogs = callLogDao.getAllCallLogs().first()
        val logsToDelete = localLogs.filter { !serverLogIds.contains(it.id) }

        if (logsToDelete.isNotEmpty()) {
            SyncLogger.log(
                    "CallLogRepo: Deleting ${logsToDelete.size} local logs not on server"
            )
            logsToDelete.forEach { callLogDao.deleteCallLog(it) }
        }

        updateLastSyncTime()
    }

    /**
     * Merges server logs with local logs. If a local log exists with the same timestamp (approx)
     * and phone number, it's considered a match. We replace the local log (which has a temp ID)
     * with the server log (UUID).
     */
    private suspend fun mergeCallLogs(serverLogs: List<CallLog>) {
        val localLogs = callLogDao.getAllCallLogs().first()

        // Build a lookup map keyed by (phoneNumber, timestamp) for O(1) matching
        val localByKey = HashMap<String, CallLog>(localLogs.size)
        for (local in localLogs) {
            val key = "${local.phoneNumber}|${local.timestamp}"
            localByKey[key] = local
        }

        for (serverLog in serverLogs) {
            val key = "${serverLog.phoneNumber}|${serverLog.timestamp}"
            val match = localByKey[key]

            if (match != null) {
                if (match.id != serverLog.id) {
                    SyncLogger.log(
                            "CallLogRepo: Merging duplicate log. Local: ${match.id} -> Server: ${serverLog.id}"
                    )
                    callLogDao.deleteCallLog(match)
                    callLogDao.insertCallLog(serverLog)
                } else {
                    callLogDao.updateCallLog(serverLog)
                }
            } else {
                callLogDao.insertCallLog(serverLog)
            }
        }
    }

    /** Performs a delta sync download for Call Logs. */
    suspend fun performDeltaSyncDownload() {
        val lastSyncTime = preferenceManager.lastSyncCalls
        val since =
                if (lastSyncTime > 0) {
                    DateUtils.formatIso(lastSyncTime)
                } else {
                    performFullSyncDownload()
                    return
                }

        SyncLogger.log("CallLogRepo: Starting Delta Download (since $since)")

        var page = 1
        val limit = 50
        var hasMore = true

        while (hasMore) {
            val result = safeApiCall {
                ApiClient.apiService.getCalls(page = page, limit = limit, updatedSince = since)
            }

            when (result) {
                is com.ran.crm.data.remote.ApiResult.Success -> {
                    val response = result.data
                    val serverLogs = response.data

                    if (serverLogs.isNotEmpty()) {
                        mergeCallLogs(serverLogs)
                        SyncLogger.log(
                                "CallLogRepo: Delta fetched ${serverLogs.size} logs"
                        )
                    }

                    if (serverLogs.size < limit) {
                        hasMore = false
                    } else {
                        page++
                    }
                }
                is com.ran.crm.data.remote.ApiResult.Error -> {
                    val msg = "Failed to download delta call logs: ${result.message}"
                    SyncLogger.log(msg)
                    // Ensure we don't crash the sync process
                    return
                }
            }
        }

        updateLastSyncTime()
    }

    private suspend fun updateLastSyncTime() {
        preferenceManager.lastSyncCalls = System.currentTimeMillis()
    }
}
