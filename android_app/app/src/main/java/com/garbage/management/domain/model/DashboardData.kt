package com.garbage.management.domain.model

/**
 * Domain model representing aggregated dashboard state for the Citizen Dashboard.
 */
data class CitizenDashboardData(
    val citizenId: String = "",
    val citizenName: String? = null,
    val totalComplaints: Int = 0,
    val activeComplaints: Int = 0,
    val pendingRequests: Int = 0,
    val emergencyRequests: Int = 0,
    val completedComplaints: Int = 0,
    val rejectedComplaints: Int = 0,
    val recentComplaints: List<GarbageComplaint> = emptyList(),
    val allComplaints: List<GarbageComplaint> = emptyList(),
    val unreadNotificationsCount: Int = 0,
    val isSynthetic: Boolean = false
)

/**
 * Domain model representing aggregated dashboard state for the Municipal Officer Dashboard.
 */
data class OfficerDashboardData(
    val totalComplaints: Int = 0,
    val submitted: Int = 0,
    val assigned: Int = 0,
    val inProgress: Int = 0,
    val cleaned: Int = 0,
    val verified: Int = 0,
    val rejected: Int = 0,
    val emergency: Int = 0,
    val activeComplaints: Int = 0,
    val availableDriversCount: Int = 0,
    val totalDriversCount: Int = 0,
    val drivers: List<GarbageDriver> = emptyList(),
    val priorityComplaints: List<GarbageComplaint> = emptyList(),
    val pendingAssignments: List<GarbageComplaint> = emptyList(),
    val recentComplaints: List<GarbageComplaint> = emptyList(),
    val allComplaints: List<GarbageComplaint> = emptyList(),
    val isSynthetic: Boolean = false
)

/**
 * Domain model representing aggregated dashboard state for the Garbage Driver Dashboard.
 */
data class DriverDashboardData(
    val driverId: String = "",
    val driverName: String = "",
    val vehicleNumber: String = "",
    val vehicleType: String = "",
    val availability: DriverAvailability = DriverAvailability.AVAILABLE,
    val totalAssignedTasks: Int = 0,
    val assignedTasksCount: Int = 0,
    val inProgressTasksCount: Int = 0,
    val completedTasksCount: Int = 0,
    val emergencyTasksCount: Int = 0,
    val activeTask: GarbageComplaint? = null,
    val assignedTasks: List<GarbageComplaint> = emptyList(),
    val collectionHistory: List<GarbageComplaint> = emptyList(),
    val unreadNotificationsCount: Int = 0,
    val isSynthetic: Boolean = false
)
