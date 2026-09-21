package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.AppNotification
import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.model.NotificationType
import com.garbage.management.domain.repository.ComplaintRepository
import com.garbage.management.domain.repository.NotificationRepository
import com.garbage.management.utils.Resource
import java.util.UUID

/**
 * Use case to validate and execute complaint status transitions by a municipal officer,
 * maintaining chronological history and notifying the reporting citizen.
 */
class UpdateComplaintStatusUseCase(
    private val complaintRepository: ComplaintRepository,
    private val notificationRepository: NotificationRepository
) {
    suspend operator fun invoke(
        complaintId: String,
        newStatus: ComplaintStatus,
        officerId: String,
        officerName: String,
        noteText: String? = null
    ): Resource<GarbageComplaint> {
        // 1. Fetch complaint
        val currentResult = complaintRepository.getComplaintById(complaintId)
        val complaint = currentResult.data
        if (currentResult is Resource.Error || complaint == null) {
            return Resource.Error(currentResult.message ?: "Complaint not found.")
        }

        // 2. Validate transition with pure domain validator
        if (!StatusTransitionValidator.canTransition(complaint.status, newStatus)) {
            return Resource.Error("Invalid status transition from ${complaint.status.displayName} to ${newStatus.displayName}.")
        }

        // 3. Persist update in repository
        val updateResult = complaintRepository.updateComplaintStatus(
            complaintId = complaintId,
            newStatus = newStatus,
            officerId = officerId,
            officerName = officerName,
            noteText = noteText
        )

        // 4. Generate corresponding citizen notification
        if (updateResult is Resource.Success) {
            val (title, message, type) = when (newStatus) {
                ComplaintStatus.ASSIGNED -> Triple(
                    "Garbage Pickup Assigned",
                    "Your complaint ${complaint.id} has been assigned to a collection vehicle.",
                    NotificationType.PICKUP_ASSIGNED
                )
                ComplaintStatus.PICKUP_IN_PROGRESS -> Triple(
                    "Garbage Collection Started",
                    "Collection has started for complaint ${complaint.id}.",
                    NotificationType.PICKUP_STARTED
                )
                ComplaintStatus.CLEANED -> Triple(
                    "Garbage Cleanup Completed",
                    "The reported garbage for complaint ${complaint.id} has been marked as cleaned.",
                    NotificationType.CLEANUP_COMPLETED
                )
                ComplaintStatus.VERIFIED -> Triple(
                    "Cleanup Verified",
                    "Your complaint ${complaint.id} has been verified as completed.",
                    NotificationType.CLEANUP_VERIFIED
                )
                ComplaintStatus.REJECTED -> Triple(
                    "Complaint Status Updated",
                    "Your complaint ${complaint.id} has been rejected.",
                    NotificationType.COMPLAINT_STATUS_UPDATED
                )
                ComplaintStatus.SUBMITTED -> Triple(
                    "Complaint Status Updated",
                    "Your complaint ${complaint.id} status was updated.",
                    NotificationType.COMPLAINT_STATUS_UPDATED
                )
            }

            notificationRepository.createNotification(
                AppNotification(
                    id = UUID.randomUUID().toString(),
                    userId = complaint.citizenId,
                    title = title,
                    message = message,
                    type = type,
                    complaintId = complaint.id
                )
            )
        }

        return updateResult
    }
}
