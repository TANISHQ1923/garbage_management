package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.AppNotification
import com.garbage.management.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to retrieve all notifications belonging to a specific user, sorted newest first.
 */
class GetNotificationsUseCase(
    private val repository: NotificationRepository
) {
    operator fun invoke(userId: String): Flow<List<AppNotification>> {
        return repository.getNotifications(userId)
    }
}
