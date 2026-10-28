package com.garbage.management.data.model

import com.garbage.management.domain.model.CitizenDashboardData
import com.garbage.management.domain.model.DriverAvailability
import com.garbage.management.domain.model.DriverDashboardData
import com.garbage.management.domain.model.OfficerDashboardData
import com.google.gson.annotations.SerializedName

/**
 * Citizen Dashboard DTO
 */
data class CitizenDashboardDto(
    @SerializedName("citizenId")
    val citizenId: String? = null,

    @SerializedName("citizenName")
    val citizenName: String? = null,

    @SerializedName("metrics")
    val metrics: CitizenMetricsDto? = null,

    @SerializedName("recentComplaints")
    val recentComplaints: List<ComplaintDto>? = null,

    @SerializedName("complaints")
    val complaints: List<ComplaintDto>? = null,

    @SerializedName("notifications")
    val notifications: List<NotificationDto>? = null,

    @SerializedName("unreadNotificationsCount")
    val unreadNotificationsCount: Int? = null,

    @SerializedName("mode")
    val mode: String? = null
) {
    fun toDomain(): CitizenDashboardData {
        val domainRecent = recentComplaints?.map { it.toDomain() } ?: emptyList()
        val domainAll = complaints?.map { it.toDomain() } ?: domainRecent

        return CitizenDashboardData(
            citizenId = citizenId ?: "",
            citizenName = citizenName,
            totalComplaints = metrics?.totalComplaints ?: domainAll.size,
            activeComplaints = metrics?.activeComplaints ?: 0,
            pendingRequests = metrics?.pendingRequests ?: 0,
            emergencyRequests = metrics?.emergencyRequests ?: 0,
            completedComplaints = metrics?.completedComplaints ?: 0,
            rejectedComplaints = metrics?.rejectedComplaints ?: 0,
            recentComplaints = domainRecent,
            allComplaints = domainAll,
            unreadNotificationsCount = unreadNotificationsCount ?: 0,
            isSynthetic = mode == "SYNTHETIC_DEMO"
        )
    }
}

data class CitizenMetricsDto(
    @SerializedName("totalComplaints")
    val totalComplaints: Int? = null,

    @SerializedName("activeComplaints")
    val activeComplaints: Int? = null,

    @SerializedName("pendingRequests")
    val pendingRequests: Int? = null,

    @SerializedName("emergencyRequests")
    val emergencyRequests: Int? = null,

    @SerializedName("completedComplaints")
    val completedComplaints: Int? = null,

    @SerializedName("rejectedComplaints")
    val rejectedComplaints: Int? = null
)

/**
 * Officer Dashboard DTO
 */
data class OfficerDashboardDto(
    @SerializedName("metrics")
    val metrics: OfficerMetricsDto? = null,

    @SerializedName("driverAvailability")
    val driverAvailability: DriverAvailabilitySummaryDto? = null,

    @SerializedName("priorityComplaints")
    val priorityComplaints: List<ComplaintDto>? = null,

    @SerializedName("pendingAssignments")
    val pendingAssignments: List<ComplaintDto>? = null,

    @SerializedName("recentComplaints")
    val recentComplaints: List<ComplaintDto>? = null,

    @SerializedName("aiMetrics")
    val aiMetrics: AiMetricsDto? = null,

    @SerializedName("complaints")
    val complaints: List<ComplaintDto>? = null,

    @SerializedName("mode")
    val mode: String? = null
) {
    fun toDomain(): OfficerDashboardData {
        val domainPriority = priorityComplaints?.map { it.toDomain() } ?: emptyList()
        val domainPending = pendingAssignments?.map { it.toDomain() } ?: emptyList()
        val domainRecent = recentComplaints?.map { it.toDomain() } ?: emptyList()
        val domainAll = complaints?.map { it.toDomain() } ?: domainRecent
        val domainDrivers = driverAvailability?.drivers?.map { it.toDomain() } ?: emptyList()

        return OfficerDashboardData(
            totalComplaints = metrics?.total ?: domainAll.size,
            submitted = metrics?.submitted ?: 0,
            assigned = metrics?.assigned ?: 0,
            inProgress = metrics?.inProgress ?: 0,
            cleaned = metrics?.cleaned ?: 0,
            verified = metrics?.verified ?: 0,
            rejected = metrics?.rejected ?: 0,
            emergency = metrics?.emergency ?: 0,
            activeComplaints = metrics?.activeComplaints ?: 0,
            availableDriversCount = driverAvailability?.available ?: 0,
            totalDriversCount = driverAvailability?.total ?: domainDrivers.size,
            drivers = domainDrivers,
            priorityComplaints = domainPriority,
            pendingAssignments = domainPending,
            recentComplaints = domainRecent,
            allComplaints = domainAll,
            isSynthetic = mode == "SYNTHETIC_DEMO"
        )
    }
}

data class OfficerMetricsDto(
    @SerializedName("total")
    val total: Int? = null,

    @SerializedName("submitted")
    val submitted: Int? = null,

    @SerializedName("assigned")
    val assigned: Int? = null,

    @SerializedName("inProgress")
    val inProgress: Int? = null,

    @SerializedName("cleaned")
    val cleaned: Int? = null,

    @SerializedName("verified")
    val verified: Int? = null,

    @SerializedName("rejected")
    val rejected: Int? = null,

    @SerializedName("emergency")
    val emergency: Int? = null,

    @SerializedName("activeComplaints")
    val activeComplaints: Int? = null
)

data class DriverAvailabilitySummaryDto(
    @SerializedName("total")
    val total: Int? = null,

    @SerializedName("available")
    val available: Int? = null,

    @SerializedName("busy")
    val busy: Int? = null,

    @SerializedName("offline")
    val offline: Int? = null,

    @SerializedName("drivers")
    val drivers: List<DriverDto>? = null
)

data class AiMetricsDto(
    @SerializedName("totalAnalyzed")
    val totalAnalyzed: Int? = null,

    @SerializedName("pending")
    val pending: Int? = null,

    @SerializedName("failed")
    val failed: Int? = null,

    @SerializedName("unavailable")
    val unavailable: Int? = null
)

/**
 * Driver Dashboard DTO
 */
data class DriverDashboardDto(
    @SerializedName("driverId")
    val driverId: String? = null,

    @SerializedName("driverInfo")
    val driverInfo: DriverDto? = null,

    @SerializedName("metrics")
    val metrics: DriverMetricsDto? = null,

    @SerializedName("activeTask")
    val activeTask: ComplaintDto? = null,

    @SerializedName("assignedTasks")
    val assignedTasks: List<ComplaintDto>? = null,

    @SerializedName("collectionHistory")
    val collectionHistory: List<ComplaintDto>? = null,

    @SerializedName("notifications")
    val notifications: List<NotificationDto>? = null,

    @SerializedName("unreadNotificationsCount")
    val unreadNotificationsCount: Int? = null,

    @SerializedName("mode")
    val mode: String? = null
) {
    fun toDomain(): DriverDashboardData {
        val domainAssigned = assignedTasks?.map { it.toDomain() } ?: emptyList()
        val domainHistory = collectionHistory?.map { it.toDomain() } ?: emptyList()
        val domainActive = activeTask?.toDomain()

        val mappedAvailability = when (driverInfo?.availability?.uppercase()) {
            "BUSY" -> DriverAvailability.BUSY
            "OFFLINE" -> DriverAvailability.OFFLINE
            else -> DriverAvailability.AVAILABLE
        }

        return DriverDashboardData(
            driverId = driverId ?: driverInfo?.driverId ?: "",
            driverName = driverInfo?.name ?: "Driver",
            vehicleNumber = driverInfo?.vehicleNumber ?: "DL-01-GB-XXXX",
            vehicleType = driverInfo?.vehicleType ?: "Heavy Compactor Truck",
            availability = mappedAvailability,
            totalAssignedTasks = metrics?.totalAssignedTasks ?: domainAssigned.size,
            assignedTasksCount = metrics?.assignedTasks ?: domainAssigned.size,
            inProgressTasksCount = metrics?.inProgressTasks ?: (if (domainActive != null) 1 else 0),
            completedTasksCount = metrics?.completedTasks ?: domainHistory.size,
            emergencyTasksCount = metrics?.emergencyTasks ?: 0,
            activeTask = domainActive,
            assignedTasks = domainAssigned,
            collectionHistory = domainHistory,
            unreadNotificationsCount = unreadNotificationsCount ?: 0,
            isSynthetic = mode == "SYNTHETIC_DEMO"
        )
    }
}

data class DriverMetricsDto(
    @SerializedName("totalAssignedTasks")
    val totalAssignedTasks: Int? = null,

    @SerializedName("assignedTasks")
    val assignedTasks: Int? = null,

    @SerializedName("inProgressTasks")
    val inProgressTasks: Int? = null,

    @SerializedName("completedTasks")
    val completedTasks: Int? = null,

    @SerializedName("emergencyTasks")
    val emergencyTasks: Int? = null
)
