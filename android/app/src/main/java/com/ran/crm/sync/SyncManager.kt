package com.ran.crm.sync

import android.content.Context
import com.ran.crm.data.local.CrmDatabase
import com.ran.crm.data.local.PreferenceManager
import com.ran.crm.data.repository.ContactRepository
import com.ran.crm.utils.ContactWriter
import com.ran.crm.utils.SyncLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Centralized manager for all synchronization logic. Enforces "Server is Truth" policy. */
class SyncManager(private val context: Context) {

    private val database = CrmDatabase.getDatabase(context)
    private val preferenceManager = PreferenceManager(context)
    private val contactRepository = ContactRepository(database.contactDao(), preferenceManager)
    private val callLogRepository =
            com.ran.crm.data.repository.CallLogRepository(database.callLogDao(), preferenceManager)
    private val contactWriter = ContactWriter(context, contactRepository)
    private val contactMigrationManager =
            com.ran.crm.data.manager.ContactMigrationManager(context, contactRepository)

    suspend fun performFullSync(): Boolean =
            withContext(Dispatchers.IO) {
                SyncLogger.log("=== STARTING FULL SYNC ===")
                val startTime = System.currentTimeMillis()
                var success = true
                var contactsCount = 0
                var callsCount = 0
                var errorMessage: String? = null

                try {
                    // 1. Sync Contacts
                    val contactsSuccess = syncContacts(isFullSync = true)
                    if (!contactsSuccess) {
                        success = false
                        errorMessage = "Contact sync failed"
                    } else {
                        contactsCount = database.contactDao().getContactsCount()
                    }

                    // 2. Sync Call Logs
                    val callsSuccess = syncCallLogs(isFullSync = true)
                    if (!callsSuccess) {
                        success = false
                        errorMessage =
                                if (errorMessage != null) "$errorMessage; Call sync failed"
                                else "Call sync failed"
                    } else {
                        callsCount = database.callLogDao().getCallLogsCount()
                    }
                } catch (e: Exception) {
                    SyncLogger.log("Full Sync CRITICAL FAILURE", e)
                    success = false
                    errorMessage = e.message ?: "Unknown error"
                } finally {
                    val duration = System.currentTimeMillis() - startTime
                    SyncLogger.log(
                            "=== FULL SYNC COMPLETED in ${duration}ms. Success: $success ==="
                    )

                    // Record sync audit (separate entries for contacts and calls)
                    recordSyncAudit(
                            syncType = "contacts",
                            status = if (success) "success" else "error",
                            contactsCount = contactsCount,
                            callsCount = 0,
                            errorMessage = errorMessage
                    )
                    recordSyncAudit(
                            syncType = "calls",
                            status = if (success) "success" else "error",
                            contactsCount = 0,
                            callsCount = callsCount,
                            errorMessage = errorMessage
                    )
                }

                return@withContext success
            }

    suspend fun performDeltaSync(): Boolean =
            withContext(Dispatchers.IO) {
                SyncLogger.log("=== STARTING DELTA SYNC ===")
                val startTime = System.currentTimeMillis()
                var success = true
                var contactsCount = 0
                var callsCount = 0
                var errorMessage: String? = null

                try {
                    // 1. Sync Contacts
                    val contactsSuccess = syncContacts(isFullSync = false)
                    if (!contactsSuccess) {
                        success = false
                        errorMessage = "Contact sync failed"
                    } else {
                        contactsCount = database.contactDao().getContactsCount()
                    }

                    // 2. Sync Call Logs
                    val callsSuccess = syncCallLogs(isFullSync = false)
                    if (!callsSuccess) {
                        success = false
                        errorMessage =
                                if (errorMessage != null) "$errorMessage; Call sync failed"
                                else "Call sync failed"
                    } else {
                        callsCount = database.callLogDao().getCallLogsCount()
                    }
                } catch (e: Exception) {
                    SyncLogger.log("Delta Sync CRITICAL FAILURE", e)
                    success = false
                    errorMessage = e.message ?: "Unknown error"
                } finally {
                    val duration = System.currentTimeMillis() - startTime
                    SyncLogger.log(
                            "=== DELTA SYNC COMPLETED in ${duration}ms. Success: $success ==="
                    )

                    // Record sync audit (contacts and calls separately for delta)
                    recordSyncAudit(
                            syncType = "contacts",
                            status = if (success) "success" else "error",
                            contactsCount = contactsCount,
                            callsCount = 0,
                            errorMessage = errorMessage
                    )
                    recordSyncAudit(
                            syncType = "calls",
                            status = if (success) "success" else "error",
                            contactsCount = 0,
                            callsCount = callsCount,
                            errorMessage = errorMessage
                    )
                }

                return@withContext success
            }

    private suspend fun syncContacts(isFullSync: Boolean): Boolean {
        SyncLogger.log("Starting Contact Sync (Full: $isFullSync)")

        try {
            // STEP 0: Automatic System Import (Device -> App)
            try {
                // Import contacts from device, don't delete from device (false)
                val importedCount =
                        contactMigrationManager.importSystemContacts(deleteAfterImport = false)
                if (importedCount > 0) {
                    SyncLogger.log("Auto-Import: Imported $importedCount new contacts from device")
                }
            } catch (e: Exception) {
                SyncLogger.log("Auto-Import Failed (Non-critical)", e)
            }

            // STEP 1: Upload Dirty Contacts (Offline-First)
            contactRepository.uploadDirtyContacts()

            // STEP 2: Download from Server
            if (isFullSync) {
                contactRepository.performFullSyncDownload()
            } else {
                contactRepository.performDeltaSyncDownload()
            }
            SyncLogger.log("Contact Download & DB Update Complete")

            // STEP 2: Export to Android Device
            val result = contactWriter.syncToDevice()
            SyncLogger.log(
                    "Device Export Complete: Created=${result.exported}, Updated=${result.updated}, Errors=${result.errors}"
            )

            return result.errors == 0
        } catch (e: Exception) {
            SyncLogger.log("Contact Sync Failed", e)
            return false
        }
    }

    private suspend fun syncCallLogs(isFullSync: Boolean): Boolean {
        SyncLogger.log("Starting Call Log Sync (Full: $isFullSync)")

        try {
            // Use the repo to sync
            // Note: CallLogRepository doesn't have a separate "Writer" like contacts,
            // as we don't write BACK to the device call log (read-only usually).
            // We just import from device -> upload to server -> download from server.

            // 1. Import from Device (Always do this to capture new calls)
            try {
                // Explicitly passing all 3 arguments to fix reported "No value passed for
                // parameter" error
                val callLogReader =
                        com.ran.crm.utils.CallLogReader(
                                context,
                                callLogRepository,
                                contactRepository
                        )
                val importResult = callLogReader.importDeviceCallLogs()
                SyncLogger.log(
                        "Call Log Import: Imported=${importResult.imported}, Skipped=${importResult.skipped}, Errors=${importResult.errors}"
                )
            } catch (e: Exception) {
                SyncLogger.log("Call Log Import Failed", e)
                // Continue to download even if import fails
            }

            // 2. Upload local logs to Server
            try {
                callLogRepository.uploadCallLogsToServer()
                SyncLogger.log("Call Log Upload Complete")
            } catch (e: Exception) {
                SyncLogger.log("Call Log Upload Failed", e)
                // Continue to download even if upload fails
            }

            // 3. Download from Server (and merge)
            if (isFullSync) {
                callLogRepository.performFullSyncDownload()
            } else {
                callLogRepository.performDeltaSyncDownload()
            }

            SyncLogger.log("Call Log Sync Complete")
            return true
        } catch (e: Exception) {
            SyncLogger.log("Call Log Sync Failed", e)
            return false
        }
    }

    private suspend fun recordSyncAudit(
            syncType: String,
            status: String,
            contactsCount: Int,
            callsCount: Int,
            errorMessage: String?
    ) {
        try {
            val request =
                    com.ran.crm.data.remote.model.SyncAuditRequest(
                            syncType = syncType,
                            status = status,
                            errorMessage = errorMessage,
                            syncedContacts = contactsCount,
                            syncedCalls = callsCount
                    )
            com.ran.crm.data.remote.ApiClient.apiService.recordSyncAudit(request)
            SyncLogger.log("Sync audit recorded: type=$syncType, status=$status")
        } catch (e: Exception) {
            SyncLogger.log("Failed to record sync audit", e)
            // Don't fail the sync if audit recording fails
        }
    }
}
