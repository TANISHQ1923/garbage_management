package com.garbage.management.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.garbage.management.di.AppContainer
import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.model.GarbageType
import com.garbage.management.domain.model.User
import com.garbage.management.domain.usecase.GetCitizenComplaintsUseCase
import com.garbage.management.domain.usecase.GetComplaintByIdUseCase
import com.garbage.management.domain.usecase.SubmitComplaintUseCase
import com.garbage.management.utils.Resource
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportUiState(
    val garbageType: GarbageType = GarbageType.OVERFLOWING_BIN,
    val description: String = "",
    val imageUri: String? = null,
    val videoUri: String? = null,
    val videoFileName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationDescription: String = "",
    val isGpsCaptured: Boolean = false,
    val isEmergency: Boolean = false,
    val showEmergencyConfirmDialog: Boolean = false,
    val isSubmitting: Boolean = false,
    val uploadProgressMessage: String? = null,
    val locationError: String? = null,
    val generalError: String? = null,
    val submittedComplaint: GarbageComplaint? = null
)

data class CitizenDashboardUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val totalComplaints: Int = 0,
    val activeComplaints: Int = 0,
    val pendingRequests: Int = 0,
    val emergencyRequests: Int = 0,
    val completedComplaints: Int = 0,
    val rejectedComplaints: Int = 0,
    val recentComplaints: List<GarbageComplaint> = emptyList(),
    val unreadNotificationsCount: Int = 0,
    val isSynthetic: Boolean = false,
    val errorMessage: String? = null
)

data class ComplaintsListUiState(
    val allComplaints: List<GarbageComplaint> = emptyList(),
    val filteredComplaints: List<GarbageComplaint> = emptyList(),
    val selectedFilter: String = "All",
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ComplaintDetailUiState(
    val complaint: GarbageComplaint? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSubmittingFeedback: Boolean = false,
    val feedbackSuccess: Boolean = false,
    val feedbackError: String? = null
)

class ComplaintViewModel(
    private val submitComplaintUseCase: SubmitComplaintUseCase,
    private val getCitizenComplaintsUseCase: GetCitizenComplaintsUseCase,
    private val getComplaintByIdUseCase: GetComplaintByIdUseCase,
    private val createNotificationUseCase: com.garbage.management.domain.usecase.CreateNotificationUseCase,
    private val systemNotificationHelper: com.garbage.management.utils.SystemNotificationHelper,
    private val preferencesManager: com.garbage.management.data.local.NotificationPreferencesManager,
    private val mediaRepository: com.garbage.management.domain.repository.MediaRepository? = null,
    private val getCitizenDashboardUseCase: com.garbage.management.domain.usecase.GetCitizenDashboardUseCase? = null,
    private val submitFeedbackUseCase: com.garbage.management.domain.usecase.SubmitFeedbackUseCase? = null
) : ViewModel() {

    private val _citizenDashboardUiState = MutableStateFlow(CitizenDashboardUiState(isLoading = true))
    val citizenDashboardUiState: StateFlow<CitizenDashboardUiState> = _citizenDashboardUiState.asStateFlow()

    private val _reportUiState = MutableStateFlow(ReportUiState())
    val reportUiState: StateFlow<ReportUiState> = _reportUiState.asStateFlow()

    private val _complaintsListUiState = MutableStateFlow(ComplaintsListUiState())
    val complaintsListUiState: StateFlow<ComplaintsListUiState> = _complaintsListUiState.asStateFlow()

    private val _complaintDetailUiState = MutableStateFlow(ComplaintDetailUiState())
    val complaintDetailUiState: StateFlow<ComplaintDetailUiState> = _complaintDetailUiState.asStateFlow()

    // ==========================================
    // REPORT FORM ACTIONS
    // ==========================================

    fun onGarbageTypeSelect(type: GarbageType) {
        _reportUiState.update { it.copy(garbageType = type, generalError = null) }
    }

    fun onDescriptionChange(text: String) {
        _reportUiState.update { it.copy(description = text) }
    }

    fun onImageSelected(uri: String) {
        _reportUiState.update { it.copy(imageUri = uri) }
    }

    fun onImageRemoved() {
        _reportUiState.update { it.copy(imageUri = null) }
    }

    fun onVideoSelected(uri: String, fileName: String) {
        _reportUiState.update { it.copy(videoUri = uri, videoFileName = fileName) }
    }

    fun onVideoRemoved() {
        _reportUiState.update { it.copy(videoUri = null, videoFileName = null) }
    }

    fun onLocationCaptured(latitude: Double, longitude: Double) {
        _reportUiState.update {
            it.copy(
                latitude = latitude,
                longitude = longitude,
                isGpsCaptured = true,
                locationError = null,
                generalError = null
            )
        }
    }

    fun onManualLocationChange(text: String) {
        _reportUiState.update {
            it.copy(
                locationDescription = text,
                locationError = null,
                generalError = null
            )
        }
    }

    fun onEmergencyChange(isEmergency: Boolean) {
        _reportUiState.update { it.copy(isEmergency = isEmergency) }
    }

    fun showEmergencyConfirmDialog() {
        _reportUiState.update { it.copy(showEmergencyConfirmDialog = true) }
    }

    fun dismissEmergencyConfirmDialog() {
        _reportUiState.update { it.copy(showEmergencyConfirmDialog = false) }
    }

    fun resetReportForm(initialEmergency: Boolean = false) {
        _reportUiState.value = ReportUiState(isEmergency = initialEmergency)
    }

    fun submitReport(
        context: android.content.Context? = null,
        citizenUser: User,
        onSuccess: (GarbageComplaint) -> Unit
    ) {
        val state = _reportUiState.value

        val hasGps = state.latitude != null && state.longitude != null
        val hasManual = state.locationDescription.isNotBlank()

        if (!hasGps && !hasManual) {
            _reportUiState.update {
                it.copy(locationError = "Please capture your GPS location or enter a location description manually.")
            }
            return
        }

        _reportUiState.update {
            it.copy(
                isSubmitting = true,
                showEmergencyConfirmDialog = false,
                generalError = null,
                uploadProgressMessage = "Preparing submission..."
            )
        }

        viewModelScope.launch {
            var uploadedImageUrl = state.imageUri
            var uploadedVideoUrl = state.videoUri

            // 1. Upload Citizen Image to Cloudinary if local URI
            if (context != null && mediaRepository != null && !state.imageUri.isNullOrBlank() && !state.imageUri.startsWith("http")) {
                _reportUiState.update { it.copy(uploadProgressMessage = "Uploading image...") }
                val imageUploadResult = mediaRepository.uploadMedia(
                    context = context,
                    uri = android.net.Uri.parse(state.imageUri),
                    folder = "complaints"
                )
                when (imageUploadResult) {
                    is Resource.Success -> {
                        uploadedImageUrl = imageUploadResult.data?.url ?: state.imageUri
                    }
                    is Resource.Error -> {
                        val isConnectionError = imageUploadResult.message?.contains("connect", ignoreCase = true) == true
                                || imageUploadResult.message?.contains("timeout", ignoreCase = true) == true
                                || imageUploadResult.message?.contains("route to host", ignoreCase = true) == true
                                || imageUploadResult.message?.contains("Cloudinary", ignoreCase = true) == true
                                || imageUploadResult.message?.contains("test-storage", ignoreCase = true) == true

                        if (isConnectionError) {
                            // Fallback to local image URI so citizen complaint submission can proceed and save offline
                            uploadedImageUrl = state.imageUri
                        } else {
                            _reportUiState.update {
                                it.copy(
                                    isSubmitting = false,
                                    uploadProgressMessage = null,
                                    generalError = "Failed to upload report image: ${imageUploadResult.message}"
                                )
                            }
                            return@launch
                        }
                    }
                    is Resource.Loading -> Unit
                }
            }

            // 2. Upload Citizen Video to Cloudinary if local URI
            if (context != null && mediaRepository != null && !state.videoUri.isNullOrBlank() && !state.videoUri.startsWith("http")) {
                _reportUiState.update { it.copy(uploadProgressMessage = "Uploading video...") }
                val videoUploadResult = mediaRepository.uploadMedia(
                    context = context,
                    uri = android.net.Uri.parse(state.videoUri),
                    folder = "videos"
                )
                when (videoUploadResult) {
                    is Resource.Success -> {
                        uploadedVideoUrl = videoUploadResult.data?.url ?: state.videoUri
                    }
                    is Resource.Error -> {
                        val isConnectionError = videoUploadResult.message?.contains("connect", ignoreCase = true) == true
                                || videoUploadResult.message?.contains("timeout", ignoreCase = true) == true
                                || videoUploadResult.message?.contains("route to host", ignoreCase = true) == true
                                || videoUploadResult.message?.contains("Cloudinary", ignoreCase = true) == true
                                || videoUploadResult.message?.contains("test-storage", ignoreCase = true) == true

                        if (isConnectionError) {
                            uploadedVideoUrl = state.videoUri
                        } else {
                            _reportUiState.update {
                                it.copy(
                                    isSubmitting = false,
                                    uploadProgressMessage = null,
                                    generalError = "Failed to upload report video: ${videoUploadResult.message}"
                                )
                            }
                            return@launch
                        }
                    }
                    is Resource.Loading -> Unit
                }
            }

            _reportUiState.update { it.copy(uploadProgressMessage = "Submitting complaint...") }

            val result = submitComplaintUseCase(
                citizenId = citizenUser.id,
                citizenName = citizenUser.name,
                citizenPhone = citizenUser.phone,
                garbageType = state.garbageType,
                description = state.description,
                imageUri = uploadedImageUrl,
                videoUri = uploadedVideoUrl,
                latitude = state.latitude,
                longitude = state.longitude,
                locationDescription = state.locationDescription,
                isEmergency = state.isEmergency
            )

            when (result) {
                is Resource.Success -> {
                    val complaint = result.data!!
                    _reportUiState.update {
                        it.copy(
                            isSubmitting = false,
                            uploadProgressMessage = null,
                            submittedComplaint = complaint,
                            generalError = null
                        )
                    }

                    // 1. Create in-app notification for complaint submission
                    val notifTimestamp = System.currentTimeMillis()
                    val submissionNotif = com.garbage.management.domain.model.AppNotification(
                        id = "NOTIF-$notifTimestamp",
                        userId = citizenUser.id,
                        title = "Garbage Report Submitted",
                        message = "Your garbage report ${complaint.id} has been submitted successfully.",
                        type = com.garbage.management.domain.model.NotificationType.COMPLAINT_SUBMITTED,
                        complaintId = complaint.id,
                        createdAt = notifTimestamp,
                        isRead = false
                    )
                    createNotificationUseCase(submissionNotif)

                    // 2. If emergency, create emergency request received notification
                    if (complaint.isEmergency) {
                        val emergencyNotif = com.garbage.management.domain.model.AppNotification(
                            id = "NOTIF-${notifTimestamp + 1}",
                            userId = citizenUser.id,
                            title = "Emergency Pickup Requested",
                            message = "Your emergency pickup request ${complaint.id} has been received.",
                            type = com.garbage.management.domain.model.NotificationType.EMERGENCY_REQUEST_RECEIVED,
                            complaintId = complaint.id,
                            createdAt = notifTimestamp + 1,
                            isRead = false
                        )
                        createNotificationUseCase(emergencyNotif)
                    }

                    // 3. Post system notification if preferences allow
                    val prefs = preferencesManager.getPreferences()
                    if (prefs.systemNotificationsEnabled) {
                        if (complaint.isEmergency && prefs.emergencyUpdatesEnabled) {
                            systemNotificationHelper.postSystemNotification(
                                notificationId = complaint.id.hashCode(),
                                title = "Emergency Pickup Requested",
                                message = "Your emergency pickup request ${complaint.id} has been received.",
                                complaintId = complaint.id
                            )
                        } else if (prefs.complaintUpdatesEnabled) {
                            systemNotificationHelper.postSystemNotification(
                                notificationId = complaint.id.hashCode(),
                                title = "Garbage Report Submitted",
                                message = "Your garbage report ${complaint.id} has been submitted successfully.",
                                complaintId = complaint.id
                            )
                        }
                    }

                    // 4. Refresh citizen complaints list so newly created complaint is immediately present
                    loadCitizenComplaints(citizenUser.id)

                    onSuccess(complaint)
                }
                is Resource.Error -> {
                    _reportUiState.update {
                        it.copy(
                            isSubmitting = false,
                            uploadProgressMessage = null,
                            generalError = result.message ?: "Failed to submit report."
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    // ==========================================
    // CITIZEN COMPLAINTS LIST ACTIONS
    // ==========================================

    fun loadCitizenComplaints(citizenId: String) {
        _complaintsListUiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            getCitizenComplaintsUseCase(citizenId).catch { error ->
                _complaintsListUiState.update { it.copy(isLoading = false, error = error.message) }
            }.collect { list ->
                _complaintsListUiState.update { current ->
                    current.copy(
                        allComplaints = list,
                        filteredComplaints = filterComplaints(list, current.selectedFilter),
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun onFilterSelected(filter: String) {
        _complaintsListUiState.update { current ->
            current.copy(
                selectedFilter = filter,
                filteredComplaints = filterComplaints(current.allComplaints, filter)
            )
        }
    }

    private fun filterComplaints(list: List<GarbageComplaint>, filter: String): List<GarbageComplaint> {
        return when (filter) {
            "Submitted" -> list.filter { it.status == ComplaintStatus.SUBMITTED }
            "Assigned" -> list.filter { it.status == ComplaintStatus.ASSIGNED }
            "In Progress" -> list.filter { it.status == ComplaintStatus.PICKUP_IN_PROGRESS }
            "Cleaned" -> list.filter { it.status == ComplaintStatus.CLEANED }
            "Verified" -> list.filter { it.status == ComplaintStatus.VERIFIED }
            "Rejected" -> list.filter { it.status == ComplaintStatus.REJECTED }
            else -> list
        }
    }

    // ==========================================
    // COMPLAINT DETAILS
    // ==========================================

    fun loadComplaintDetails(complaintId: String) {
        _complaintDetailUiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = getComplaintByIdUseCase(complaintId)) {
                is Resource.Success -> {
                    _complaintDetailUiState.update {
                        it.copy(complaint = result.data, isLoading = false, error = null)
                    }
                }
                is Resource.Error -> {
                    _complaintDetailUiState.update {
                        it.copy(complaint = null, isLoading = false, error = result.message)
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun submitFeedback(
        complaintId: String,
        rating: Int,
        comment: String?,
        citizenId: String? = null,
        citizenName: String? = null
    ) {
        if (_complaintDetailUiState.value.isSubmittingFeedback) return
        _complaintDetailUiState.update {
            it.copy(
                isSubmittingFeedback = true,
                feedbackError = null,
                feedbackSuccess = false
            )
        }
        viewModelScope.launch {
            val useCase = submitFeedbackUseCase
            if (useCase == null) {
                _complaintDetailUiState.update {
                    it.copy(
                        isSubmittingFeedback = false,
                        feedbackError = "Feedback service not configured."
                    )
                }
                return@launch
            }
            when (val result = useCase(complaintId, rating, comment, citizenId, citizenName)) {
                is Resource.Success -> {
                    val updated = result.data
                    _complaintDetailUiState.update {
                        it.copy(
                            complaint = updated ?: it.complaint,
                            isSubmittingFeedback = false,
                            feedbackSuccess = true,
                            feedbackError = null
                        )
                    }
                    if (updated != null) {
                        _complaintsListUiState.update { current ->
                            val updatedAll = current.allComplaints.map { c ->
                                if (c.id == updated.id) updated else c
                            }
                            current.copy(
                                allComplaints = updatedAll,
                                filteredComplaints = filterComplaints(updatedAll, current.selectedFilter)
                            )
                        }
                    }
                }
                is Resource.Error -> {
                    _complaintDetailUiState.update {
                        it.copy(
                            isSubmittingFeedback = false,
                            feedbackSuccess = false,
                            feedbackError = result.message ?: "Failed to submit feedback."
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearFeedbackStatus() {
        _complaintDetailUiState.update {
            it.copy(feedbackSuccess = false, feedbackError = null)
        }
    }

    // ==========================================
    // CITIZEN DASHBOARD
    // ==========================================

    fun loadCitizenDashboard(citizenId: String? = null) {
        val hasData = _citizenDashboardUiState.value.recentComplaints.isNotEmpty()
        _citizenDashboardUiState.update {
            it.copy(
                isLoading = !hasData,
                isRefreshing = hasData,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            if (getCitizenDashboardUseCase != null) {
                getCitizenDashboardUseCase().collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val data = result.data
                            if (data != null) {
                                _citizenDashboardUiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isRefreshing = false,
                                        totalComplaints = data.totalComplaints,
                                        activeComplaints = data.activeComplaints,
                                        pendingRequests = data.pendingRequests,
                                        emergencyRequests = data.emergencyRequests,
                                        completedComplaints = data.completedComplaints,
                                        rejectedComplaints = data.rejectedComplaints,
                                        recentComplaints = data.recentComplaints,
                                        unreadNotificationsCount = data.unreadNotificationsCount,
                                        isSynthetic = data.isSynthetic,
                                        errorMessage = null
                                    )
                                }
                                if (data.allComplaints.isNotEmpty()) {
                                    _complaintsListUiState.update { current ->
                                        current.copy(
                                            allComplaints = data.allComplaints,
                                            filteredComplaints = filterComplaints(data.allComplaints, current.selectedFilter),
                                            isLoading = false,
                                            error = null
                                        )
                                    }
                                }
                            }
                        }
                        is Resource.Error -> {
                            _citizenDashboardUiState.update {
                                it.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    errorMessage = result.message ?: "Unable to load dashboard data. Please check the server connection."
                                )
                            }
                        }
                        is Resource.Loading -> {
                            _citizenDashboardUiState.update { it.copy(isLoading = !hasData) }
                        }
                    }
                }
            } else if (citizenId != null) {
                loadCitizenComplaints(citizenId)
            }
        }
    }
}

class ComplaintViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ComplaintViewModel::class.java)) {
            return ComplaintViewModel(
                submitComplaintUseCase = appContainer.submitComplaintUseCase,
                getCitizenComplaintsUseCase = appContainer.getCitizenComplaintsUseCase,
                getComplaintByIdUseCase = appContainer.getComplaintByIdUseCase,
                createNotificationUseCase = appContainer.createNotificationUseCase,
                systemNotificationHelper = appContainer.systemNotificationHelper,
                preferencesManager = appContainer.notificationPreferencesManager,
                mediaRepository = appContainer.mediaRepository,
                getCitizenDashboardUseCase = appContainer.getCitizenDashboardUseCase,
                submitFeedbackUseCase = appContainer.submitFeedbackUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
