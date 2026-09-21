package com.garbage.management.domain.usecase

import android.util.Patterns
import com.garbage.management.domain.model.User
import com.garbage.management.domain.model.UserRole
import com.garbage.management.domain.repository.AuthRepository
import com.garbage.management.utils.Resource

/**
 * UseCase encapsulating login validation and authentication execution.
 */
class LoginUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        role: UserRole
    ): Resource<User> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            return Resource.Error("Email address cannot be empty.")
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return Resource.Error("Please enter a valid email address.")
        }
        if (password.isBlank()) {
            return Resource.Error("Password cannot be empty.")
        }
        if (password.length < 6) {
            return Resource.Error("Password must be at least 6 characters.")
        }
        return repository.login(trimmedEmail, password, role)
    }
}
