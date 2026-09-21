package com.garbage.management.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.garbage.management.domain.model.User
import com.garbage.management.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "smart_garbage_session")

/**
 * Manages local session persistence using DataStore Preferences.
 * Stores only non-sensitive user identity and role information for navigation.
 * Never stores plaintext passwords.
 */
class SessionManager(private val context: Context) {

    companion object {
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_USER_ROLE = stringPreferencesKey("user_role")
        val KEY_USER_PHONE = stringPreferencesKey("user_phone")
        val KEY_USER_ADDRESS = stringPreferencesKey("user_address")
        val KEY_USER_EMPLOYEE_ID = stringPreferencesKey("user_employee_id")
        val KEY_USER_WARD = stringPreferencesKey("user_ward")
        val KEY_USER_DRIVER_ID = stringPreferencesKey("user_driver_id")
        val KEY_USER_VEHICLE_NUMBER = stringPreferencesKey("user_vehicle_number")
        val KEY_JWT_TOKEN = stringPreferencesKey("jwt_token")
    }

    val sessionFlow: Flow<User?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isLoggedIn = preferences[KEY_IS_LOGGED_IN] ?: false
            if (!isLoggedIn) {
                null
            } else {
                val roleString = preferences[KEY_USER_ROLE] ?: UserRole.CITIZEN.name
                val role = try {
                    UserRole.valueOf(roleString)
                } catch (e: Exception) {
                    UserRole.CITIZEN
                }

                User(
                    id = preferences[KEY_USER_ID] ?: "",
                    name = preferences[KEY_USER_NAME] ?: "",
                    email = preferences[KEY_USER_EMAIL] ?: "",
                    phone = preferences[KEY_USER_PHONE],
                    role = role,
                    address = preferences[KEY_USER_ADDRESS],
                    employeeId = preferences[KEY_USER_EMPLOYEE_ID],
                    ward = preferences[KEY_USER_WARD],
                    driverId = preferences[KEY_USER_DRIVER_ID],
                    vehicleNumber = preferences[KEY_USER_VEHICLE_NUMBER],
                    token = preferences[KEY_JWT_TOKEN]
                )
            }
        }

    suspend fun saveSession(user: User) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = true
            preferences[KEY_USER_ID] = user.id
            preferences[KEY_USER_NAME] = user.name
            preferences[KEY_USER_EMAIL] = user.email
            preferences[KEY_USER_ROLE] = user.role.name
            user.phone?.let { preferences[KEY_USER_PHONE] = it }
            user.address?.let { preferences[KEY_USER_ADDRESS] = it }
            user.employeeId?.let { preferences[KEY_USER_EMPLOYEE_ID] = it }
            user.ward?.let { preferences[KEY_USER_WARD] = it }
            user.driverId?.let { preferences[KEY_USER_DRIVER_ID] = it }
            user.vehicleNumber?.let { preferences[KEY_USER_VEHICLE_NUMBER] = it }
            user.token?.let { preferences[KEY_JWT_TOKEN] = it }
        }
    }

    suspend fun getToken(): String? {
        val prefs = context.dataStore.data.firstOrNull() ?: return null
        return prefs[KEY_JWT_TOKEN]
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_JWT_TOKEN] = token
        }
    }

    suspend fun getSavedSession(): User? {
        return sessionFlow.firstOrNull()
    }

    suspend fun isLoggedIn(): Boolean {
        val prefs = context.dataStore.data.firstOrNull() ?: return false
        return prefs[KEY_IS_LOGGED_IN] == true
    }

    suspend fun getSavedRole(): UserRole? {
        val user = getSavedSession()
        return user?.role
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
