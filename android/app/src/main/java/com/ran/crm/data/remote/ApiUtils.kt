package com.ran.crm.data.remote

import java.io.IOException
import retrofit2.HttpException

suspend fun <T> safeApiCall(apiCall: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(apiCall())
    } catch (e: HttpException) {
        when (e.code()) {
            401 -> ApiResult.Error(401, "Re-authentication required")
            403 -> ApiResult.Error(403, "Access denied")
            409 -> ApiResult.Error(409, "Data conflict - merge required")
            else -> {
                val errorBody = e.response()?.errorBody()?.string()
                val message =
                        if (!errorBody.isNullOrEmpty()) {
                            "$errorBody (Code: ${e.code()})"
                        } else {
                            e.message()
                        }
                ApiResult.Error(e.code(), message)
            }
        }
    } catch (e: IOException) {
        ApiResult.Error(-1, "Network error")
    } catch (e: Exception) {
        ApiResult.Error(-2, e.message ?: "Unknown error")
    }
}
