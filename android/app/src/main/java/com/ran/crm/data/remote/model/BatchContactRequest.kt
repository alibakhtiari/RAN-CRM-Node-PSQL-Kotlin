package com.ran.crm.data.remote.model

data class BatchContactRequest(
    val contacts: List<BatchContactData>
)

data class BatchContactData(
    val name: String,
    val phone_raw: String,
    val created_at: String? = null
)
