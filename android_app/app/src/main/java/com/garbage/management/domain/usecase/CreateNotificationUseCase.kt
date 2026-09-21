package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.AppNotification
import com.garbage.management.domain.repository.NotificationRepository

/**
 * Use case to create and persist a new user notification.
 */
class CreateNotificationUseCase(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(notification: AppNotification) {
        repository.createNotification(notification)
    }
}
