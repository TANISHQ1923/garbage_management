package com.garbage.management.data.model

import com.garbage.management.domain.model.AppNotification
import com.garbage.management.domain.model.NotificationType
import com.google.gson.annotations.SerializedName

/**
 * Data transfer object representing an In-App Notification from the backend.
 */
data class NotificationDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("notificationId")
    val notificationId: String? = null,

    @SerializedName("userId")
    val userId: String? = null,

    @SerializedName("type")
    val type: String? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("complaintId")
    val complaintId: String? = null,

    @SerializedName("isRead")
    val isRead: Boolean? = false,

    @SerializedName("createdAt")
    val createdAt: Any? = null
) {
    fun toDomain(): AppNotification {
        val mappedType = try {
            if (type != null) NotificationType.valueOf(type) else NotificationType.SYSTEM_NOTIFICATION
        } catch (e: Exception) {
            NotificationType.SYSTEM_NOTIFICATION
        }

        val parsedCreatedAt = when (createdAt) {
            is Number -> createdAt.toLong()
            else -> System.currentTimeMillis()
        }

        return AppNotification(
            id = notificationId ?: id ?: "",
            userId = userId ?: "",
            title = title ?: "Notification",
            message = message ?: "",
            type = mappedType,
            complaintId = complaintId,
            createdAt = parsedCreatedAt,
            isRead = isRead ?: false
        )
    }
}
