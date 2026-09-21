package com.garbage.management.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.model.GarbageDriver
import com.garbage.management.domain.model.User
import com.garbage.management.domain.usecase.AddOfficerNoteUseCase
import com.garbage.management.domain.usecase.AssignDriverUseCase
import com.garbage.management.domain.usecase.GetAllComplaintsUseCase
import com.garbage.management.domain.usecase.GetAvailableDriversUseCase
import com.garbage.management.domain.usecase.GetComplaintByIdUseCase
import com.garbage.management.domain.usecase.UpdateComplaintStatusUseCase
import com.garbage.management.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OfficerSortOption(val displayName: String) {
    EMERGENCY_FIRST("Emergency First"),
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First")
}

data class OfficerMetrics(
    val total: Int = 0,
    val submitted: Int = 0,
    val assigned: Int = 0,
    val inProgress: Int = 0,
    val cleaned: Int = 0,
    val verified: Int = 0,
    val rejected: Int = 0,
    val emergency: Int = 0
)

data class OfficerDashboardUiState(
    val allComplaints: List<GarbageComplaint> = emptyList(),
    val metrics: OfficerMetrics = OfficerMetrics(),
    val priorityComplaints: List<GarbageComplaint> = emptyList(),
    val pendingAssignments: List<GarbageComplaint> = emptyList(),
    val recentComplaints: List<GarbageComplaint> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class OfficerComplaintsUiState(
    val complaints: List<GarbageComplaint> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: String = "All",
    val selectedSort: OfficerSortOption = OfficerSortOption.EMERGENCY_FIRST,
    val totalCount: Int = 0
)

data class OfficerComplaintDetailUiState(
    val complaint: GarbageComplaint? = null,
    val availableDrivers: List<GarbageDriver> = emptyList(),
    val isLoading: Boolean = false,
    val isAssigning: Boolean = false,
    val isUpdatingStatus: Boolean = false,
    val isAddingNote: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class OfficerViewModel(
    private val getAllComplaintsUseCase: GetAllComplaintsUseCase,
    private val getComplaintByIdUseCase: GetComplaintByIdUseCase,
    private val getAvailableDriversUseCase: GetAvailableDriversUseCase,
    private val assignDriverUseCase: AssignDriverUseCase,
    private val updateComplaintStatusUseCase: UpdateComplaintStatusUseCase,
    private val addOfficerNoteUseCase: AddOfficerNoteUseCase
) : ViewModel() {

    private val _dashboardState = MutableStateFlow(OfficerDashboardUiState(isLoading = true))
    val dashboardState: StateFlow<OfficerDashboardUiState> = _dashboardState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow("All")
    private val _selectedSort = MutableStateFlow(OfficerSortOption.EMERGENCY_FIRST)

    private val _detailState = MutableStateFlow(OfficerComplaintDetailUiState())
    val detailState: StateFlow<OfficerComplaintDetailUiState> = _detailState.asStateFlow()

    val complaintsListState: StateFlow<OfficerComplaintsUiState> = combine(
        getAllComplaintsUseCase(),
        _searchQuery,
        _selectedFilter,
        _selectedSort
    ) { allComplaints, query, filter, sort ->
        val filtered = allComplaints.filter { complaint ->
            // Search criteria: complaint ID, citizen name, garbage type, location description
            val matchesQuery = if (query.isBlank()) true else {
                complaint.id.contains(query, ignoreCase = true) ||
                        complaint.citizenName.contains(query, ignoreCase = true) ||
                        complaint.garbageType.displayName.contains(query, ignoreCase = true) ||
                        complaint.locationDescription.contains(query, ignoreCase = true)
            }

            // Filter criteria: All, Submitted, Assigned, Pickup In Progress, Cleaned, Verified, Rejected, Emergency
            val matchesFilter = when (filter) {
                "All" -> true
                "Submitted" -> complaint.status == ComplaintStatus.SUBMITTED
                "Assigned" -> complaint.status == ComplaintStatus.ASSIGNED
                "In Progress", "Pickup In Progress" -> complaint.status == ComplaintStatus.PICKUP_IN_PROGRESS
                "Cleaned" -> complaint.status == ComplaintStatus.CLEANED
                "Verified" -> complaint.status == ComplaintStatus.VERIFIED
                "Rejected" -> complaint.status == ComplaintStatus.REJECTED
                "Emergency" -> complaint.isEmergency
                else -> true
            }

            matchesQuery && matchesFilter
        }

        // Sorting rule: Local rule-based order
        val sorted = when (sort) {
            OfficerSortOption.EMERGENCY_FIRST -> {
                filtered.sortedWith(
                    compareByDescending<GarbageComplaint> { it.isEmergency }
                        .thenBy { it.createdAt }
                )
            }
            OfficerSortOption.NEWEST_FIRST -> {
                filtered.sortedByDescending { it.createdAt }
            }
            OfficerSortOption.OLDEST_FIRST -> {
                filtered.sortedBy { it.createdAt }
            }
        }

        OfficerComplaintsUiState(
            complaints = sorted,
            searchQuery = query,
            selectedFilter = filter,
            selectedSort = sort,
            totalCount = sorted.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OfficerComplaintsUiState()
    )

    init {
        observeAllComplaints()
        loadAvailableDrivers()
    }

    fun refreshAllComplaints() {
        observeAllComplaints()
        loadAvailableDrivers()
    }

    private fun observeAllComplaints() {
        viewModelScope.launch {
            getAllComplaintsUseCase().collect { complaints ->
                val metrics = OfficerMetrics(
                    total = complaints.size,
                    submitted = complaints.count { it.status == ComplaintStatus.SUBMITTED },
                    assigned = complaints.count { it.status == ComplaintStatus.ASSIGNED },
                    inProgress = complaints.count { it.status == ComplaintStatus.PICKUP_IN_PROGRESS },
                    cleaned = complaints.count { it.status == ComplaintStatus.CLEANED },
                    verified = complaints.count { it.status == ComplaintStatus.VERIFIED },
                    rejected = complaints.count { it.status == ComplaintStatus.REJECTED },
                    emergency = complaints.count { it.isEmergency }
                )

                // Rule-based priority requests:
                // 1. Emergency complaints that are not yet cleaned/verified
                // 2. Oldest complaints waiting for assignment or in progress
                val priority = complaints
                    .filter { it.status != ComplaintStatus.CLEANED && it.status != ComplaintStatus.VERIFIED && it.status != ComplaintStatus.REJECTED }
                    .sortedWith(
                        compareByDescending<GarbageComplaint> { it.isEmergency }
                            .thenBy { it.createdAt }
                    )

                val pending = complaints.filter { it.status == ComplaintStatus.SUBMITTED }
                    .sortedBy { it.createdAt }

                val recent = complaints.sortedByDescending { it.createdAt }.take(5)

                _dashboardState.value = OfficerDashboardUiState(
                    allComplaints = complaints,
                    metrics = metrics,
                    priorityComplaints = priority,
                    pendingAssignments = pending,
                    recentComplaints = recent,
                    isLoading = false,
                    error = null
                )

                // Also update detailState if a complaint is currently open
                val currentDetailId = _detailState.value.complaint?.id
                if (currentDetailId != null) {
                    val updated = complaints.find { it.id.equals(currentDetailId, ignoreCase = true) }
                    if (updated != null) {
                        _detailState.update { it.copy(complaint = updated) }
                    }
                }
            }
        }
    }

    private fun loadAvailableDrivers() {
        viewModelScope.launch {
            getAvailableDriversUseCase().collect { drivers ->
                _detailState.update { it.copy(availableDrivers = drivers) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: String) {
        _selectedFilter.value = filter
    }

    fun onSortSelected(sort: OfficerSortOption) {
        _selectedSort.value = sort
    }

    fun loadComplaintDetails(complaintId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, error = null, successMessage = null) }
            when (val result = getComplaintByIdUseCase(complaintId)) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(
                            complaint = result.data,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                is Resource.Error -> {
                    _detailState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message ?: "Complaint not found."
                        )
                    }
                }
                is Resource.Loading -> {
                    _detailState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun assignDriver(
        complaintId: String,
        driver: GarbageDriver,
        officer: User,
        noteText: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _detailState.update { it.copy(isAssigning = true, error = null, successMessage = null) }
            val result = assignDriverUseCase(
                complaintId = complaintId,
                driver = driver,
                officerId = officer.employeeId ?: officer.id,
                officerName = officer.name,
                noteText = noteText
            )
            when (result) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(
                            complaint = result.data,
                            isAssigning = false,
                            successMessage = "Vehicle ${driver.vehicleNumber} assigned to complaint successfully."
                        )
                    }
                    refreshAllComplaints()
                    onSuccess()
                }
                is Resource.Error -> {
                    _detailState.update {
                        it.copy(
                            isAssigning = false,
                            error = result.message ?: "Failed to assign driver."
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun updateStatus(
        complaintId: String,
        newStatus: ComplaintStatus,
        officer: User,
        noteText: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _detailState.update { it.copy(isUpdatingStatus = true, error = null, successMessage = null) }
            val result = updateComplaintStatusUseCase(
                complaintId = complaintId,
                newStatus = newStatus,
                officerId = officer.employeeId ?: officer.id,
                officerName = officer.name,
                noteText = noteText
            )
            when (result) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(
                            complaint = result.data,
                            isUpdatingStatus = false,
                            successMessage = "Status updated to '${newStatus.displayName}'."
                        )
                    }
                    refreshAllComplaints()
                    onSuccess()
                }
                is Resource.Error -> {
                    _detailState.update {
                        it.copy(
                            isUpdatingStatus = false,
                            error = result.message ?: "Failed to update status."
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun addOfficerNote(
        complaintId: String,
        officer: User,
        noteText: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _detailState.update { it.copy(isAddingNote = true, error = null, successMessage = null) }
            val result = addOfficerNoteUseCase(
                complaintId = complaintId,
                officerId = officer.employeeId ?: officer.id,
                officerName = officer.name,
                text = noteText
            )
            when (result) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(
                            complaint = result.data,
                            isAddingNote = false,
                            successMessage = "Internal note recorded."
                        )
                    }
                    refreshAllComplaints()
                    onSuccess()
                }
                is Resource.Error -> {
                    _detailState.update {
                        it.copy(
                            isAddingNote = false,
                            error = result.message ?: "Failed to attach note."
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearMessages() {
        _detailState.update { it.copy(error = null, successMessage = null) }
    }
}
