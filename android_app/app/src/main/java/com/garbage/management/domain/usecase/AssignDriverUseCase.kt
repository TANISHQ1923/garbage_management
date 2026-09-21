package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.AppNotification
import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.model.GarbageDriver
import com.garbage.management.domain.model.NotificationType
import com.garbage.management.domain.repository.ComplaintRepository
import com.garbage.management.domain.repository.NotificationRepository
import com.garbage.management.utils.Resource
import java.util.UUID

/**
 * Use case to assign a collection driver to a citizen garbage complaint,
 * update the complaint status, append status history, and notify the citizen.
 */
class AssignDriverUseCase(
    private val complaintRepository: ComplaintRepository,
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(
        complaintId: String,
        driver: GarbageDriver,
        officerId: String,
        officerName: String,
        noteText: String? = null
    ): Resource<GarbageComplaint> {
        // 1. Fetch complaint to validate
        val currentComplaintResult = complaintRepository.getComplaintById(complaintId)
        val complaint = currentComplaintResult.data
        if (currentComplaintResult is Resource.Error || complaint == null) {
            return Resource.Error(currentComplaintResult.message ?: "Complaint not found.")
        }

        // 2. Validate status rules
        if (complaint.status == ComplaintStatus.VERIFIED) {
            return Resource.Error("Cannot assign driver: Complaint is already verified.")
        }
        if (complaint.status == ComplaintStatus.REJECTED) {
            return Resource.Error("Cannot assign driver: Complaint has been rejected.")
        }

        // 3. Perform assignment in repository
        val assignResult = complaintRepository.assignDriver(
            complaintId = complaintId,
            driver = driver,
            officerId = officerId,
            officerName = officerName,
            noteText = noteText
        )

        // 4. On success, dispatch citizen notification and driver notification
        if (assignResult is Resource.Success) {
            val now = System.currentTimeMillis()
            notificationRepository.createNotification(
                AppNotification(
                    id = UUID.randomUUID().toString(),
                    userId = complaint.citizenId,
                    title = "Garbage Pickup Assigned",
                    message = "Your complaint ${complaint.id} has been assigned to a collection vehicle (${driver.vehicleNumber}).",
                    type = NotificationType.PICKUP_ASSIGNED,
                    complaintId = complaint.id,
                    createdAt = now
                )
            )

            // Step 6: Dispatch Driver Assignment notification
            notificationRepository.createNotification(
                AppNotification(
                    id = UUID.randomUUID().toString(),
                    userId = driver.id,
                    title = "New Collection Assignment",
                    message = "Complaint ${complaint.id} has been assigned to you.",
                    type = NotificationType.DRIVER_ASSIGNMENT,
                    complaintId = complaint.id,
                    createdAt = now
                )
            )
        }

        return assignResult
    }
}
