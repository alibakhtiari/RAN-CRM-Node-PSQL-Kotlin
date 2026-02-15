package com.ran.crm.data.repository

import com.ran.crm.data.local.PreferenceManager
import com.ran.crm.data.local.entity.User
import com.ran.crm.data.remote.ApiClient
import com.ran.crm.data.remote.ApiResult
import com.ran.crm.data.remote.model.LoginRequest
import com.ran.crm.data.remote.safeApiCall

class AuthRepository(private val preferenceManager: PreferenceManager) {

    suspend fun login(username: String, password: String): Result<User> {
        val loginRequest = LoginRequest(username, password)

        val result = safeApiCall { ApiClient.apiService.login(loginRequest) }

        return when (result) {
            is ApiResult.Success -> {
                val response = result.data
                // Store the token
                ApiClient.setAuthToken(response.token)
                preferenceManager.authToken = response.token
                preferenceManager.isAdmin = response.user.isAdmin
                // Store user info if needed
                Result.success(response.user)
            }
            is ApiResult.Error -> {
                Result.failure(Exception(result.message))
            }
        }
    }

    suspend fun getCurrentUser(): Result<User> {
        val result = safeApiCall { ApiClient.apiService.getCurrentUser() }

        return when (result) {
            is ApiResult.Success -> {
                Result.success(result.data)
            }
            is ApiResult.Error -> {
                Result.failure(Exception(result.message))
            }
        }
    }

    fun logout() {
        ApiClient.setAuthToken(null)
        preferenceManager.authToken = null
        // Clear any stored user data
    }

    fun isLoggedIn(): Boolean {
        return ApiClient.getAuthToken() != null
    }

    fun getToken(): String? {
        return ApiClient.getAuthToken()
    }
}
