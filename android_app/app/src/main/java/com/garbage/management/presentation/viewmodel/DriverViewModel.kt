package com.garbage.management.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.garbage.management.di.AppContainer
import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.DriverAvailability
import com.garbage.management.domain.model.DriverLocation
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.usecase.CompleteCollectionUseCase
import com.garbage.management.domain.usecase.GetComplaintByIdUseCase
import com.garbage.management.domain.usecase.GetDriverCollectionHistoryUseCase
import com.garbage.management.domain.usecase.GetDriverComplaintsUseCase
import com.garbage.management.domain.usecase.GetDriverLocationUseCase
import com.garbage.management.domain.usecase.HistoryFilter
import com.garbage.management.domain.usecase.StartCollectionUseCase
import com.garbage.management.domain.usecase.UpdateDriverLocationUseCase
import com.garbage.management.utils.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DriverTaskFilter(val displayName: String) {
    ALL("All"),
    EMERGENCY("Emergency"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    CLEANED("Cleaned"),
    COMPLETED("Completed")
}

enum class DriverTaskSort(val displayName: String) {
    EMERGENCY_FIRST("Emergency First"),
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First")
}

data class DriverUiState(
    val isLoading: Boolean = false,
    val allTasks: List<GarbageComplaint> = emptyList(),
    val filteredTasks: List<GarbageComplaint> = emptyList(),
    val activeTask: GarbageComplaint? = null,
    val activeDriverLocation: DriverLocation? = null,
    val isUpdatingLocation: Boolean = false,
    val driverAvailability: DriverAvailability = DriverAvailability.AVAILABLE,
    val selectedFilter: DriverTaskFilter = DriverTaskFilter.ALL,
    val selectedSort: DriverTaskSort = DriverTaskSort.EMERGENCY_FIRST,
    val selectedHistoryFilter: HistoryFilter = HistoryFilter.TODAY,
    val historyComplaints: List<GarbageComplaint> = emptyList(),
    val currentComplaintDetail: GarbageComplaint? = null,
    val isLoadingDetail: Boolean = false,
    // Metrics
    val assignedCount: Int = 0,
    val inProgressCount: Int = 0,
    val completedTodayCount: Int = 0,
    val emergencyCount: Int = 0,
    // Working photo state for the active task
    val draftBeforeImageUri: String? = null,
    val draftAfterImageUri: String? = null,
    val isUploadingMedia: Boolean = false,
    val uploadProgressMessage: String? = null,
    // Feedback
    val errorMessage: String? = null,
    val userFeedbackMessage: String? = null
)

class DriverViewModel(
    private val getDriverComplaintsUseCase: GetDriverComplaintsUseCase,
    private val startCollectionUseCase: StartCollectionUseCase,
    private val completeCollectionUseCase: CompleteCollectionUseCase,
    private val getDriverCollectionHistoryUseCase: GetDriverCollectionHistoryUseCase,
    private val updateDriverLocationUseCase: UpdateDriverLocationUseCase,
    private val getDriverLocationUseCase: GetDriverLocationUseCase,
    private val getComplaintByIdUseCase: GetComplaintByIdUseCase,
    private val localDriverDataSource: com.garbage.management.data.local.LocalDriverDataSource,
    private val mediaRepository: com.garbage.management.domain.repository.MediaRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriverUiState())
    val uiState: StateFlow<DriverUiState> = _uiState.asStateFlow()

    private var tasksJob: Job? = null
    private var locationJob: Job? = null
    private var historyJob: Job? = null
    private var currentDriverId: String? = null

    fun initializeDriver(driverId: String) {
        if (currentDriverId == driverId && tasksJob != null) return
        currentDriverId = driverId

        // Query initial availability
        val driver = localDriverDataSource.getDriverById(driverId)
        if (driver != null) {
            _uiState.update { it.copy(driverAvailability = driver.availability) }
        }

        observeDriverTasks(driverId)
        observeDriverLocation(driverId)
        observeDriverHistory(driverId, _uiState.value.selectedHistoryFilter)
    }

    fun refreshTasks() {
        currentDriverId?.let { id ->
            observeDriverTasks(id)
            observeDriverHistory(id, _uiState.value.selectedHistoryFilter)
        }
    }

    private fun observeDriverTasks(driverId: String) {
        tasksJob?.cancel()
        tasksJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getDriverComplaintsUseCase(driverId)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to load assigned collection tasks."
                        )
                    }
                }
                .collect { complaints ->
                    val now = System.currentTimeMillis()
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = now
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val startOfToday = cal.timeInMillis

                    // Compute dynamic metrics
                    val assigned = complaints.count { it.status == ComplaintStatus.ASSIGNED }
                    val inProgress = complaints.count { it.status == ComplaintStatus.PICKUP_IN_PROGRESS }
                    val completedToday = complaints.count {
                        (it.status == ComplaintStatus.CLEANED || it.status == ComplaintStatus.VERIFIED) &&
                                it.updatedAt >= startOfToday
                    }
                    val emergency = complaints.count {
                        it.isEmergency && it.status != ComplaintStatus.CLEANED && it.status != ComplaintStatus.VERIFIED
                    }

                    // Find current active task in progress
                    val active = complaints.find { it.status == ComplaintStatus.PICKUP_IN_PROGRESS }

                    // Also if we have a currently open detail, update it
                    val updatedDetail = _uiState.value.currentComplaintDetail?.let { detail ->
                        complaints.find { it.id.equals(detail.id, ignoreCase = true) } ?: detail
                    }

                    _uiState.update { state ->
                        val filtered = applyFilterAndSort(complaints, state.selectedFilter, state.selectedSort)
                        state.copy(
                            isLoading = false,
                            allTasks = complaints,
                            filteredTasks = filtered,
                            activeTask = active,
                            assignedCount = assigned,
                            inProgressCount = inProgress,
                            completedTodayCount = completedToday,
                            emergencyCount = emergency,
                            currentComplaintDetail = updatedDetail,
                            draftBeforeImageUri = updatedDetail?.beforeCleaningImageUri ?: state.draftBeforeImageUri,
                            draftAfterImageUri = updatedDetail?.afterCleaningImageUri ?: state.draftAfterImageUri
                        )
                    }
                }
        }
    }

    private fun observeDriverLocation(driverId: String) {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            getDriverLocationUseCase(driverId).collect { loc ->
                _uiState.update { it.copy(activeDriverLocation = loc) }
            }
        }
    }

    private fun observeDriverHistory(driverId: String, filter: HistoryFilter) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            getDriverCollectionHistoryUseCase(driverId, filter).collect { list ->
                _uiState.update { it.copy(historyComplaints = list, selectedHistoryFilter = filter) }
            }
        }
    }

    fun setFilter(filter: DriverTaskFilter) {
        _uiState.update { state ->
            val filtered = applyFilterAndSort(state.allTasks, filter, state.selectedSort)
            state.copy(selectedFilter = filter, filteredTasks = filtered)
        }
    }

    fun setSort(sort: DriverTaskSort) {
        _uiState.update { state ->
            val filtered = applyFilterAndSort(state.allTasks, state.selectedFilter, sort)
            state.copy(selectedSort = sort, filteredTasks = filtered)
        }
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        currentDriverId?.let { driverId ->
            observeDriverHistory(driverId, filter)
        }
    }

    private fun applyFilterAndSort(
        list: List<GarbageComplaint>,
        filter: DriverTaskFilter,
        sort: DriverTaskSort
    ): List<GarbageComplaint> {
        val filtered = when (filter) {
            DriverTaskFilter.ALL -> list
            DriverTaskFilter.EMERGENCY -> list.filter { it.isEmergency }
            DriverTaskFilter.ASSIGNED -> list.filter { it.status == ComplaintStatus.ASSIGNED }
            DriverTaskFilter.IN_PROGRESS -> list.filter { it.status == ComplaintStatus.PICKUP_IN_PROGRESS }
            DriverTaskFilter.CLEANED -> list.filter { it.status == ComplaintStatus.CLEANED }
            DriverTaskFilter.COMPLETED -> list.filter {
                it.status == ComplaintStatus.CLEANED || it.status == ComplaintStatus.VERIFIED
            }
        }

        return when (sort) {
            DriverTaskSort.EMERGENCY_FIRST -> filtered.sortedWith(
                compareByDescending<GarbageComplaint> { it.isEmergency }
                    .thenByDescending { it.updatedAt }
            )
            DriverTaskSort.NEWEST_FIRST -> filtered.sortedByDescending { it.createdAt }
            DriverTaskSort.OLDEST_FIRST -> filtered.sortedBy { it.createdAt }
        }
    }

    fun loadComplaintDetail(complaintId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetail = true, errorMessage = null) }
            val result = getComplaintByIdUseCase(complaintId)
            when (result) {
                is Resource.Success -> {
                    val complaint = result.data
                    _uiState.update {
                        it.copy(
                            isLoadingDetail = false,
                            currentComplaintDetail = complaint,
                            draftBeforeImageUri = complaint?.beforeCleaningImageUri,
                            draftAfterImageUri = complaint?.afterCleaningImageUri
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingDetail = false,
                            errorMessage = result.message ?: "Complaint not found."
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun setBeforeCleaningImage(uri: String?) {
        _uiState.update { it.copy(draftBeforeImageUri = uri) }
    }

    fun setAfterCleaningImage(uri: String?) {
        _uiState.update { it.copy(draftAfterImageUri = uri) }
    }

    fun startCollection(
        context: Context? = null,
        complaintId: String,
        driverId: String,
        driverName: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            var beforeUri = _uiState.value.draftBeforeImageUri

            // Upload before-photo to Cloudinary if it is a local URI
            if (context != null && mediaRepository != null && !beforeUri.isNullOrBlank() && !beforeUri.startsWith("http")) {
                _uiState.update { it.copy(isUploadingMedia = true, uploadProgressMessage = "Uploading before-cleaning photo...") }
                val uploadResult = mediaRepository.uploadMedia(
                    context = context,
                    uri = Uri.parse(beforeUri),
                    folder = "before-cleaning",
                    complaintId = complaintId
                )
                when (uploadResult) {
                    is Resource.Success -> {
                        beforeUri = uploadResult.data?.url ?: beforeUri
                        _uiState.update { it.copy(draftBeforeImageUri = beforeUri) }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isUploadingMedia = false,
                                uploadProgressMessage = null,
                                errorMessage = "Failed to upload before-cleaning photo: ${uploadResult.message}"
                            )
                        }
                        return@launch
                    }
                    is Resource.Loading -> Unit
                }
            }

            _uiState.update { it.copy(isUploadingMedia = false, uploadProgressMessage = "Starting collection...") }

            val result = startCollectionUseCase(
                complaintId = complaintId,
                driverId = driverId,
                driverName = driverName,
                beforeCleaningUri = beforeUri
            )

            when (result) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isUploadingMedia = false,
                            uploadProgressMessage = null,
                            driverAvailability = DriverAvailability.BUSY,
                            currentComplaintDetail = result.data,
                            userFeedbackMessage = "Collection started for complaint $complaintId."
                        )
                    }
                    currentDriverId?.let { id ->
                        observeDriverTasks(id)
                    }
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isUploadingMedia = false,
                            uploadProgressMessage = null,
                            errorMessage = result.message ?: "Failed to start collection."
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun completeCollection(
        context: Context? = null,
        complaintId: String,
        driverId: String,
        driverName: String,
        onSuccess: () -> Unit = {}
    ) {
        val rawAfterUri = _uiState.value.draftAfterImageUri
        if (rawAfterUri.isNullOrBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Please capture or select an After-Cleaning Photo before completing collection.")
            }
            return
        }
        var afterUri: String = rawAfterUri

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            var beforeUri = _uiState.value.draftBeforeImageUri

            // 1. Upload before photo if local URI
            if (context != null && mediaRepository != null && !beforeUri.isNullOrBlank() && !beforeUri.startsWith("http")) {
                _uiState.update { it.copy(isUploadingMedia = true, uploadProgressMessage = "Uploading before-photo...") }
                val beforeUpload = mediaRepository.uploadMedia(
                    context = context,
                    uri = Uri.parse(beforeUri),
                    folder = "before-cleaning",
                    complaintId = complaintId
                )
                if (beforeUpload is Resource.Success) {
                    beforeUri = beforeUpload.data?.url ?: beforeUri
                    _uiState.update { it.copy(draftBeforeImageUri = beforeUri) }
                }
            }

            // 2. Upload mandatory after photo to Cloudinary if local URI
            if (context != null && mediaRepository != null && !afterUri.startsWith("http")) {
                _uiState.update { it.copy(isUploadingMedia = true, uploadProgressMessage = "Uploading after-photo to cloud...") }
                val uploadResult = mediaRepository.uploadMedia(
                    context = context,
                    uri = Uri.parse(afterUri),
                    folder = "after-cleaning",
                    complaintId = complaintId
                )
                when (uploadResult) {
                    is Resource.Success -> {
                        afterUri = uploadResult.data?.url ?: afterUri
                        _uiState.update { it.copy(draftAfterImageUri = afterUri) }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isUploadingMedia = false,
                                uploadProgressMessage = null,
                                errorMessage = "Failed to upload after-cleaning photo: ${uploadResult.message}. Completion requires cloud photo evidence."
                            )
                        }
                        return@launch
                    }
                    is Resource.Loading -> Unit
                }
            }

            _uiState.update { it.copy(isUploadingMedia = false, uploadProgressMessage = "Completing collection...") }

            val result = completeCollectionUseCase(
                complaintId = complaintId,
                driverId = driverId,
                driverName = driverName,
                afterCleaningUri = afterUri,
                beforeCleaningUri = beforeUri
            )

            when (result) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isUploadingMedia = false,
                            uploadProgressMessage = null,
                            driverAvailability = DriverAvailability.AVAILABLE,
                            currentComplaintDetail = result.data,
                            userFeedbackMessage = "Collection successfully completed!"
                        )
                    }
                    // Refresh history
                    currentDriverId?.let { id ->
                        observeDriverTasks(id)
                        observeDriverHistory(id, _uiState.value.selectedHistoryFilter)
                    }
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isUploadingMedia = false,
                            uploadProgressMessage = null,
                            errorMessage = result.message ?: "Failed to complete collection."
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun updateLocation(driverId: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingLocation = true, errorMessage = null) }
            try {
                updateDriverLocationUseCase(driverId, latitude, longitude)
                _uiState.update {
                    it.copy(
                        isUpdatingLocation = false,
                        userFeedbackMessage = "Driver location updated successfully."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUpdatingLocation = false,
                        errorMessage = "Failed to update location: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun updateAvailability(driverId: String, availability: DriverAvailability) {
        localDriverDataSource.setDriverAvailability(driverId, availability)
        _uiState.update {
            it.copy(
                driverAvailability = availability,
                userFeedbackMessage = "Availability set to ${availability.displayName}."
            )
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(errorMessage = null, userFeedbackMessage = null) }
    }
}

class DriverViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DriverViewModel::class.java)) {
            return DriverViewModel(
                getDriverComplaintsUseCase = appContainer.getDriverComplaintsUseCase,
                startCollectionUseCase = appContainer.startCollectionUseCase,
                completeCollectionUseCase = appContainer.completeCollectionUseCase,
                getDriverCollectionHistoryUseCase = appContainer.getDriverCollectionHistoryUseCase,
                updateDriverLocationUseCase = appContainer.updateDriverLocationUseCase,
                getDriverLocationUseCase = appContainer.getDriverLocationUseCase,
                getComplaintByIdUseCase = appContainer.getComplaintByIdUseCase,
                localDriverDataSource = appContainer.localDriverDataSource,
                mediaRepository = appContainer.mediaRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
