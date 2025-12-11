package com.ran.crm.data.local.dao

import androidx.room.*
import com.ran.crm.data.local.entity.CallLog
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLog>>

    @Query("SELECT * FROM call_logs WHERE contact_id = :contactId ORDER BY timestamp DESC")
    fun getCallLogsForContact(contactId: String): Flow<List<CallLog>>

    @Query("SELECT * FROM call_logs WHERE timestamp > :since ORDER BY timestamp ASC")
    suspend fun getCallLogsUpdatedSince(since: String): List<CallLog>

    @Query("SELECT * FROM call_logs WHERE id = :id")
    suspend fun getCallLogById(id: String): CallLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLogs(callLogs: List<CallLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLog)

    @Update
    suspend fun updateCallLog(callLog: CallLog)

    @Delete
    suspend fun deleteCallLog(callLog: CallLog)

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteCallLogById(id: String)

    @Query("SELECT COUNT(*) FROM call_logs")
    suspend fun getCallLogsCount(): Int

    @Query("SELECT COUNT(*) FROM call_logs WHERE contact_id = :contactId")
    suspend fun getCallLogsCountForContact(contactId: String): Int
}
