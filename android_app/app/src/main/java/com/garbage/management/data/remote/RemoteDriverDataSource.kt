package com.garbage.management.data.remote

import com.garbage.management.data.model.CompleteCollectionRequestDto
import com.garbage.management.data.model.StartCollectionRequestDto
import com.garbage.management.data.model.UpdateEvidenceRequestDto
import com.garbage.management.data.model.UpdateLocationRequestDto
import com.garbage.management.domain.model.DriverLocation
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.utils.Resource
import org.json.JSONObject

/**
 * Remote data source for collection driver operations.
 */
class RemoteDriverDataSource(private val apiService: ApiService) {

    suspend fun getDriverTasks(): Resource<List<GarbageComplaint>> {
        return try {
            val response = apiService.getDriverTasks()
            if (response.isSuccessful && response.body()?.data != null) {
                val list = response.body()!!.data!!.map { it.toDomain() }
                Resource.Success(list)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch driver tasks.")
        }
    }

    suspend fun getDriverTaskById(id: String): Resource<GarbageComplaint> {
        return try {
            val response = apiService.getDriverTaskById(id)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.toDomain())
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to load task details.")
        }
    }

    suspend fun startCollection(
        complaintId: String,
        beforeCleaningImageUrl: String? = null
    ): Resource<GarbageComplaint> {
        return try {
            val request = StartCollectionRequestDto(beforeCleaningImageUrl = beforeCleaningImageUrl)
            val response = apiService.startCollection(complaintId, request)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.toDomain())
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to start collection.")
        }
    }

    suspend fun completeCollection(
        complaintId: String,
        afterCleaningImageUrl: String,
        beforeCleaningImageUrl: String? = null
    ): Resource<GarbageComplaint> {
        return try {
            val request = CompleteCollectionRequestDto(
                afterCleaningImageUrl = afterCleaningImageUrl,
                beforeCleaningImageUrl = beforeCleaningImageUrl
            )
            val response = apiService.completeCollection(complaintId, request)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.toDomain())
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to complete collection.")
        }
    }

    suspend fun updateEvidence(
        complaintId: String,
        beforeCleaningImageUrl: String? = null,
        afterCleaningImageUrl: String? = null
    ): Resource<GarbageComplaint> {
        return try {
            val request = UpdateEvidenceRequestDto(
                beforeCleaningImageUrl = beforeCleaningImageUrl,
                afterCleaningImageUrl = afterCleaningImageUrl
            )
            val response = apiService.updateEvidence(complaintId, request)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.toDomain())
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update evidence.")
        }
    }

    suspend fun updateDriverLocation(latitude: Double, longitude: Double): Resource<DriverLocation> {
        return try {
            val request = UpdateLocationRequestDto(latitude = latitude, longitude = longitude)
            val response = apiService.updateDriverLocation(request)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.toDomain())
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update location.")
        }
    }

    suspend fun getDriverLocation(driverId: String? = null): Resource<DriverLocation> {
        return try {
            val response = apiService.getDriverLocation(driverId)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.toDomain(driverId ?: ""))
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to query driver location.")
        }
    }

    suspend fun getDriverHistory(): Resource<List<GarbageComplaint>> {
        return try {
            val response = apiService.getDriverHistory()
            if (response.isSuccessful && response.body()?.data != null) {
                val list = response.body()!!.data!!.map { it.toDomain() }
                Resource.Success(list)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to load collection history.")
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
