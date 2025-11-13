package com.ran.crm.data.remote.model

data class CallUploadRequest(
    val calls: List<CallData>
)

data class CallData(
    val contact_id: String? = null,
    val direction: String, // "incoming", "outgoing", "missed"
    val duration_seconds: Int,
    val timestamp: String,
    val phone_normalized: String? = null
)
