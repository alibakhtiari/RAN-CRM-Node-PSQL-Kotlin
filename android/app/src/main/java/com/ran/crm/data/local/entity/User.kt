package com.ran.crm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String, // UUID

    val username: String,

    val name: String,

    @ColumnInfo(name = "is_admin")
    val isAdmin: Boolean,

    @ColumnInfo(name = "created_at")
    val createdAt: String, // ISO 8601 timestamp

    @ColumnInfo(name = "updated_at")
    val updatedAt: String // ISO 8601 timestamp
)
