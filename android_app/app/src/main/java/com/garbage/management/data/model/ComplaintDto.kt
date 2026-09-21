package com.garbage.management.data.model

import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.ComplaintStatusHistory
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.model.GarbageType
import com.garbage.management.domain.model.OfficerNote
import com.google.gson.annotations.SerializedName

/**
 * Data transfer object representing a Garbage Complaint from the backend API.
 */
data class ComplaintDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("complaintId")
    val complaintId: String? = null,

    @SerializedName("citizenId")
    val citizenId: String? = null,

    @SerializedName("citizenName")
    val citizenName: String? = null,

    @SerializedName("citizenPhone")
    val citizenPhone: String? = null,

    @SerializedName("garbageType")
    val garbageType: String? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("imageUrl")
    val imageUrl: String? = null,

    @SerializedName("videoUrl")
    val videoUrl: String? = null,

    @SerializedName("latitude")
    val latitude: Double? = null,

    @SerializedName("longitude")
    val longitude: Double? = null,

    @SerializedName("locationText")
    val locationText: String? = null,

    @SerializedName("emergency")
    val emergency: Boolean? = false,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("assignedDriverId")
    val assignedDriverId: String? = null,

    @SerializedName("assignedDriverName")
    val assignedDriverName: String? = null,

    @SerializedName("assignedVehicleNumber")
    val assignedVehicleNumber: String? = null,

    @SerializedName("assignedAt")
    val assignedAt: Long? = null,

    @SerializedName("beforeCleaningImageUrl")
    val beforeCleaningImageUrl: String? = null,

    @SerializedName("afterCleaningImageUrl")
    val afterCleaningImageUrl: String? = null,

    @SerializedName("officerNotes")
    val officerNotes: List<OfficerNoteDto>? = null,

    @SerializedName("statusHistory")
    val statusHistory: List<ComplaintStatusHistoryDto>? = null,

    @SerializedName("aiAnalysis")
    val aiAnalysis: AiAnalysisDto? = null,

    @SerializedName("aiAnalysisStatus")
    val aiAnalysisStatus: String? = null,

    @SerializedName("createdAt")
    val createdAt: Any? = null,

    @SerializedName("updatedAt")
    val updatedAt: Any? = null
) {
    fun toDomain(): GarbageComplaint {
        val finalId = complaintId ?: id ?: ""

        val mappedType = try {
            if (garbageType != null) GarbageType.valueOf(garbageType) else GarbageType.OVERFLOWING_BIN
        } catch (e: Exception) {
            GarbageType.OVERFLOWING_BIN
        }

        val mappedStatus = try {
            if (status != null) ComplaintStatus.valueOf(status) else ComplaintStatus.SUBMITTED
        } catch (e: Exception) {
            ComplaintStatus.SUBMITTED
        }

        val mappedNotes = officerNotes?.map { it.toDomain() } ?: emptyList()
        val mappedHistory = statusHistory?.map { it.toDomain() } ?: emptyList()

        val parsedCreatedAt = when (createdAt) {
            is Number -> createdAt.toLong()
            else -> System.currentTimeMillis()
        }

        val parsedUpdatedAt = when (updatedAt) {
            is Number -> updatedAt.toLong()
            else -> System.currentTimeMillis()
        }

        return GarbageComplaint(
            id = finalId,
            citizenId = citizenId ?: "",
            citizenName = citizenName ?: "Citizen",
            citizenPhone = citizenPhone,
            garbageType = mappedType,
            description = description,
            imageUri = imageUrl,
            videoUri = videoUrl,
            latitude = latitude,
            longitude = longitude,
            locationDescription = locationText ?: "Location not specified",
            isEmergency = emergency ?: false,
            status = mappedStatus,
            statusHistory = mappedHistory,
            assignedDriverId = assignedDriverId,
            assignedDriverName = assignedDriverName,
            assignedVehicleNumber = assignedVehicleNumber,
            assignedAt = assignedAt,
            officerNotes = mappedNotes,
            beforeCleaningImageUri = beforeCleaningImageUrl,
            afterCleaningImageUri = afterCleaningImageUrl,
            aiAnalysis = aiAnalysis?.toDomain(),
            aiAnalysisStatus = aiAnalysisStatus,
            createdAt = parsedCreatedAt,
            updatedAt = parsedUpdatedAt
        )
    }
}

data class AiAnalysisDto(
    @SerializedName("classification")
    val classification: ClassificationDto? = null,

    @SerializedName("severity")
    val severity: SeverityDto? = null,

    @SerializedName("modelStatus")
    val modelStatus: String? = "DEVELOPMENT_BASELINE",

    @SerializedName("analyzedAt")
    val analyzedAt: Any? = null
) {
    fun toDomain(): com.garbage.management.domain.model.AiAnalysis {
        val parsedTime = when (analyzedAt) {
            is Number -> analyzedAt.toLong()
            is String -> {
                try {
                    java.time.Instant.parse(analyzedAt).toEpochMilli()
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
        return com.garbage.management.domain.model.AiAnalysis(
            classification = classification?.toDomain(),
            severity = severity?.toDomain(),
            modelStatus = modelStatus ?: "DEVELOPMENT_BASELINE",
            analyzedAt = parsedTime
        )
    }
}

data class ClassificationDto(
    @SerializedName("category")
    val category: String? = null,

    @SerializedName("confidence")
    val confidence: Double? = null
) {
    fun toDomain(): com.garbage.management.domain.model.AiClassification? {
        val cat = category ?: return null
        return com.garbage.management.domain.model.AiClassification(
            category = cat,
            confidence = confidence
        )
    }
}

data class SeverityDto(
    @SerializedName("level")
    val level: String? = null,

    @SerializedName("confidence")
    val confidence: Double? = null
) {
    fun toDomain(): com.garbage.management.domain.model.AiSeverity? {
        val lvl = level ?: return null
        return com.garbage.management.domain.model.AiSeverity(
            level = lvl,
            confidence = confidence
        )
    }
}

data class OfficerNoteDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("officerId")
    val officerId: String? = null,

    @SerializedName("officerName")
    val officerName: String? = null,

    @SerializedName("text")
    val text: String? = null,

    @SerializedName("createdAt")
    val createdAt: Long? = null
) {
    fun toDomain(): OfficerNote {
        return OfficerNote(
            id = id ?: "",
            officerId = officerId ?: "",
            officerName = officerName ?: "Officer",
            text = text ?: "",
            createdAt = createdAt ?: System.currentTimeMillis()
        )
    }
}

data class ComplaintStatusHistoryDto(
    @SerializedName("status")
    val status: String? = null,

    @SerializedName("timestamp")
    val timestamp: Long? = null,

    @SerializedName("note")
    val note: String? = null
) {
    fun toDomain(): ComplaintStatusHistory {
        val mappedStatus = try {
            if (status != null) ComplaintStatus.valueOf(status) else ComplaintStatus.SUBMITTED
        } catch (e: Exception) {
            ComplaintStatus.SUBMITTED
        }
        return ComplaintStatusHistory(
            status = mappedStatus,
            timestamp = timestamp ?: System.currentTimeMillis(),
            note = note ?: ""
        )
    }
}

data class CreateComplaintRequestDto(
    @SerializedName("garbageType")
    val garbageType: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("imageUrl")
    val imageUrl: String?,

    @SerializedName("videoUrl")
    val videoUrl: String?,

    @SerializedName("latitude")
    val latitude: Double?,

    @SerializedName("longitude")
    val longitude: Double?,

    @SerializedName("locationDescription")
    val locationDescription: String,

    @SerializedName("isEmergency")
    val isEmergency: Boolean
)

data class AssignDriverRequestDto(
    @SerializedName("driverId")
    val driverId: String,

    @SerializedName("driverName")
    val driverName: String? = null,

    @SerializedName("vehicleNumber")
    val vehicleNumber: String? = null,

    @SerializedName("noteText")
    val noteText: String? = null
)

data class UpdateStatusRequestDto(
    @SerializedName("newStatus")
    val newStatus: String,

    @SerializedName("noteText")
    val noteText: String? = null
)

data class AddOfficerNoteRequestDto(
    @SerializedName("text")
    val text: String
)

data class StartCollectionRequestDto(
    @SerializedName("beforeCleaningImageUrl")
    val beforeCleaningImageUrl: String? = null
)

data class CompleteCollectionRequestDto(
    @SerializedName("afterCleaningImageUrl")
    val afterCleaningImageUrl: String,

    @SerializedName("beforeCleaningImageUrl")
    val beforeCleaningImageUrl: String? = null
)

data class UpdateEvidenceRequestDto(
    @SerializedName("beforeCleaningImageUrl")
    val beforeCleaningImageUrl: String? = null,

    @SerializedName("afterCleaningImageUrl")
    val afterCleaningImageUrl: String? = null
)
