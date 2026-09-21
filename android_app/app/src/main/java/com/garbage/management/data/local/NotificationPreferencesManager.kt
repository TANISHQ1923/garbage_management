package com.garbage.management.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.garbage.management.domain.model.NotificationPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.notificationPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "smart_garbage_notification_prefs"
)

/**
 * Manages user notification preference flags using DataStore Preferences.
 */
class NotificationPreferencesManager(private val context: Context) {

    companion object {
        val KEY_COMPLAINT_UPDATES = booleanPreferencesKey("pref_complaint_updates")
        val KEY_EMERGENCY_UPDATES = booleanPreferencesKey("pref_emergency_updates")
        val KEY_CLEANUP_UPDATES = booleanPreferencesKey("pref_cleanup_updates")
        val KEY_SYSTEM_NOTIFICATIONS = booleanPreferencesKey("pref_system_notifications")
    }

    val preferencesFlow: Flow<NotificationPreferences> = context.notificationPreferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            NotificationPreferences(
                complaintUpdatesEnabled = preferences[KEY_COMPLAINT_UPDATES] ?: true,
                emergencyUpdatesEnabled = preferences[KEY_EMERGENCY_UPDATES] ?: true,
                cleanupUpdatesEnabled = preferences[KEY_CLEANUP_UPDATES] ?: true,
                systemNotificationsEnabled = preferences[KEY_SYSTEM_NOTIFICATIONS] ?: true
            )
        }

    suspend fun getPreferences(): NotificationPreferences {
        return preferencesFlow.firstOrNull() ?: NotificationPreferences()
    }

    suspend fun updatePreferences(prefs: NotificationPreferences) {
        context.notificationPreferencesDataStore.edit { preferences ->
            preferences[KEY_COMPLAINT_UPDATES] = prefs.complaintUpdatesEnabled
            preferences[KEY_EMERGENCY_UPDATES] = prefs.emergencyUpdatesEnabled
            preferences[KEY_CLEANUP_UPDATES] = prefs.cleanupUpdatesEnabled
            preferences[KEY_SYSTEM_NOTIFICATIONS] = prefs.systemNotificationsEnabled
        }
    }

    suspend fun setComplaintUpdates(enabled: Boolean) {
        context.notificationPreferencesDataStore.edit { it[KEY_COMPLAINT_UPDATES] = enabled }
    }

    suspend fun setEmergencyUpdates(enabled: Boolean) {
        context.notificationPreferencesDataStore.edit { it[KEY_EMERGENCY_UPDATES] = enabled }
    }

    suspend fun setCleanupUpdates(enabled: Boolean) {
        context.notificationPreferencesDataStore.edit { it[KEY_CLEANUP_UPDATES] = enabled }
    }

    suspend fun setSystemNotifications(enabled: Boolean) {
        context.notificationPreferencesDataStore.edit { it[KEY_SYSTEM_NOTIFICATIONS] = enabled }
    }
}
