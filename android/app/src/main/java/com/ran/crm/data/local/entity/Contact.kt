package com.ran.crm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey
    val id: String, // UUID

    val name: String,

    @ColumnInfo(name = "phone_raw")
    @SerializedName("phone_raw")
    val phoneRaw: String,

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
    val updatedAt: String // ISO 8601 timestamp
)
