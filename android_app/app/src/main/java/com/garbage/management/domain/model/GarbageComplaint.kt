package com.garbage.management.domain.model

/**
 * Domain entity representing a citizen garbage issue complaint.
 */
data class GarbageComplaint(
    val id: String,
    val citizenId: String,
    val citizenName: String,
    val citizenPhone: String? = null,
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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
