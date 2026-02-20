package com.ran.crm.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
        tableName = "call_logs",
        indices =
                [
                        Index(value = ["contact_id"]),
                        Index(value = ["timestamp"]),
                        Index(value = ["phone_number"]),
                        Index(value = ["phone_number", "timestamp"])
                ]
)
data class CallLog(
        @PrimaryKey @SerializedName("id") val id: String, // UUID
        @ColumnInfo(name = "user_id") @SerializedName("user_id") val userId: String, // UUID
        @ColumnInfo(name = "user_name")
        @SerializedName("user_name")
        val userName: String? = null, // User's full name from users table
        @ColumnInfo(name = "contact_id")
        @SerializedName("contact_id")
        val contactId: String?, // UUID or null
        @SerializedName("direction") val direction: String, // "incoming" | "outgoing" | "missed"
        @ColumnInfo(name = "duration_seconds")
        @SerializedName("duration_seconds")
        val durationSeconds: Int,
        @SerializedName("timestamp") val timestamp: String, // ISO 8601 timestamp
        @ColumnInfo(name = "phone_number")
        @SerializedName("phone_number")
        val phoneNumber: String?,
        @ColumnInfo(name = "contact_name")
        @SerializedName("contact_name")
        val contactName: String?
)
