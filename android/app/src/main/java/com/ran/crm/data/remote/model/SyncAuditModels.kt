package com.ran.crm.data.remote.model

import com.google.gson.annotations.SerializedName

data class SyncAuditRequest(
    @SerializedName("sync_type")
    val syncType: String, // "contacts", "calls", or "full"
    
    @SerializedName("status")
    val status: String, // "success" or "error"
    
    @SerializedName("error_message")
    val errorMessage: String? = null,
    
    @SerializedName("synced_contacts")
    val syncedContacts: Int = 0,
    
    @SerializedName("synced_calls")
    val syncedCalls: Int = 0
)

data class SyncAuditResponse(
    @SerializedName("sync_audit")
    val syncAudit: SyncAudit
)

data class SyncAudit(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("user_id")
    val userId: String,
    
    @SerializedName("sync_type")
    val syncType: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("created_at")
    val createdAt: String
)
