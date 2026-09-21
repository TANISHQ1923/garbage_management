package com.garbage.management.data.remote

import com.garbage.management.data.model.ComplaintDto
import com.garbage.management.data.model.CreateComplaintRequestDto
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.utils.Resource
import org.json.JSONObject

/**
 * Remote data source for citizen complaint submissions and tracking.
 */
class RemoteComplaintDataSource(private val apiService: ApiService) {

    suspend fun submitComplaint(request: CreateComplaintRequestDto): Resource<GarbageComplaint> {
        return try {
            val response = apiService.submitComplaint(request)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.toDomain())
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to submit complaint to backend.")
        }
    }

    suspend fun getMyComplaints(): Resource<List<GarbageComplaint>> {
        return try {
            val response = apiService.getMyComplaints()
            if (response.isSuccessful && response.body()?.data != null) {
                val domainList = response.body()!!.data!!.map { it.toDomain() }
                Resource.Success(domainList)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch complaints.")
        }
    }

    suspend fun getComplaintById(id: String): Resource<GarbageComplaint> {
        return try {
            val response = apiService.getComplaintById(id)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.toDomain())
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to load complaint details.")
        }
    }

    suspend fun requestEmergency(id: String): Resource<GarbageComplaint> {
        return try {
            val response = apiService.requestEmergency(id)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.toDomain())
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to request emergency escalation.")
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
