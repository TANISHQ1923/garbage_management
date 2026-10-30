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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OfficerSortOption(val displayName: String) {
    PRIORITY_QUEUE("Priority Queue"),
    EMERGENCY_FIRST("Emergency First"),
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First")
}

object PriorityCalculator {
    fun calculateScore(complaint: GarbageComplaint, now: Long = System.currentTimeMillis()): Int {
        var score = 0
        if (complaint.isEmergency) {
            score += 50
        }
        val ageMs = (now - complaint.createdAt).coerceAtLeast(0)
        val hours = ageMs / (1000 * 60 * 60)
        when {
            hours >= 24 -> score += 25
            hours >= 12 -> score += 15
            hours >= 6 -> score += 8
            else -> score += 0
        }
        when (complaint.status) {
            ComplaintStatus.SUBMITTED -> score += 20
            ComplaintStatus.ASSIGNED -> score += 10
            ComplaintStatus.PICKUP_IN_PROGRESS -> score += 5
            else -> {}
        }
        return score.coerceIn(0, 100)
    }

    fun getPriorityLevel(score: Int): String {
        return when {
            score >= 70 -> "CRITICAL"
            score >= 50 -> "HIGH"
            score >= 25 -> "MEDIUM"
            else -> "LOW"
        }
    }

    fun isEligibleForActiveQueue(status: ComplaintStatus): Boolean {
        return status == ComplaintStatus.SUBMITTED ||
               status == ComplaintStatus.ASSIGNED ||
               status == ComplaintStatus.PICKUP_IN_PROGRESS
    }
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
    val bins: List<com.garbage.management.domain.model.SmartBinStatus> = emptyList(),
    val allComplaints: List<GarbageComplaint> = emptyList(),
    val metrics: OfficerMetrics = OfficerMetrics(),
    val priorityComplaints: List<GarbageComplaint> = emptyList(),
    val pendingAssignments: List<GarbageComplaint> = emptyList(),
    val recentComplaints: List<GarbageComplaint> = emptyList(),
    val availableDriversCount: Int = 0,
    val totalDriversCount: Int = 0,
    val isSynthetic: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class OfficerComplaintsUiState(
    val complaints: List<GarbageComplaint> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: String = "All",
    val selectedSort: OfficerSortOption = OfficerSortOption.PRIORITY_QUEUE,
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
    private val addOfficerNoteUseCase: AddOfficerNoteUseCase,
    private val getOfficerDashboardUseCase: com.garbage.management.domain.usecase.GetOfficerDashboardUseCase? = null
) : ViewModel() {

    private val _dashboardState = MutableStateFlow(OfficerDashboardUiState(isLoading = true))
    val dashboardState: StateFlow<OfficerDashboardUiState> = _dashboardState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow("All")
    private val _selectedSort = MutableStateFlow(OfficerSortOption.PRIORITY_QUEUE)

    private val _detailState = MutableStateFlow(OfficerComplaintDetailUiState())
    val detailState: StateFlow<OfficerComplaintDetailUiState> = _detailState.asStateFlow()

    val complaintsListState: StateFlow<OfficerComplaintsUiState> = combine(
        getAllComplaintsUseCase().catch { error ->
            _dashboardState.update { it.copy(isLoading = false, error = error.message) }
            emit(emptyList())
        },
        _searchQuery,
        _selectedFilter,
        _selectedSort
    ) { allComplaints, query, filter, sort ->
        val enriched = allComplaints.map { complaint ->
            val score = complaint.priorityScore ?: PriorityCalculator.calculateScore(complaint)
            val level = complaint.priorityLevel ?: PriorityCalculator.getPriorityLevel(score)
            if (complaint.priorityScore == null || complaint.priorityLevel == null) {
                complaint.copy(priorityScore = score, priorityLevel = level)
            } else {
                complaint
            }
        }

        val filtered = enriched.filter { complaint ->
            // Search criteria: complaint ID, citizen name, garbage type, location description
            val matchesQuery = if (query.isBlank()) true else {
                complaint.id.contains(query, ignoreCase = true) ||
                        complaint.citizenName.contains(query, ignoreCase = true) ||
                        complaint.garbageType.displayName.contains(query, ignoreCase = true) ||
                        complaint.locationDescription.contains(query, ignoreCase = true)
            }

            // Filter criteria: All, Priority Queue, Submitted, Assigned, Pickup In Progress, Cleaned, Verified, Rejected, Emergency
            val matchesFilter = when (filter) {
                "All" -> true
                "Priority Queue" -> PriorityCalculator.isEligibleForActiveQueue(complaint.status)
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

        // Sorting rule: Rule-based priority or chronological order
        val sorted = when (sort) {
            OfficerSortOption.PRIORITY_QUEUE -> {
                filtered.sortedWith(
                    compareByDescending<GarbageComplaint> { it.priorityScore ?: 0 }
                        .thenBy { it.createdAt }
                        .thenBy { it.id }
                )
            }
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
        loadOfficerDashboard()
        observeAllComplaints()
        loadAvailableDrivers()
    }

    fun refreshAllComplaints() {
        loadOfficerDashboard()
        observeAllComplaints()
        loadAvailableDrivers()
    }

    fun loadOfficerDashboard() {
        if (getOfficerDashboardUseCase == null) return

        val hasData = _dashboardState.value.allComplaints.isNotEmpty()
        _dashboardState.update { it.copy(isLoading = !hasData, error = null) }

        viewModelScope.launch {
            getOfficerDashboardUseCase().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val data = result.data
                        if (data != null) {
                            val metrics = OfficerMetrics(
                                total = data.totalComplaints,
                                submitted = data.submitted,
                                assigned = data.assigned,
                                inProgress = data.inProgress,
                                cleaned = data.cleaned,
                                verified = data.verified,
                                rejected = data.rejected,
                                emergency = data.emergency
                            )
                            _dashboardState.update {
                                it.copy(
                                    bins = data.bins,
                                    allComplaints = data.allComplaints,
                                    metrics = metrics,
                                    priorityComplaints = data.priorityComplaints,
                                    pendingAssignments = data.pendingAssignments,
                                    recentComplaints = data.recentComplaints,
                                    availableDriversCount = data.availableDriversCount,
                                    totalDriversCount = data.totalDriversCount,
                                    isSynthetic = data.isSynthetic,
                                    isLoading = false,
                                    error = null
                                )
                            }
                            if (data.drivers.isNotEmpty()) {
                                _detailState.update { it.copy(availableDrivers = data.drivers) }
                            }
                        }
                    }
                    is Resource.Error -> {
                        _dashboardState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message ?: "Unable to load dashboard data. Please check the server connection."
                            )
                        }
                    }
                    is Resource.Loading -> {
                        _dashboardState.update { it.copy(isLoading = !hasData) }
                    }
                }
            }
        }
    }

    private fun observeAllComplaints() {
        viewModelScope.launch {
            getAllComplaintsUseCase().catch { error ->
                _dashboardState.update { it.copy(isLoading = false, error = error.message) }
            }.collect { complaints ->
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

                val enrichedComplaints = complaints.map { complaint ->
                    val score = complaint.priorityScore ?: PriorityCalculator.calculateScore(complaint)
                    val level = complaint.priorityLevel ?: PriorityCalculator.getPriorityLevel(score)
                    if (complaint.priorityScore == null || complaint.priorityLevel == null) {
                        complaint.copy(priorityScore = score, priorityLevel = level)
                    } else {
                        complaint
                    }
                }

                // Rule-based priority requests using active dispatch queue criteria:
                val priority = enrichedComplaints
                    .filter { PriorityCalculator.isEligibleForActiveQueue(it.status) }
                    .sortedWith(
                        compareByDescending<GarbageComplaint> { it.priorityScore ?: 0 }
                            .thenBy { it.createdAt }
                            .thenBy { it.id }
                    )

                val pending = enrichedComplaints.filter { it.status == ComplaintStatus.SUBMITTED }
                    .sortedBy { it.createdAt }

                val recent = enrichedComplaints.sortedByDescending { it.createdAt }.take(5)

                _dashboardState.value = OfficerDashboardUiState(
                    allComplaints = enrichedComplaints,
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
            getAvailableDriversUseCase().catch { error ->
                _detailState.update { it.copy(error = error.message) }
            }.collect { drivers ->
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
