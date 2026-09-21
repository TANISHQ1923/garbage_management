package com.garbage.management.domain.model

/**
 * Domain entity representing an internal municipal note attached to a complaint by an officer.
 * These notes are restricted to municipal personnel and not exposed to citizens.
 */
data class OfficerNote(
    val id: String,
    val officerId: String,
    val officerName: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)
