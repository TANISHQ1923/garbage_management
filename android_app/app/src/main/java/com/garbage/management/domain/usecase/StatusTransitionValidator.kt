package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.ComplaintStatus

/**
 * Domain business rules engine governing permissible complaint status transitions.
 * Enforces municipal operational workflow and prevents illegal state bypasses.
 */
object StatusTransitionValidator {

    /**
     * Returns the list of permitted next statuses for a given current status.
     */
    fun getAllowedNextStatuses(currentStatus: ComplaintStatus): List<ComplaintStatus> {
        return when (currentStatus) {
            ComplaintStatus.SUBMITTED -> listOf(
                ComplaintStatus.ASSIGNED,
                ComplaintStatus.REJECTED
            )
            ComplaintStatus.ASSIGNED -> listOf(
                ComplaintStatus.PICKUP_IN_PROGRESS,
                ComplaintStatus.REJECTED
            )
            ComplaintStatus.PICKUP_IN_PROGRESS -> listOf(
                ComplaintStatus.CLEANED
            )
            ComplaintStatus.CLEANED -> listOf(
                ComplaintStatus.VERIFIED
            )
            ComplaintStatus.VERIFIED -> emptyList() // Terminal completed state
            ComplaintStatus.REJECTED -> emptyList() // Terminal rejected state
        }
    }

    /**
     * Checks whether transitioning from [currentStatus] to [targetStatus] is valid.
     */
    fun canTransition(currentStatus: ComplaintStatus, targetStatus: ComplaintStatus): Boolean {
        if (currentStatus == targetStatus) return false
        return getAllowedNextStatuses(currentStatus).contains(targetStatus)
    }

    /**
     * Returns permissible status transitions specifically authorized for garbage drivers.
     * Enforces that drivers cannot reject, reassign, or verify complaints.
     */
    fun getAllowedDriverTransitions(currentStatus: ComplaintStatus): List<ComplaintStatus> {
        return when (currentStatus) {
            ComplaintStatus.ASSIGNED -> listOf(ComplaintStatus.PICKUP_IN_PROGRESS)
            ComplaintStatus.PICKUP_IN_PROGRESS -> listOf(ComplaintStatus.CLEANED)
            else -> emptyList()
        }
    }

    /**
     * Checks whether a driver is authorized to execute a status change from [currentStatus] to [targetStatus].
     */
    fun canDriverTransition(currentStatus: ComplaintStatus, targetStatus: ComplaintStatus): Boolean {
        if (currentStatus == targetStatus) return false
        return getAllowedDriverTransitions(currentStatus).contains(targetStatus)
    }
}
