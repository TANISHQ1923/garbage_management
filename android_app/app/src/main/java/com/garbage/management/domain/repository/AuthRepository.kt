package com.garbage.management.domain.repository

import com.garbage.management.domain.model.User
import com.garbage.management.domain.model.UserRole
import com.garbage.management.utils.Resource

/**
 * Interface defining authentication contracts in the domain layer.
 */
interface AuthRepository {
    suspend fun login(email: String, password: String, role: UserRole): Resource<User>
    suspend fun register(user: User, password: String): Resource<User>
    suspend fun logout(): Resource<Unit>
    suspend fun getSavedSession(): User?
    suspend fun isLoggedIn(): Boolean
    suspend fun getSavedRole(): UserRole?
    suspend fun checkHealth(): Resource<Map<String, String>>
}
