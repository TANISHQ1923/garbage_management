package com.garbage.management.data.remote

import com.garbage.management.data.model.AuthResponseDto
import com.garbage.management.data.model.LoginRequestDto
import com.garbage.management.data.model.RegisterRequestDto
import com.garbage.management.data.model.UserDto
import com.garbage.management.domain.model.User
import com.garbage.management.utils.Resource
import org.json.JSONObject

/**
 * Remote data source responsible for authentication and user profile API calls.
 */
class RemoteAuthDataSource(private val apiService: ApiService) {

    suspend fun login(email: String, password: String, role: String? = null): Resource<AuthResponseDto> {
        return try {
            val response = apiService.login(LoginRequestDto(email = email, password = password, role = role))
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network error connecting to backend service.")
        }
    }

    suspend fun register(request: RegisterRequestDto): Resource<AuthResponseDto> {
        return try {
            val response = apiService.register(request)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network error connecting to backend service.")
        }
    }

    suspend fun getMe(): Resource<UserDto> {
        return try {
            val response = apiService.getMe()
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network error")
        }
    }

    private fun parseErrorMessage(errorBody: String?, fallback: String): String {
        if (errorBody.isNullOrBlank()) return fallback.ifBlank { "Request failed" }
        return try {
            val json = JSONObject(errorBody)
            json.optString("message", fallback)
        } catch (e: Exception) {
            fallback
        }
    }
}
