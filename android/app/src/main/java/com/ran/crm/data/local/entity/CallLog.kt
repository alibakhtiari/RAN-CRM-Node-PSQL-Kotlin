package com.ran.crm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey
    val id: String, // UUID

    @ColumnInfo(name = "user_id")
    val userId: String, // UUID

    @ColumnInfo(name = "contact_id")
    val contactId: String?, // UUID or null

    val direction: String, // "incoming" | "outgoing" | "missed"

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int,

    val timestamp: String // ISO 8601 timestamp
)
