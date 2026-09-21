package com.garbage.management.data.remote

import com.garbage.management.data.model.AddOfficerNoteRequestDto
import com.garbage.management.data.model.ApiResponseDto
import com.garbage.management.data.model.AssignDriverRequestDto
import com.garbage.management.data.model.AuthResponseDto
import com.garbage.management.data.model.ComplaintDto
import com.garbage.management.data.model.CompleteCollectionRequestDto
import com.garbage.management.data.model.CreateComplaintRequestDto
import com.garbage.management.data.model.DriverDto
import com.garbage.management.data.model.DriverLocationDto
import com.garbage.management.data.model.LoginRequestDto
import com.garbage.management.data.model.MediaUploadResponseDto
import com.garbage.management.data.model.NotificationDto
import com.garbage.management.data.model.RegisterRequestDto
import com.garbage.management.data.model.StartCollectionRequestDto
import com.garbage.management.data.model.UpdateEvidenceRequestDto
import com.garbage.management.data.model.UpdateLocationRequestDto
import com.garbage.management.data.model.UpdateStatusRequestDto
import com.garbage.management.data.model.UserDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface defining REST API contracts for the Smart Garbage Management backend.
 */
interface ApiService {

    // =========================================================================
    // HEALTH CHECK
    // =========================================================================
    @GET("health")
    suspend fun checkHealth(): Response<Map<String, Any>>

    // =========================================================================
    // AUTHENTICATION
    // =========================================================================
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequestDto
    ): Response<ApiResponseDto<AuthResponseDto>>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): Response<ApiResponseDto<AuthResponseDto>>

    @GET("auth/me")
    suspend fun getMe(): Response<ApiResponseDto<UserDto>>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(
        @Body body: Map<String, String>
    ): Response<ApiResponseDto<Unit>>

    // =========================================================================
    // CITIZEN COMPLAINTS
    // =========================================================================
    @POST("complaints")
    suspend fun submitComplaint(
        @Body request: CreateComplaintRequestDto
    ): Response<ApiResponseDto<ComplaintDto>>

    @GET("complaints/my")
    suspend fun getMyComplaints(): Response<ApiResponseDto<List<ComplaintDto>>>

    @GET("complaints/{id}")
    suspend fun getComplaintById(
        @Path("id") id: String
    ): Response<ApiResponseDto<ComplaintDto>>

    @POST("complaints/{id}/emergency")
    suspend fun requestEmergency(
        @Path("id") id: String
    ): Response<ApiResponseDto<ComplaintDto>>

    // =========================================================================
    // MUNICIPAL OFFICER
    // =========================================================================
    @GET("officer/complaints")
    suspend fun getAllOfficerComplaints(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
        @Query("emergency") emergency: Boolean? = null,
        @Query("sort") sort: String? = null
    ): Response<ApiResponseDto<List<ComplaintDto>>>

    @GET("officer/complaints/{id}")
    suspend fun getOfficerComplaintById(
        @Path("id") id: String
    ): Response<ApiResponseDto<ComplaintDto>>

    @GET("officer/drivers")
    suspend fun getDrivers(): Response<ApiResponseDto<List<DriverDto>>>

    @PUT("officer/complaints/{id}/assign")
    suspend fun assignDriver(
        @Path("id") id: String,
        @Body request: AssignDriverRequestDto
    ): Response<ApiResponseDto<ComplaintDto>>

    @PUT("officer/complaints/{id}/status")
    suspend fun updateComplaintStatus(
        @Path("id") id: String,
        @Body request: UpdateStatusRequestDto
    ): Response<ApiResponseDto<ComplaintDto>>

    @POST("officer/complaints/{id}/notes")
    suspend fun addOfficerNote(
        @Path("id") id: String,
        @Body request: AddOfficerNoteRequestDto
    ): Response<ApiResponseDto<ComplaintDto>>

    // =========================================================================
    // GARBAGE DRIVER
    // =========================================================================
    @GET("driver/complaints")
    suspend fun getDriverTasks(): Response<ApiResponseDto<List<ComplaintDto>>>

    @GET("driver/complaints/{id}")
    suspend fun getDriverTaskById(
        @Path("id") id: String
    ): Response<ApiResponseDto<ComplaintDto>>

    @PUT("driver/complaints/{id}/start")
    suspend fun startCollection(
        @Path("id") id: String,
        @Body request: StartCollectionRequestDto
    ): Response<ApiResponseDto<ComplaintDto>>

    @PUT("driver/complaints/{id}/complete")
    suspend fun completeCollection(
        @Path("id") id: String,
        @Body request: CompleteCollectionRequestDto
    ): Response<ApiResponseDto<ComplaintDto>>

    @PUT("driver/complaints/{id}/evidence")
    suspend fun updateEvidence(
        @Path("id") id: String,
        @Body request: UpdateEvidenceRequestDto
    ): Response<ApiResponseDto<ComplaintDto>>

    @PUT("driver/location")
    suspend fun updateDriverLocation(
        @Body request: UpdateLocationRequestDto
    ): Response<ApiResponseDto<DriverLocationDto>>

    @GET("driver/location")
    suspend fun getDriverLocation(
        @Query("driverId") driverId: String? = null
    ): Response<ApiResponseDto<DriverLocationDto>>

    @GET("driver/history")
    suspend fun getDriverHistory(): Response<ApiResponseDto<List<ComplaintDto>>>

    // =========================================================================
    // NOTIFICATIONS
    // =========================================================================
    @GET("notifications")
    suspend fun getNotifications(): Response<ApiResponseDto<List<NotificationDto>>>

    @PUT("notifications/{id}/read")
    suspend fun markNotificationRead(
        @Path("id") id: String
    ): Response<ApiResponseDto<NotificationDto>>

    @PUT("notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<ApiResponseDto<Unit>>

    // =========================================================================
    // MEDIA UPLOAD
    // =========================================================================
    @Multipart
    @POST("media/upload")
    suspend fun uploadMedia(
        @Part file: MultipartBody.Part,
        @Part("folder") folder: RequestBody? = null,
        @Part("complaintId") complaintId: RequestBody? = null
    ): Response<ApiResponseDto<MediaUploadResponseDto>>
}
