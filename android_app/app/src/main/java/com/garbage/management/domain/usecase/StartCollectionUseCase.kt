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
 * Use case to transition an assigned complaint into PICKUP_IN_PROGRESS,
 * update driver status to BUSY, and notify the reporting citizen.
 */
class StartCollectionUseCase(
    private val complaintRepository: ComplaintRepository,
    private val notificationRepository: NotificationRepository,
    private val driverDataSource: LocalDriverDataSource
) {
    suspend operator fun invoke(
        complaintId: String,
        driverId: String,
        driverName: String,
        beforeCleaningUri: String? = null
    ): Resource<GarbageComplaint> {
        // 1. Fetch complaint
        val complaintResult = complaintRepository.getComplaintById(complaintId)
        val complaint = complaintResult.data
        if (complaintResult is Resource.Error || complaint == null) {
            return Resource.Error(complaintResult.message ?: "Complaint not found.")
        }

        // 2. Validate driver role transition rules
        if (!StatusTransitionValidator.canDriverTransition(complaint.status, ComplaintStatus.PICKUP_IN_PROGRESS)) {
            return Resource.Error("Cannot start collection: Complaint is in '${complaint.status.displayName}' status.")
        }

        // 3. Persist status transition and history
        val startResult = complaintRepository.startCollection(
            complaintId = complaintId,
            driverId = driverId,
            driverName = driverName,
            beforeCleaningUri = beforeCleaningUri
        )

        // 4. Update driver availability to BUSY & emit citizen notification
        if (startResult is Resource.Success) {
            driverDataSource.setDriverAvailability(driverId, DriverAvailability.BUSY)

            notificationRepository.createNotification(
                AppNotification(
                    id = UUID.randomUUID().toString(),
                    userId = complaint.citizenId,
                    title = "Garbage Collection Started",
                    message = "Collection has started for complaint ${complaint.id}.",
                    type = NotificationType.PICKUP_STARTED,
                    complaintId = complaint.id
                )
            )
        }

        return startResult
    }
}
