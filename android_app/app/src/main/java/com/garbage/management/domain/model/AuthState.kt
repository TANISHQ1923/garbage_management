package com.garbage.management.domain.model

/**
 * Sealed hierarchy representing reactive authentication states.
 */
sealed interface AuthState {
    data object Initial : AuthState
    data object Loading : AuthState
    data class Authenticated(val user: User) : AuthState
    data object Unauthenticated : AuthState
    data class Error(val message: String) : AuthState
}
