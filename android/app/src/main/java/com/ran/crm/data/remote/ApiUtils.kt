package com.ran.crm.data.remote

import retrofit2.HttpException
import java.io.IOException

suspend fun <T> safeApiCall(apiCall: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(apiCall())
    } catch (e: HttpException) {
        when (e.code()) {
            401 -> ApiResult.Error(401, "Re-authentication required")
            403 -> ApiResult.Error(403, "Access denied")
            409 -> ApiResult.Error(409, "Data conflict - merge required")
            else -> ApiResult.Error(e.code(), e.message())
        }
    } catch (e: IOException) {
        ApiResult.Error(-1, "Network error")
    } catch (e: Exception) {
        ApiResult.Error(-2, e.message ?: "Unknown error")
    }
}
