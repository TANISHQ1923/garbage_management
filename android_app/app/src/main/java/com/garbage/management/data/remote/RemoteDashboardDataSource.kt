package com.garbage.management.data.remote

import com.garbage.management.domain.model.CitizenDashboardData
import com.garbage.management.domain.model.DriverDashboardData
import com.garbage.management.domain.model.OfficerDashboardData
import com.garbage.management.utils.Resource
import org.json.JSONObject

/**
 * Remote data source for fetching aggregated dashboard data for Citizen, Officer, and Driver roles.
 */
class RemoteDashboardDataSource(private val apiService: ApiService) {

    suspend fun getCitizenDashboard(): Resource<CitizenDashboardData> {
        return try {
            val response = apiService.getCitizenDashboard()
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.toDomain())
            } else {
                val errorMsg = parseErrorMessage(
                    response.errorBody()?.string(),
                    "Unable to load dashboard data. Please check the server connection."
                )
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error("Unable to load dashboard data. Please check the server connection.")
        }
    }

    suspend fun getOfficerDashboard(): Resource<OfficerDashboardData> {
        return try {
            val response = apiService.getOfficerDashboard()
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.toDomain())
            } else {
                val errorMsg = parseErrorMessage(
                    response.errorBody()?.string(),
                    "Unable to load dashboard data. Please check the server connection."
                )
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error("Unable to load dashboard data. Please check the server connection.")
        }
    }

    suspend fun getDriverDashboard(): Resource<DriverDashboardData> {
        return try {
            val response = apiService.getDriverDashboard()
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.toDomain())
            } else {
                val errorMsg = parseErrorMessage(
                    response.errorBody()?.string(),
                    "Unable to load dashboard data. Please check the server connection."
                )
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error("Unable to load dashboard data. Please check the server connection.")
        }
    }

    private fun parseErrorMessage(errorBody: String?, fallback: String): String {
        if (errorBody.isNullOrBlank()) return fallback
        return try {
            val json = JSONObject(errorBody)
            json.optString("message", fallback)
        } catch (e: Exception) {
            fallback
        }
    }
}
