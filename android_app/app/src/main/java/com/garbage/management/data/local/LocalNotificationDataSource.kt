package com.garbage.management.data.local

import com.garbage.management.domain.model.AppNotification
import com.garbage.management.domain.model.NotificationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory thread-safe local data source for notifications.
 * Preloaded with realistic initial notifications for development and demo accounts.
 */
class LocalNotificationDataSource {

    private val notificationsFlow = MutableStateFlow<List<AppNotification>>(
        listOf(
            AppNotification(
                id = "NOTIF-2026-0005",
                userId = "DRV-882",
                title = "New Collection Assignment",
                message = "Complaint SGM-2026-003890 has been assigned to you.",
                type = NotificationType.DRIVER_ASSIGNMENT,
                complaintId = "SGM-2026-003890",
                createdAt = System.currentTimeMillis() - 3600000 * 2,
                isRead = false
            ),
            AppNotification(
                id = "NOTIF-2026-0004",
                userId = "mock-citizen-001",
                title = "Emergency Pickup Requested",
                message = "Your emergency pickup request SGM-2026-003890 has been received and prioritized.",
                type = NotificationType.EMERGENCY_REQUEST_RECEIVED,
                complaintId = "SGM-2026-003890",
                createdAt = System.currentTimeMillis() - 86400000 * 1 + 300000,
                isRead = false
            ),
            AppNotification(
                id = "NOTIF-2026-0003",
                userId = "mock-citizen-001",
                title = "Garbage Report Submitted",
                message = "Your garbage report SGM-2026-004128 has been submitted successfully.",
                type = NotificationType.COMPLAINT_SUBMITTED,
                complaintId = "SGM-2026-004128",
                createdAt = System.currentTimeMillis() - 3600000 * 5,
                isRead = false
            ),
            AppNotification(
                id = "NOTIF-2026-0002",
                userId = "mock-citizen-001",
                title = "Cleanup Completed",
                message = "Garbage report SGM-2026-002145 has been resolved and marked cleaned.",
                type = NotificationType.CLEANUP_COMPLETED,
                complaintId = "SGM-2026-002145",
                createdAt = System.currentTimeMillis() - 86400000 * 2,
                isRead = true
            ),
            AppNotification(
                id = "NOTIF-2026-0001",
                userId = "mock-citizen-001",
                title = "Welcome to Smart Garbage Management",
                message = "Welcome to the Citizen Portal! You can report overflowing bins, request emergency pickups, and track cleanup progress in real-time.",
                type = NotificationType.SYSTEM_NOTIFICATION,
                complaintId = null,
                createdAt = System.currentTimeMillis() - 86400000 * 7,
                isRead = true
            )
        )
    )

    fun addNotification(notification: AppNotification) {
        notificationsFlow.update { current ->
            listOf(notification) + current
        }
    }

    fun getNotifications(userId: String): Flow<List<AppNotification>> {
        return notificationsFlow.map { list ->
            list.filter {
                it.userId.equals(userId, ignoreCase = true) ||
                        (userId.equals("mock-driver-001", ignoreCase = true) && it.userId.equals("DRV-882", ignoreCase = true)) ||
                        (userId.equals("DRV-882", ignoreCase = true) && it.userId.equals("mock-driver-001", ignoreCase = true))
            }
                .sortedByDescending { it.createdAt }
        }
    }

    fun markAsRead(notificationId: String) {
        notificationsFlow.update { current ->
            current.map { notif ->
                if (notif.id == notificationId) notif.copy(isRead = true) else notif
            }
        }
    }

    fun markAllAsRead(userId: String) {
        notificationsFlow.update { current ->
            current.map { notif ->
                if (notif.userId.equals(userId, ignoreCase = true)) notif.copy(isRead = true) else notif
            }
        }
    }

    fun getUnreadCount(userId: String): Flow<Int> {
        return notificationsFlow.map { list ->
            list.count { it.userId.equals(userId, ignoreCase = true) && !it.isRead }
        }
    }
}
