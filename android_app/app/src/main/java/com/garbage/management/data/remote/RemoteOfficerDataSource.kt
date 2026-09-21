package com.garbage.management.data.remote

import com.garbage.management.data.model.AddOfficerNoteRequestDto
import com.garbage.management.data.model.AssignDriverRequestDto
import com.garbage.management.data.model.UpdateStatusRequestDto
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.model.GarbageDriver
import com.garbage.management.utils.Resource
import org.json.JSONObject

/**
 * Remote data source for municipal officer workflow API calls.
 */
class RemoteOfficerDataSource(private val apiService: ApiService) {

    suspend fun getAllComplaints(
        search: String? = null,
        status: String? = null,
        emergency: Boolean? = null,
        sort: String? = null
    ): Resource<List<GarbageComplaint>> {
        return try {
            val response = apiService.getAllOfficerComplaints(search, status, emergency, sort)
            if (response.isSuccessful && response.body()?.data != null) {
                val list = response.body()!!.data!!.map { it.toDomain() }
                Resource.Success(list)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch complaints list.")
        }
    }

    suspend fun getComplaintById(id: String): Resource<GarbageComplaint> {
        return try {
            val response = apiService.getOfficerComplaintById(id)
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

    suspend fun getDrivers(): Resource<List<GarbageDriver>> {
        return try {
            val response = apiService.getDrivers()
            if (response.isSuccessful && response.body()?.data != null) {
                val list = response.body()!!.data!!.map { it.toDomain() }
                Resource.Success(list)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to load drivers list.")
        }
    }

    suspend fun assignDriver(
        complaintId: String,
        driverId: String,
        driverName: String? = null,
        vehicleNumber: String? = null,
        noteText: String? = null
    ): Resource<GarbageComplaint> {
        return try {
            val request = AssignDriverRequestDto(
                driverId = driverId,
                driverName = driverName,
                vehicleNumber = vehicleNumber,
                noteText = noteText
            )
            val response = apiService.assignDriver(complaintId, request)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.toDomain())
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to assign driver.")
        }
    }

    suspend fun updateStatus(
        complaintId: String,
        newStatus: String,
        noteText: String? = null
    ): Resource<GarbageComplaint> {
        return try {
            val request = UpdateStatusRequestDto(newStatus = newStatus, noteText = noteText)
            val response = apiService.updateComplaintStatus(complaintId, request)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.toDomain())
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update complaint status.")
        }
    }

    suspend fun addOfficerNote(complaintId: String, text: String): Resource<GarbageComplaint> {
        return try {
            val request = AddOfficerNoteRequestDto(text = text)
            val response = apiService.addOfficerNote(complaintId, request)
            if (response.isSuccessful && response.body()?.data != null) {
                Resource.Success(response.body()!!.data!!.toDomain())
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string(), response.message())
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to attach note.")
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
