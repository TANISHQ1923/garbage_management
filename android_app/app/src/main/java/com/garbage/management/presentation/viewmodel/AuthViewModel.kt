package com.garbage.management.presentation.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.garbage.management.data.local.MockAuthDataSource
import com.garbage.management.di.AppContainer
import com.garbage.management.domain.model.AuthState
import com.garbage.management.domain.model.User
import com.garbage.management.domain.model.UserRole
import com.garbage.management.domain.usecase.LoginUseCase
import com.garbage.management.domain.usecase.LogoutUseCase
import com.garbage.management.domain.usecase.RegisterUseCase
import com.garbage.management.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val rememberMe: Boolean = true,
    val selectedRole: UserRole = UserRole.CITIZEN,
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null
)

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val selectedRole: UserRole = UserRole.CITIZEN,
    val address: String = "",
    val employeeId: String = "",
    val ward: String = "",
    val driverId: String = "",
    val vehicleNumber: String = "",
    val isLoading: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val roleFieldError: String? = null,
    val generalError: String? = null,
    val isRegisteredSuccess: Boolean = false
)

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val successMessage: String? = null,
    val generalError: String? = null
)

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val appContainer: AppContainer
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    private val _registerUiState = MutableStateFlow(RegisterUiState())
    val registerUiState: StateFlow<RegisterUiState> = _registerUiState.asStateFlow()

    private val _forgotPasswordUiState = MutableStateFlow(ForgotPasswordUiState())
    val forgotPasswordUiState: StateFlow<ForgotPasswordUiState> = _forgotPasswordUiState.asStateFlow()

    init {
        // Observe current session from SessionManager
        viewModelScope.launch {
            appContainer.sessionManager.sessionFlow.collect { user ->
                if (user != null) {
                    _authState.value = AuthState.Authenticated(user)
                } else if (_authState.value !is AuthState.Loading) {
                    _authState.value = AuthState.Unauthenticated
                }
            }
        }
    }

    // ==========================================
    // LOGIN ACTIONS
    // ==========================================

    fun onLoginEmailChange(email: String) {
        _loginUiState.update {
            it.copy(
                email = email,
                emailError = null,
                generalError = null
            )
        }
    }

    fun onLoginPasswordChange(password: String) {
        _loginUiState.update {
            it.copy(
                password = password,
                passwordError = null,
                generalError = null
            )
        }
    }

    fun toggleLoginPasswordVisibility() {
        _loginUiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onLoginRoleSelect(role: UserRole) {
        _loginUiState.update { it.copy(selectedRole = role, generalError = null) }
    }

    fun onRememberMeChange(enabled: Boolean) {
        _loginUiState.update { it.copy(rememberMe = enabled) }
    }

    fun fillDemoCredentials(email: String, role: UserRole) {
        _loginUiState.update {
            it.copy(
                email = email,
                password = MockAuthDataSource.DEV_DEFAULT_PASSWORD,
                selectedRole = role,
                emailError = null,
                passwordError = null,
                generalError = null
            )
        }
    }

    fun login(onSuccess: (User) -> Unit) {
        val state = _loginUiState.value
        val email = state.email.trim()
        val password = state.password

        var hasError = false
        if (email.isBlank()) {
            _loginUiState.update { it.copy(emailError = "Email cannot be empty.") }
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _loginUiState.update { it.copy(emailError = "Please enter a valid email address.") }
            hasError = true
        }

        if (password.isBlank()) {
            _loginUiState.update { it.copy(passwordError = "Password cannot be empty.") }
            hasError = true
        } else if (password.length < 6) {
            _loginUiState.update { it.copy(passwordError = "Password must be at least 6 characters.") }
            hasError = true
        }

        if (hasError) return

        _loginUiState.update { it.copy(isLoading = true, generalError = null) }
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            when (val result = loginUseCase(email, password, state.selectedRole)) {
                is Resource.Success -> {
                    _loginUiState.update { it.copy(isLoading = false, generalError = null) }
                    val user = result.data!!
                    _authState.value = AuthState.Authenticated(user)
                    onSuccess(user)
                }
                is Resource.Error -> {
                    val message = result.message ?: "Login failed. Please check credentials."
                    _loginUiState.update { it.copy(isLoading = false, generalError = message) }
                    _authState.value = AuthState.Error(message)
                }
                is Resource.Loading -> Unit
            }
        }
    }

    // ==========================================
    // REGISTRATION ACTIONS
    // ==========================================

    fun onRegisterNameChange(name: String) = _registerUiState.update { it.copy(name = name, nameError = null) }
    fun onRegisterEmailChange(email: String) = _registerUiState.update { it.copy(email = email, emailError = null) }
    fun onRegisterPhoneChange(phone: String) = _registerUiState.update { it.copy(phone = phone, phoneError = null) }
    fun onRegisterPasswordChange(password: String) = _registerUiState.update { it.copy(password = password, passwordError = null) }
    fun onRegisterConfirmPasswordChange(confirm: String) = _registerUiState.update { it.copy(confirmPassword = confirm, confirmPasswordError = null) }
    fun toggleRegisterPasswordVisibility() = _registerUiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    fun toggleRegisterConfirmPasswordVisibility() = _registerUiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    fun onRegisterRoleSelect(role: UserRole) = _registerUiState.update { it.copy(selectedRole = role, roleFieldError = null) }
    fun onRegisterAddressChange(address: String) = _registerUiState.update { it.copy(address = address, roleFieldError = null) }
    fun onRegisterEmployeeIdChange(id: String) = _registerUiState.update { it.copy(employeeId = id, roleFieldError = null) }
    fun onRegisterWardChange(ward: String) = _registerUiState.update { it.copy(ward = ward, roleFieldError = null) }
    fun onRegisterDriverIdChange(id: String) = _registerUiState.update { it.copy(driverId = id, roleFieldError = null) }
    fun onRegisterVehicleNumberChange(num: String) = _registerUiState.update { it.copy(vehicleNumber = num, roleFieldError = null) }

    fun register(onSuccess: () -> Unit) {
        val state = _registerUiState.value
        _registerUiState.update { it.copy(isLoading = true, generalError = null) }

        viewModelScope.launch {
            val result = registerUseCase(
                name = state.name,
                email = state.email,
                phone = state.phone,
                password = state.password,
                confirmPassword = state.confirmPassword,
                role = state.selectedRole,
                address = state.address,
                employeeId = state.employeeId,
                ward = state.ward,
                driverId = state.driverId,
                vehicleNumber = state.vehicleNumber
            )

            when (result) {
                is Resource.Success -> {
                    _registerUiState.update {
                        it.copy(
                            isLoading = false,
                            isRegisteredSuccess = true,
                            generalError = null
                        )
                    }
                    onSuccess()
                }
                is Resource.Error -> {
                    _registerUiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = result.message ?: "Registration failed."
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun resetRegisterSuccess() {
        _registerUiState.update { it.copy(isRegisteredSuccess = false) }
    }

    // ==========================================
    // FORGOT PASSWORD ACTIONS
    // ==========================================

    fun onForgotEmailChange(email: String) {
        _forgotPasswordUiState.update {
            it.copy(email = email, emailError = null, generalError = null, successMessage = null)
        }
    }

    fun requestPasswordReset() {
        val email = _forgotPasswordUiState.value.email.trim()
        if (email.isBlank()) {
            _forgotPasswordUiState.update { it.copy(emailError = "Email cannot be empty.") }
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _forgotPasswordUiState.update { it.copy(emailError = "Please enter a valid email address.") }
            return
        }

        _forgotPasswordUiState.update { it.copy(isLoading = true, generalError = null) }
        viewModelScope.launch {
            // Simulated network delay
            kotlinx.coroutines.delay(600)
            _forgotPasswordUiState.update {
                it.copy(
                    isLoading = false,
                    successMessage = "Password reset instructions have been requested for $email. (Mock development confirmation)"
                )
            }
        }
    }

    // ==========================================
    // SESSION & LOGOUT
    // ==========================================

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            _authState.value = AuthState.Unauthenticated
            _loginUiState.update { LoginUiState() }
            onLoggedOut()
        }
    }

    suspend fun checkSession(): UserRole? {
        return appContainer.sessionManager.getSavedRole()
    }
}

class AuthViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(
                loginUseCase = appContainer.loginUseCase,
                registerUseCase = appContainer.registerUseCase,
                logoutUseCase = appContainer.logoutUseCase,
                appContainer = appContainer
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
