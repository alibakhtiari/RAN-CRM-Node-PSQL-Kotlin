package com.ran.crm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey
    val id: String, // UUID

    @ColumnInfo(name = "user_id")
    @SerializedName("user_id")
    val userId: String, // UUID

    @ColumnInfo(name = "user_name")
    @SerializedName("user_name")
    val userName: String? = null, // User's full name from users table

    @ColumnInfo(name = "contact_id")
    @SerializedName("contact_id")
    val contactId: String?, // UUID or null

    val direction: String, // "incoming" | "outgoing" | "missed"

    @ColumnInfo(name = "duration_seconds")
    @SerializedName("duration_seconds")
    val durationSeconds: Int,

    val timestamp: String, // ISO 8601 timestamp

    @ColumnInfo(name = "phone_number")
    @SerializedName("phone_number")
    val phoneNumber: String?,

    @ColumnInfo(name = "contact_name")
    @SerializedName("contact_name")
    val contactName: String?
)
