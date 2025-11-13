package com.ran.crm.data.remote

import com.ran.crm.data.local.entity.CallLog
import com.ran.crm.data.local.entity.Contact
import com.ran.crm.data.local.entity.User
import com.ran.crm.data.remote.model.*
import retrofit2.http.*

interface CrmApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("auth/me")
    suspend fun getCurrentUser(): User

    @GET("contacts")
    suspend fun getContacts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): PaginatedResponse<Contact>

    @GET("contacts")
    suspend fun getContacts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("updated_since") updatedSince: String? = null
    ): PaginatedResponse<Contact>

    @POST("contacts")
    suspend fun createContact(@Body contact: CreateContactRequest): ContactResponse

    @POST("contacts/batch")
    suspend fun batchCreateContacts(@Body request: BatchContactRequest): BatchContactResponse

    @GET("calls")
    suspend fun getCalls(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("updated_since") updatedSince: String? = null
    ): PaginatedResponse<CallLog>

    @POST("calls")
    suspend fun uploadCalls(@Body request: CallUploadRequest): CallUploadResponse
}
