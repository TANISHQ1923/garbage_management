package com.garbage.management.domain.usecase

import com.garbage.management.domain.repository.NotificationRepository

/**
 * Use case to mark a specific notification as read.
 */
class MarkNotificationReadUseCase(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(notificationId: String) {
        repository.markAsRead(notificationId)
    }
}
