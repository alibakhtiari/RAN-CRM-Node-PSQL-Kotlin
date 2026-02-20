package com.ran.crm.data.remote.model

import com.google.gson.annotations.SerializedName
import com.ran.crm.data.local.entity.Contact

data class BatchContactResponse(
        @SerializedName("results") val results: List<BatchContactResult>,
        @SerializedName("errors") val errors: List<BatchContactError>,
        @SerializedName("summary") val summary: BatchContactSummary
)

data class BatchContactResult(
        @SerializedName("index") val index: Int = 0,
        @SerializedName("action") val action: String, // "created", "updated", "existing"
        @SerializedName("contact") val contact: Contact
)

data class BatchContactError(
        @SerializedName("index") val index: Int,
        @SerializedName("error") val error: String,
        @SerializedName("existing_contact") val existing_contact: Contact? = null,
        @SerializedName("contact") val contact: BatchContactData
)

data class BatchContactSummary(
        @SerializedName("total") val total: Int,
        @SerializedName("created") val created: Int,
        @SerializedName("updated") val updated: Int,
        @SerializedName("existing") val existing: Int,
        @SerializedName("errors") val errors: Int
)
