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
            Resource.Error(resolveErrorMessage(e, "Failed to submit complaint to backend."))
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
            Resource.Error(resolveErrorMessage(e, "Failed to fetch complaints."))
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
            Resource.Error(resolveErrorMessage(e, "Failed to load complaint details."))
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
            Resource.Error(resolveErrorMessage(e, "Failed to request emergency escalation."))
        }
    }

    suspend fun submitFeedback(id: String, rating: Int, comment: String?): Resource<GarbageComplaint> {
        return try {
            val request = com.garbage.management.data.model.SubmitFeedbackRequestDto(
                rating = rating,
                comment = comment
            )
            val response = apiService.submitFeedback(id, request)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.toDomain())
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(resolveErrorMessage(e, "Failed to submit feedback to backend."))
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

    private fun resolveErrorMessage(e: Exception, fallback: String): String {
        val msg = e.localizedMessage ?: return fallback
        return when {
            msg.contains("Failed to connect", ignoreCase = true) || msg.contains("Connection refused", ignoreCase = true) ->
                "Unable to connect to the backend server. Please verify the server is running on port 5000."
            msg.contains("timeout", ignoreCase = true) ->
                "Connection to server timed out. Please check your network connection."
            else -> msg.ifBlank { fallback }
        }
    }
}
