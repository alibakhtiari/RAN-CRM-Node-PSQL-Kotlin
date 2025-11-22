package com.ran.crm.sync

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import android.util.Log

class SyncAdapter(
    context: Context,
    autoInitialize: Boolean
) : AbstractThreadedSyncAdapter(context, autoInitialize) {

    override fun onPerformSync(
        account: Account,
        extras: Bundle,
        authority: String,
        provider: ContentProviderClient,
        syncResult: SyncResult
    ) {
        Log.d("SyncAdapter", "onPerformSync called for account: ${account.name}")
        
        try {
            // Initialize Repository (Manual DI since SyncAdapter is created by system)
            val applicationContext = context.applicationContext
            val preferenceManager = com.ran.crm.data.local.PreferenceManager(applicationContext)
            
            // CRITICAL: Restore auth token to ApiClient before making requests
            preferenceManager.authToken?.let { token ->
                com.ran.crm.data.remote.ApiClient.setAuthToken(token)
                com.ran.crm.utils.SyncLogger.log("SyncAdapter: Auth token restored")
            } ?: run {
                com.ran.crm.utils.SyncLogger.log("SyncAdapter: No auth token found - sync will fail")
                syncResult.stats.numAuthExceptions++
                return
            }
            
            val syncManager = com.ran.crm.sync.SyncManager(applicationContext)
            
            kotlinx.coroutines.runBlocking {
                com.ran.crm.utils.SyncLogger.log("SyncAdapter: Starting sync...")
                
                // Check extras for forced full sync
                val forceFullSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false) ||
                                   extras.getBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, false)
                
                val success = if (forceFullSync) {
                    syncManager.performFullSync()
                } else {
                    syncManager.performDeltaSync()
                }
                
                if (success) {
                    com.ran.crm.utils.SyncLogger.log("SyncAdapter: Sync completed successfully.")
                } else {
                    com.ran.crm.utils.SyncLogger.log("SyncAdapter: Sync failed.")
                    syncResult.stats.numIoExceptions++
                }
            }
        } catch (e: Exception) {
            Log.e("SyncAdapter", "Sync failed", e)
            syncResult.stats.numIoExceptions++
        }
    }
}
