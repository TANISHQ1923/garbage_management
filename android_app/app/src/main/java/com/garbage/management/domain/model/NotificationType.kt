package com.garbage.management.domain.model

/**
 * Categorization of notifications delivered to citizens and municipal users.
 */
enum class NotificationType(val displayName: String) {
    COMPLAINT_SUBMITTED("Complaint Submitted"),
    COMPLAINT_STATUS_UPDATED("Status Updated"),
    EMERGENCY_REQUEST_RECEIVED("Emergency Request Received"),
    PICKUP_ASSIGNED("Pickup Assigned"),
    PICKUP_STARTED("Pickup Started"),
    CLEANUP_COMPLETED("Cleanup Completed"),
    CLEANUP_VERIFIED("Cleanup Verified"),
    DRIVER_ASSIGNMENT("Driver Assignment"),
    SYSTEM_NOTIFICATION("System Notification")
}
