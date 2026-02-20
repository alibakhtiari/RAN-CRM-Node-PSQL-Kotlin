package com.ran.crm.data.remote.model

import com.google.gson.annotations.SerializedName

data class PaginatedResponse<T>(
        @SerializedName("data") val data: List<T>,
        @SerializedName("pagination") val pagination: PaginationInfo
)

data class PaginationInfo(
        @SerializedName("currentPage") val currentPage: Int,
        @SerializedName("totalPages") val totalPages: Int,
        @SerializedName("totalItems") val totalItems: Int,
        @SerializedName("itemsPerPage") val itemsPerPage: Int,
        @SerializedName("hasNext") val hasNext: Boolean,
        @SerializedName("hasPrev") val hasPrev: Boolean
)
