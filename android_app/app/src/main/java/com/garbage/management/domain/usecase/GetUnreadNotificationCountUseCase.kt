package com.garbage.management.domain.usecase

import com.garbage.management.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to stream the real-time unread notification count for a user.
 */
class GetUnreadNotificationCountUseCase(
    private val repository: NotificationRepository
) {
    operator fun invoke(userId: String): Flow<Int> {
        return repository.getUnreadCount(userId)
    }
}
