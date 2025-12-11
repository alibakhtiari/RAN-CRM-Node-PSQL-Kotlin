package com.ran.crm.data.local.dao

import androidx.room.*
import com.ran.crm.data.local.entity.SyncAudit
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncAuditDao {

    @Query("SELECT * FROM sync_audit ORDER BY created_at DESC")
    fun getAllSyncAudits(): Flow<List<SyncAudit>>

    @Query("SELECT * FROM sync_audit ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentSyncAudits(limit: Int = 10): List<SyncAudit>

    @Query("SELECT * FROM sync_audit WHERE id = :id")
    suspend fun getSyncAuditById(id: String): SyncAudit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncAudit(syncAudit: SyncAudit)

    @Update
    suspend fun updateSyncAudit(syncAudit: SyncAudit)

    @Delete
    suspend fun deleteSyncAudit(syncAudit: SyncAudit)

    @Query("DELETE FROM sync_audit WHERE id = :id")
    suspend fun deleteSyncAuditById(id: String)

    @Query("SELECT COUNT(*) FROM sync_audit")
    suspend fun getSyncAuditsCount(): Int
}
