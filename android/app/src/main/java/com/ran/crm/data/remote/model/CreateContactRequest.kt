package com.ran.crm.data.remote.model

import com.google.gson.annotations.SerializedName

data class CreateContactRequest(
        @SerializedName("name") val name: String,
        @SerializedName("phone_raw") val phone_raw: String,
        @SerializedName("phone_normalized") val phone_normalized: String,
        @SerializedName("created_at") val created_at: String? = null
)
