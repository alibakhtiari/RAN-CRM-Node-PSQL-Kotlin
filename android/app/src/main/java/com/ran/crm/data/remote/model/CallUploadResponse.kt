package com.ran.crm.data.remote.model

import com.ran.crm.data.local.entity.CallLog

data class CallUploadResponse(
    val calls: List<CallLog>,
    val count: Int
)
