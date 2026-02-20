package com.ran.crm.data.remote.model

import com.google.gson.annotations.SerializedName

data class BatchContactRequest(@SerializedName("contacts") val contacts: List<BatchContactData>)

data class BatchContactData(
        @SerializedName("name") val name: String,
        @SerializedName("phone_raw") val phone_raw: String,
        @SerializedName("created_at") val created_at: String? = null
)
