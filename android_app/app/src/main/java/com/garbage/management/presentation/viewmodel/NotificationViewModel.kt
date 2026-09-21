package com.garbage.management.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.garbage.management.data.local.NotificationPreferencesManager
import com.garbage.management.di.AppContainer
import com.garbage.management.domain.model.AppNotification
import com.garbage.management.domain.model.NotificationPreferences
import com.garbage.management.domain.usecase.GetNotificationsUseCase
import com.garbage.management.domain.usecase.GetUnreadNotificationCountUseCase
import com.garbage.management.domain.usecase.MarkAllNotificationsReadUseCase
import com.garbage.management.domain.usecase.MarkNotificationReadUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class NotificationPreferencesUiState(
    val preferences: NotificationPreferences = NotificationPreferences(),
    val isSaving: Boolean = false
)

class NotificationViewModel(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val getUnreadNotificationCountUseCase: GetUnreadNotificationCountUseCase,
    private val markNotificationReadUseCase: MarkNotificationReadUseCase,
    private val markAllNotificationsReadUseCase: MarkAllNotificationsReadUseCase,
    private val preferencesManager: NotificationPreferencesManager
) : ViewModel() {

    private val _notificationsUiState = MutableStateFlow(NotificationsUiState())
    val notificationsUiState: StateFlow<NotificationsUiState> = _notificationsUiState.asStateFlow()

    private val _preferencesUiState = MutableStateFlow(NotificationPreferencesUiState())
    val preferencesUiState: StateFlow<NotificationPreferencesUiState> = _preferencesUiState.asStateFlow()

    init {
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesManager.preferencesFlow.collect { prefs ->
                _preferencesUiState.update { it.copy(preferences = prefs) }
            }
        }
    }

    fun loadNotifications(userId: String) {
        if (userId.isBlank()) return
        _notificationsUiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            getNotificationsUseCase(userId).collect { list ->
                _notificationsUiState.update { current ->
                    current.copy(
                        notifications = list,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }

        viewModelScope.launch {
            getUnreadNotificationCountUseCase(userId).collect { count ->
                _notificationsUiState.update { current ->
                    current.copy(unreadCount = count)
                }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            markNotificationReadUseCase(notificationId)
        }
    }

    fun markAllAsRead(userId: String) {
        viewModelScope.launch {
            markAllNotificationsReadUseCase(userId)
        }
    }

    fun updatePreferences(newPrefs: NotificationPreferences) {
        viewModelScope.launch {
            _preferencesUiState.update { it.copy(isSaving = true) }
            preferencesManager.updatePreferences(newPrefs)
            _preferencesUiState.update { it.copy(isSaving = false) }
        }
    }

    fun setComplaintUpdates(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setComplaintUpdates(enabled)
        }
    }

    fun setEmergencyUpdates(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setEmergencyUpdates(enabled)
        }
    }

    fun setCleanupUpdates(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setCleanupUpdates(enabled)
        }
    }

    fun setSystemNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setSystemNotifications(enabled)
        }
    }
}

class NotificationViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            return NotificationViewModel(
                getNotificationsUseCase = appContainer.getNotificationsUseCase,
                getUnreadNotificationCountUseCase = appContainer.getUnreadNotificationCountUseCase,
                markNotificationReadUseCase = appContainer.markNotificationReadUseCase,
                markAllNotificationsReadUseCase = appContainer.markAllNotificationsReadUseCase,
                preferencesManager = appContainer.notificationPreferencesManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
