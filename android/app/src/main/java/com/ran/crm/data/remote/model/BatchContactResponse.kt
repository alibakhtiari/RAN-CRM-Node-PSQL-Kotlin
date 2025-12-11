package com.ran.crm.data.remote.model

import com.ran.crm.data.local.entity.Contact

data class BatchContactResponse(
    val results: List<BatchContactResult>,
    val errors: List<BatchContactError>,
    val summary: BatchContactSummary
)

data class BatchContactResult(
    val index: Int,
    val action: String, // "created", "updated", "existing"
    val contact: Contact
)

data class BatchContactError(
    val index: Int,
    val error: String,
    val existing_contact: Contact? = null,
    val contact: BatchContactData
)

data class BatchContactSummary(
    val total: Int,
    val created: Int,
    val updated: Int,
    val existing: Int,
    val errors: Int
)
