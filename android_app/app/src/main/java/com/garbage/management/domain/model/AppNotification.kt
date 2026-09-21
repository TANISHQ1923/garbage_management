package com.garbage.management.domain.model

/**
 * Domain entity representing an in-app notification for a user.
 */
data class AppNotification(
    val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val complaintId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
