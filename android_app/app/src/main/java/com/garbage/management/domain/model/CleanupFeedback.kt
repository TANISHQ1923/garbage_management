package com.garbage.management.domain.model

/**
 * Domain entity representing citizen feedback submitted after cleanup verification.
 */
data class CleanupFeedback(
    val rating: Int,
    val comment: String = "",
    val submittedAt: Long = System.currentTimeMillis(),
    val citizenId: String,
    val citizenName: String = ""
)
