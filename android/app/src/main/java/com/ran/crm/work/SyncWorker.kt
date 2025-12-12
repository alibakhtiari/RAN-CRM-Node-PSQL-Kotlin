package com.ran.crm.work

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(context: Context, workerParams: WorkerParameters) :
        CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result =
            withContext(Dispatchers.IO) {
                try {
                    // 1. Initialize dependencies
                    val preferenceManager =
                            com.ran.crm.data.local.PreferenceManager(applicationContext)

                    // 2. CRITICAL: Restore Auth Token
                    val token = preferenceManager.authToken
                    if (token != null) {
                        com.ran.crm.data.remote.ApiClient.setAuthToken(token)
                        com.ran.crm.utils.SyncLogger.log("SyncWorker: Auth token restored")
                    } else {
                        com.ran.crm.utils.SyncLogger.log(
                                "SyncWorker: No auth token found - aborting sync"
                        )
                        return@withContext Result.failure()
                    }

                    // Set timeout of 5 minutes

                    com.ran.crm.utils.SyncLogger.log("SyncWorker: Starting sync...")

                    val syncManager = com.ran.crm.sync.SyncManager(applicationContext)

                    // Determine if full sync is needed (e.g. from input data or periodic check)
                    // For now, we'll default to Delta Sync unless specified
                    val forceFullSync = inputData.getBoolean("force_full_sync", false)

                    val success =
                            if (forceFullSync) {
                                syncManager.performFullSync()
                            } else {
                                syncManager.performDeltaSync()
                            }

                    if (success) {
                        com.ran.crm.utils.SyncLogger.log("SyncWorker: Sync completed successfully")
                        Result.success()
                    } else {
                        com.ran.crm.utils.SyncLogger.log("SyncWorker: Sync failed")
                        Result.retry()
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    com.ran.crm.utils.SyncLogger.log("SyncWorker: Sync cancelled")
                    throw e
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    com.ran.crm.utils.SyncLogger.log("SyncWorker: Sync timed out", e)
                    Result.failure()
                } catch (e: Exception) {
                    com.ran.crm.utils.SyncLogger.log("SyncWorker: Sync error", e)
                    Result.retry()
                }
            }

    companion object {
        private const val SYNC_WORK_NAME = "crm_sync_work"

        fun schedulePeriodicSync(context: Context, intervalMinutes: Int) {
            val constraints =
                    Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .setRequiresBatteryNotLow(true)
                            .build()

            val syncWorkRequest =
                    PeriodicWorkRequestBuilder<SyncWorker>(
                                    intervalMinutes.toLong(),
                                    TimeUnit.MINUTES
                            )
                            .setConstraints(constraints)
                            .setBackoffCriteria(
                                    BackoffPolicy.EXPONENTIAL,
                                    WorkRequest.MIN_BACKOFF_MILLIS,
                                    TimeUnit.MILLISECONDS
                            )
                            .build()

            WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(
                            SYNC_WORK_NAME,
                            ExistingPeriodicWorkPolicy.UPDATE,
                            syncWorkRequest
                    )
        }

        fun scheduleOneTimeSync(context: Context, forceFullSync: Boolean = false) {
            val constraints =
                    Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .setRequiresBatteryNotLow(true)
                            .build()

            val data = Data.Builder().putBoolean("force_full_sync", forceFullSync).build()

            val syncWorkRequest =
                    OneTimeWorkRequestBuilder<SyncWorker>()
                            .setConstraints(constraints)
                            .setInputData(data)
                            .setBackoffCriteria(
                                    BackoffPolicy.EXPONENTIAL,
                                    WorkRequest.MIN_BACKOFF_MILLIS,
                                    TimeUnit.MILLISECONDS
                            )
                            .build()

            WorkManager.getInstance(context)
                    .enqueueUniqueWork(
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
                    .getWorkInfosForUniqueWorkLiveData("${SYNC_WORK_NAME}_one_time")
        }
    }
}
