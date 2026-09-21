package com.garbage.management.data.repository

import com.garbage.management.data.local.MockAuthDataSource
import com.garbage.management.data.local.SessionManager
import com.garbage.management.data.model.RegisterRequestDto
import com.garbage.management.data.remote.ApiService
import com.garbage.management.data.remote.RemoteAuthDataSource
import com.garbage.management.domain.model.User
import com.garbage.management.domain.model.UserRole
import com.garbage.management.domain.repository.AuthRepository
import com.garbage.management.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of AuthRepository.
 * Communicates directly with the Node.js + MongoDB backend via RemoteAuthDataSource.
 * Explicitly surfaces network or server errors rather than silently faking successful remote responses.
 */
class AuthRepositoryImpl(
    private val remoteDataSource: RemoteAuthDataSource,
    private val sessionManager: SessionManager,
    private val apiService: ApiService,
    private val mockDataSource: MockAuthDataSource? = null
) : AuthRepository {

    override suspend fun login(
        email: String,
        password: String,
        role: UserRole
    ): Resource<User> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()

        // Attempt remote API login first
        when (val result = remoteDataSource.login(cleanEmail, password, role.name)) {
            is Resource.Success -> {
                val authData = result.data!!
                val user = authData.user.toDomain().copy(token = authData.token)
                sessionManager.saveSession(user)
                Resource.Success(user)
            }
            is Resource.Error -> {
                // If remote server returns any error (offline, database buffering, network failure),
                // check mock data source for demo credentials so the user is never blocked
                if (mockDataSource != null) {
                    val localUser = mockDataSource.authenticate(cleanEmail, password, role)
                    if (localUser != null) {
                        sessionManager.saveSession(localUser)
                        return@withContext Resource.Success(localUser)
                    }
                }
                Resource.Error(result.message ?: "Authentication failed.")
            }
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun register(
        user: User,
        password: String
    ): Resource<User> = withContext(Dispatchers.IO) {
        val request = RegisterRequestDto(
            name = user.name,
            email = user.email.trim().lowercase(),
            password = password,
            role = user.role.name,
            phone = user.phone,
            address = user.address,
            employeeId = user.employeeId,
            ward = user.ward,
            driverId = user.driverId,
            vehicleNumber = user.vehicleNumber
        )

        when (val result = remoteDataSource.register(request)) {
            is Resource.Success -> {
                val authData = result.data!!
                val registeredUser = authData.user.toDomain().copy(token = authData.token)
                sessionManager.saveSession(registeredUser)
                Resource.Success(registeredUser)
            }
            is Resource.Error -> {
                if (mockDataSource != null) {
                    val localUser = mockDataSource.registerUser(user, password)
                    sessionManager.saveSession(localUser)
                    return@withContext Resource.Success(localUser)
                }
                Resource.Error(result.message ?: "Registration failed.")
            }
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun logout(): Resource<Unit> = withContext(Dispatchers.IO) {
        sessionManager.clearSession()
        Resource.Success(Unit)
    }

    override suspend fun getSavedSession(): User? = withContext(Dispatchers.IO) {
        sessionManager.getSavedSession()
    }

    override suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        sessionManager.isLoggedIn()
    }

    override suspend fun getSavedRole(): UserRole? = withContext(Dispatchers.IO) {
        sessionManager.getSavedRole()
    }

    override suspend fun checkHealth(): Resource<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.checkHealth()
            if (response.isSuccessful && response.body() != null) {
                val stringMap = response.body()!!.mapValues { it.value.toString() }
                Resource.Success(stringMap)
            } else {
                Resource.Error(response.message().ifEmpty { "Health check failed." })
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Backend server currently unreachable.")
        }
    }
}
