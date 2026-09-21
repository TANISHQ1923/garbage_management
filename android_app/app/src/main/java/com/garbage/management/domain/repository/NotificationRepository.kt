package com.garbage.management.domain.repository

import com.garbage.management.domain.model.AppNotification
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user notifications.
 */
interface NotificationRepository {
    fun getNotifications(userId: String): Flow<List<AppNotification>>
    suspend fun createNotification(notification: AppNotification)
    suspend fun markAsRead(notificationId: String)
    suspend fun markAllAsRead(userId: String)
    fun getUnreadCount(userId: String): Flow<Int>
}
