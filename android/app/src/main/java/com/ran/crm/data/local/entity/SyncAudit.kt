package com.ran.crm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "sync_audit")
data class SyncAudit(
    @PrimaryKey
    val id: String, // UUID

    @ColumnInfo(name = "user_id")
    val userId: String, // UUID

    @ColumnInfo(name = "synced_contacts")
    val syncedContacts: Int,

    @ColumnInfo(name = "synced_calls")
    val syncedCalls: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: String // ISO 8601 timestamp
)
