package com.ran.crm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String, // UUID

    val username: String,

    val name: String,

    val isAdmin: Boolean,

    val createdAt: String, // ISO 8601 timestamp

    val updatedAt: String // ISO 8601 timestamp
)
