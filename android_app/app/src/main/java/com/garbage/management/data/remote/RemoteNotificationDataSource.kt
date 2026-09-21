package com.garbage.management.data.remote

import com.garbage.management.domain.model.AppNotification
import com.garbage.management.utils.Resource
import org.json.JSONObject

/**
 * Remote data source for user notifications.
 */
class RemoteNotificationDataSource(private val apiService: ApiService) {

    suspend fun getNotifications(): Resource<List<AppNotification>> {
        return try {
            val response = apiService.getNotifications()
            if (response.isSuccessful && response.body()?.data != null) {
                val list = response.body()!!.data!!.map { it.toDomain() }
                Resource.Success(list)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch notifications.")
        }
    }

    suspend fun markAsRead(notificationId: String): Resource<Unit> {
        return try {
            val response = apiService.markNotificationRead(notificationId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update notification.")
        }
    }

    suspend fun markAllAsRead(): Resource<Unit> {
        return try {
            val response = apiService.markAllNotificationsRead()
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to mark notifications read.")
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
