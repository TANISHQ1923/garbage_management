package com.garbage.management.data.repository

import com.garbage.management.data.local.LocalNotificationDataSource
import com.garbage.management.data.remote.ApiClient
import com.garbage.management.data.remote.RemoteNotificationDataSource
import com.garbage.management.domain.model.AppNotification
import com.garbage.management.domain.repository.NotificationRepository
import com.garbage.management.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Implementation of NotificationRepository synchronizing notifications with
 * the backend REST API while retaining local cache for offline readiness.
 */
class NotificationRepositoryImpl(
    private val remoteDataSource: RemoteNotificationDataSource,
    private val localDataSource: LocalNotificationDataSource,
    private val isLocalOnly: Boolean = false
) : NotificationRepository {

    constructor(localDataSource: LocalNotificationDataSource) : this(
        remoteDataSource = RemoteNotificationDataSource(ApiClient.create()),
        localDataSource = localDataSource,
        isLocalOnly = true
    )

    override fun getNotifications(userId: String): Flow<List<AppNotification>> {
        if (isLocalOnly) {
            return localDataSource.getNotifications(userId)
        }

        return flow {
            val cached = localDataSource.getNotifications(userId).firstOrNull() ?: emptyList()
            if (cached.isNotEmpty()) {
                emit(cached)
            }

            when (val remote = remoteDataSource.getNotifications()) {
                is Resource.Success -> {
                    val list = remote.data ?: emptyList()
                    list.forEach { localDataSource.addNotification(it) }
                    emit(list)
                }
                is Resource.Error -> {
                    if (cached.isEmpty()) {
                        emit(emptyList())
                    }
                }
                is Resource.Loading -> Unit
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun createNotification(notification: AppNotification) {
        localDataSource.addNotification(notification)
    }

    override suspend fun markAsRead(notificationId: String) = withContext(Dispatchers.IO) {
        localDataSource.markAsRead(notificationId)
        if (!isLocalOnly) {
            remoteDataSource.markAsRead(notificationId)
        }
        Unit
    }

    override suspend fun markAllAsRead(userId: String) = withContext(Dispatchers.IO) {
        localDataSource.markAllAsRead(userId)
        if (!isLocalOnly) {
            remoteDataSource.markAllAsRead()
        }
        Unit
    }

    override fun getUnreadCount(userId: String): Flow<Int> {
        return localDataSource.getUnreadCount(userId)
    }
}
