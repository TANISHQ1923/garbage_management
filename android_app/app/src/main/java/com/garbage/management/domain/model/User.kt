package com.garbage.management.domain.model

enum class UserRole {
    CITIZEN,
    MUNICIPAL_OFFICER,
    GARBAGE_DRIVER
}

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val role: UserRole,
    val address: String? = null,
    val employeeId: String? = null,
    val ward: String? = null,
    val driverId: String? = null,
    val vehicleNumber: String? = null,
    val token: String? = null
)
