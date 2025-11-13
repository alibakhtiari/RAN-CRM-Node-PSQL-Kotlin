package com.ran.crm.data.remote.model

data class ErrorResponse(
    val error: String
)

// Special case for contact conflicts (409)
data class ConflictErrorResponse(
    val error: String,
    val existing_contact: com.ran.crm.data.local.entity.Contact // Details of the conflicting contact
)
