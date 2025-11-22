package com.ran.crm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String, // UUID

    val username: String,

    val name: String,

    @ColumnInfo(name = "is_admin")
    @SerializedName("is_admin")
    val isAdmin: Boolean,

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String, // ISO 8601 timestamp

    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    val updatedAt: String // ISO 8601 timestamp
)
