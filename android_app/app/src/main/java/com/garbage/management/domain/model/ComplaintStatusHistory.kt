package com.garbage.management.domain.model

/**
 * Represents a single progression event in a complaint's lifecycle.
 */
data class ComplaintStatusHistory(
    val status: ComplaintStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String
)
