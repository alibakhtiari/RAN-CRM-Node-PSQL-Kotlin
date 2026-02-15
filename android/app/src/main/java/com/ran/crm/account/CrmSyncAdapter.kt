package com.ran.crm.account

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import com.ran.crm.sync.SyncManager
import com.ran.crm.utils.SyncLogger
import kotlinx.coroutines.runBlocking

/**
 * SyncAdapter that the Android OS calls when a system-level sync is requested (e.g. from Settings >
 * Accounts > RAN CRM > Sync now, or periodic auto-sync).
 *
 * Delegates all work to [SyncManager].
 */
class CrmSyncAdapter(context: Context, autoInitialize: Boolean) :
        AbstractThreadedSyncAdapter(context, autoInitialize) {

    override fun onPerformSync(
            account: Account?,
            extras: Bundle?,
            authority: String?,
            provider: ContentProviderClient?,
            syncResult: SyncResult?
    ) {
        SyncLogger.log("CrmSyncAdapter: System sync triggered")
        try {
            val syncManager = SyncManager.getInstance(context)
            runBlocking {
                val success = syncManager.performFullSync()
                if (!success) {
                    syncResult?.stats?.numIoExceptions = 1
                }
            }
            SyncLogger.log("CrmSyncAdapter: System sync completed")
        } catch (e: Exception) {
            SyncLogger.log("CrmSyncAdapter: System sync failed", e)
            syncResult?.stats?.numIoExceptions = 1
        }
    }
}
