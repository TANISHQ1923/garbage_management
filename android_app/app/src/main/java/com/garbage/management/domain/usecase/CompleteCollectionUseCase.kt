package com.garbage.management.domain.usecase

import com.garbage.management.data.local.LocalDriverDataSource
import com.garbage.management.domain.model.AppNotification
import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.DriverAvailability
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.model.NotificationType
import com.garbage.management.domain.repository.ComplaintRepository
import com.garbage.management.domain.repository.NotificationRepository
import com.garbage.management.utils.Resource
import java.util.UUID

/**
 * Use case to validate mandatory after-cleaning photo evidence,
 * transition the complaint to CLEANED status, restore driver availability to AVAILABLE,
 * and dispatch the citizen cleanup notification.
 */
class CompleteCollectionUseCase(
    private val complaintRepository: ComplaintRepository,
    private val notificationRepository: NotificationRepository,
    private val driverDataSource: LocalDriverDataSource
) {
    suspend operator fun invoke(
        complaintId: String,
        driverId: String,
        driverName: String,
        afterCleaningUri: String,
        beforeCleaningUri: String? = null
    ): Resource<GarbageComplaint> {
        // 1. Mandatory after-cleaning image validation
        if (afterCleaningUri.isBlank()) {
            return Resource.Error("After-cleaning photo evidence is strictly required before marking collection completed.")
        }

        // 2. Fetch complaint
        val complaintResult = complaintRepository.getComplaintById(complaintId)
        val complaint = complaintResult.data
        if (complaintResult is Resource.Error || complaint == null) {
            return Resource.Error(complaintResult.message ?: "Complaint not found.")
        }

        // 3. Validate driver role transition rules
        if (!StatusTransitionValidator.canDriverTransition(complaint.status, ComplaintStatus.CLEANED)) {
            return Resource.Error("Cannot complete collection: Complaint is in '${complaint.status.displayName}' status.")
        }

        // 4. Persist completion in repository
        val completeResult = complaintRepository.completeCollection(
            complaintId = complaintId,
            driverId = driverId,
            driverName = driverName,
            afterCleaningUri = afterCleaningUri,
            beforeCleaningUri = beforeCleaningUri
        )

        // 5. Restore driver availability to AVAILABLE and notify citizen
        if (completeResult is Resource.Success) {
            driverDataSource.setDriverAvailability(driverId, DriverAvailability.AVAILABLE)

            notificationRepository.createNotification(
                AppNotification(
                    id = UUID.randomUUID().toString(),
                    userId = complaint.citizenId,
                    title = "Garbage Cleanup Completed",
                    message = "The reported garbage for complaint ${complaint.id} has been marked as cleaned.",
                    type = NotificationType.CLEANUP_COMPLETED,
                    complaintId = complaint.id
                )
            )
        }

        return completeResult
    }
}
