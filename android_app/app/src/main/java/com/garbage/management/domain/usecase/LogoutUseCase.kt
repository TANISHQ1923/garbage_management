package com.garbage.management.domain.usecase

import com.garbage.management.domain.repository.AuthRepository
import com.garbage.management.utils.Resource

/**
 * UseCase encapsulating user logout and session cleanup.
 */
class LogoutUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Resource<Unit> {
        return repository.logout()
    }
}
