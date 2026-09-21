package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.model.GarbageType
import com.garbage.management.domain.repository.ComplaintRepository
import com.garbage.management.utils.Resource
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * Use case to validate and submit a citizen garbage report.
 */
class SubmitComplaintUseCase(
    private val repository: ComplaintRepository
) {
    companion object {
        private val counter = AtomicInteger(100)
    }

    suspend operator fun invoke(
        citizenId: String,
        citizenName: String,
        citizenPhone: String?,
        garbageType: GarbageType,
        description: String?,
        imageUri: String?,
        videoUri: String?,
        latitude: Double?,
        longitude: Double?,
        locationDescription: String,
        isEmergency: Boolean
    ): Resource<GarbageComplaint> {
        if (citizenId.isBlank()) {
            return Resource.Error("User session error: Citizen ID missing.")
        }

        val hasGps = latitude != null && longitude != null
        val hasManualLocation = locationDescription.isNotBlank()

        if (!hasGps && !hasManualLocation) {
            return Resource.Error("Please capture your current location or enter a location description manually.")
        }

        // Generate clean unique complaint ID in SGM-2026-XXXXXX format
        val seqNumber = (counter.incrementAndGet() * 1000 + Random.nextInt(100, 999)) % 1000000
        val generatedId = String.format(Locale.US, "SGM-2026-%06d", seqNumber)

        val resolvedLocationDesc = if (hasManualLocation) {
            locationDescription.trim()
        } else {
            String.format(Locale.US, "GPS: %.4f, %.4f", latitude, longitude)
        }

        val now = System.currentTimeMillis()
        val initialHistory = listOf(
            com.garbage.management.domain.model.ComplaintStatusHistory(
                status = ComplaintStatus.SUBMITTED,
                timestamp = now,
                note = "Garbage report submitted successfully"
            )
        )
        val complaint = GarbageComplaint(
            id = generatedId,
            citizenId = citizenId,
            citizenName = citizenName,
            citizenPhone = citizenPhone,
            garbageType = garbageType,
            description = description?.trim()?.ifBlank { null },
            imageUri = imageUri,
            videoUri = videoUri,
            latitude = latitude,
            longitude = longitude,
            locationDescription = resolvedLocationDesc,
            isEmergency = isEmergency,
            status = ComplaintStatus.SUBMITTED,
            statusHistory = initialHistory,
            createdAt = now,
            updatedAt = now
        )

        return repository.submitComplaint(complaint)
    }
}
