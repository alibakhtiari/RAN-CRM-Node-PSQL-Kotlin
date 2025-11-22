package com.ran.crm.data.repository

import com.ran.crm.data.local.entity.User
import com.ran.crm.data.remote.ApiClient
import com.ran.crm.data.remote.model.LoginRequest
import com.ran.crm.data.remote.safeApiCall

import com.ran.crm.data.local.PreferenceManager

class AuthRepository(private val preferenceManager: PreferenceManager) {

    suspend fun login(username: String, password: String): Result<User> {
        val loginRequest = LoginRequest(username, password)

        val result = safeApiCall {
            ApiClient.apiService.login(loginRequest)
        }

        return when (result) {
            is com.ran.crm.data.remote.ApiResult.Success -> {
                val response = result.data
                // Store the token
                ApiClient.setAuthToken(response.token)
                preferenceManager.authToken = response.token
                // Store user info if needed
                Result.success(response.user)
            }
            is com.ran.crm.data.remote.ApiResult.Error -> {
                Result.failure(Exception(result.message))
            }
        }
    }

    suspend fun getCurrentUser(): Result<User> {
        val result = safeApiCall {
            ApiClient.apiService.getCurrentUser()
        }

        return when (result) {
            is com.ran.crm.data.remote.ApiResult.Success -> {
                Result.success(result.data)
            }
            is com.ran.crm.data.remote.ApiResult.Error -> {
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
