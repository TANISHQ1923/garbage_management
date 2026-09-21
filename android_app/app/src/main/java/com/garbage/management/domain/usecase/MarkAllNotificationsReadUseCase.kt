package com.garbage.management.domain.usecase

import com.garbage.management.domain.repository.NotificationRepository

/**
 * Use case to mark all notifications as read for a given user.
 */
class MarkAllNotificationsReadUseCase(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(userId: String) {
        repository.markAllAsRead(userId)
    }
}
