package com.garbage.management.domain.model

data class DispatchResult(
    val assignmentStatus: String,
    val assignedDriverId: String? = null,
    val assignedDriverName: String? = null,
    val assignedVehicleNumber: String? = null,
    val distanceKm: Double? = null,
    val message: String? = null
)

/**
 * Domain entity representing a citizen garbage issue complaint.
 */
data class GarbageComplaint(
    val id: String,
    val citizenId: String,
    val citizenName: String,
    val citizenPhone: String? = null,
    val source: String = "CITIZEN",
    val binId: String? = null,
    val sensorEmptyConfirmedAt: Long? = null,
    val garbageType: GarbageType,
    val description: String? = null,
    val imageUri: String? = null,
    val videoUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationDescription: String,
    val isEmergency: Boolean = false,
    val status: ComplaintStatus = ComplaintStatus.SUBMITTED,
    val statusHistory: List<ComplaintStatusHistory> = emptyList(),
    val assignedDriverId: String? = null,
    val assignedDriverName: String? = null,
    val assignedVehicleNumber: String? = null,
    val assignedAt: Long? = null,
    val officerNotes: List<OfficerNote> = emptyList(),
    val beforeCleaningImageUri: String? = null,
    val afterCleaningImageUri: String? = null,
    val aiAnalysis: AiAnalysis? = null,
    val aiAnalysisStatus: String? = null,
    val feedback: CleanupFeedback? = null,
    val priorityScore: Int? = null,
    val priorityLevel: String? = null,
    val dispatchResult: DispatchResult? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
