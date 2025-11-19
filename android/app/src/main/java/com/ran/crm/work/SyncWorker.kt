package com.ran.crm.work

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.*
import com.ran.crm.data.local.CrmDatabase
import com.ran.crm.data.repository.CallLogRepository
import com.ran.crm.data.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Get repositories
            val database = CrmDatabase.getDatabase(applicationContext)
            val preferenceManager = com.ran.crm.data.local.PreferenceManager(applicationContext)
            val contactRepository = ContactRepository(database.contactDao(), preferenceManager)
            val callLogRepository = CallLogRepository(database.callLogDao(), preferenceManager)
            val syncAuditDao = database.syncAuditDao()

            val startTime = System.currentTimeMillis()
            var status = "SUCCESS"
            var message = "Sync completed successfully"

            try {
                // Perform sync operations
                contactRepository.syncContacts()
                callLogRepository.syncCallLogs()
            } catch (e: Exception) {
                status = "FAILURE"
                message = e.message ?: "Unknown error"
                throw e
            } finally {
                // Record sync audit
                val endTime = System.currentTimeMillis()
                val audit = com.ran.crm.data.local.entity.SyncAudit(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = "", // TODO: Get actual user ID
                    syncedContacts = 0, // TODO: Track actual count
                    syncedCalls = 0, // TODO: Track actual count
                    createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())
                )
                syncAuditDao.insertSyncAudit(audit)
            }

            Result.success()
        } catch (e: Exception) {
            // Log error and retry
            Result.retry()
        }
    }

    companion object {
        private const val SYNC_WORK_NAME = "crm_sync_work"

        fun schedulePeriodicSync(context: Context, intervalMinutes: Int) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                intervalMinutes.toLong(), TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                syncWorkRequest
            )
        }

        fun scheduleOneTimeSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${SYNC_WORK_NAME}_one_time",
                ExistingWorkPolicy.REPLACE,
                syncWorkRequest
            )
        }

        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
        }

        fun getSyncWorkInfo(context: Context): LiveData<List<WorkInfo>> {
            return WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(SYNC_WORK_NAME)
        }
    }
}
