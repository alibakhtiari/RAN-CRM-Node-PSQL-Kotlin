package com.ran.crm.data.remote.model

data class CreateContactRequest(
    val name: String,
    val phone_raw: String,
    val phone_normalized: String,
    val created_at: String
)
