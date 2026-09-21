package com.garbage.management.domain.model

enum class ComplaintStatus(val displayName: String, val stepOrder: Int) {
    SUBMITTED("Submitted", 1),
    ASSIGNED("Assigned", 2),
    PICKUP_IN_PROGRESS("Pickup In Progress", 3),
    CLEANED("Cleaned", 4),
    VERIFIED("Verified", 5),
    REJECTED("Rejected", -1)
}
