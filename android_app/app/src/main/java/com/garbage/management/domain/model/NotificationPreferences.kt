package com.garbage.management.domain.model

/**
 * User preference settings for notification channels and alerts.
 */
data class NotificationPreferences(
    val complaintUpdatesEnabled: Boolean = true,
    val emergencyUpdatesEnabled: Boolean = true,
    val cleanupUpdatesEnabled: Boolean = true,
    val systemNotificationsEnabled: Boolean = true
)
