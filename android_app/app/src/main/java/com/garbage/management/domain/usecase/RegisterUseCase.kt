package com.garbage.management.domain.usecase

import android.util.Patterns
import com.garbage.management.domain.model.User
import com.garbage.management.domain.model.UserRole
import com.garbage.management.domain.repository.AuthRepository
import com.garbage.management.utils.Resource

/**
 * UseCase encapsulating user registration validation and execution.
 */
class RegisterUseCase(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(
        name: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String,
        role: UserRole,
        address: String? = null,
        employeeId: String? = null,
        ward: String? = null,
        driverId: String? = null,
        vehicleNumber: String? = null
    ): Resource<User> {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedPhone = phone.trim()

        if (trimmedName.isBlank() || trimmedName.length < 2) {
            return Resource.Error("Please enter your full name (at least 2 characters).")
        }
        if (trimmedEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            return Resource.Error("Please enter a valid email address.")
        }
        if (trimmedPhone.isBlank() || trimmedPhone.replace("[^0-9]".toRegex(), "").length < 10) {
            return Resource.Error("Please enter a valid 10-digit phone number.")
        }
        if (password.length < 6) {
            return Resource.Error("Password must be at least 6 characters.")
        }
        if (password != confirmPassword) {
            return Resource.Error("Passwords do not match.")
        }

        // Role-specific validation
        when (role) {
            UserRole.CITIZEN -> {
                if (address.isNullOrBlank()) {
                    return Resource.Error("Address is required for Citizen registration.")
                }
            }
            UserRole.MUNICIPAL_OFFICER -> {
                if (employeeId.isNullOrBlank()) {
                    return Resource.Error("Employee ID is required for Municipal Officer registration.")
                }
                if (ward.isNullOrBlank()) {
                    return Resource.Error("Ward / Zone is required for Municipal Officer registration.")
                }
            }
            UserRole.GARBAGE_DRIVER -> {
                if (driverId.isNullOrBlank()) {
                    return Resource.Error("Driver ID is required for Garbage Driver registration.")
                }
                if (vehicleNumber.isNullOrBlank()) {
                    return Resource.Error("Vehicle Number is required for Garbage Driver registration.")
                }
            }
        }

        val newUser = User(
            id = "",
            name = trimmedName,
            email = trimmedEmail,
            phone = trimmedPhone,
            role = role,
            address = address?.trim(),
            employeeId = employeeId?.trim(),
            ward = ward?.trim(),
            driverId = driverId?.trim(),
            vehicleNumber = vehicleNumber?.trim()
        )

        return repository.register(newUser, password)
    }
}
