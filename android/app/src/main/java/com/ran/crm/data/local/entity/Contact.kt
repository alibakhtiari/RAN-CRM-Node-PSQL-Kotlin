package com.ran.crm.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
        tableName = "contacts",
        indices =
                [
                        Index(value = ["phone_normalized"]),
                        Index(value = ["sync_status"]),
                        Index(value = ["created_by"])
                ]
)
data class Contact(
        @PrimaryKey val id: String, // UUID
        val name: String,
        @ColumnInfo(name = "phone_raw") @SerializedName("phone_raw") val phoneRaw: String,
        @ColumnInfo(name = "phone_normalized")
        @SerializedName("phone_normalized")
        val phoneNormalized: String,
        @ColumnInfo(name = "created_by")
        @SerializedName("created_by")
        val createdBy: String, // UUID (User ID)
        @ColumnInfo(name = "created_at")
        @SerializedName("created_at")
        val createdAt: String, // ISO 8601 timestamp
        @ColumnInfo(name = "updated_at")
        @SerializedName("updated_at")
        val updatedAt: String, // ISO 8601 timestamp
        @ColumnInfo(name = "creator_name")
        @SerializedName("creator_name")
        val creatorName: String? = null,
        @ColumnInfo(name = "sync_status") val syncStatus: Int = 0 // 0 = Synced, 1 = Dirty/Pending
)
