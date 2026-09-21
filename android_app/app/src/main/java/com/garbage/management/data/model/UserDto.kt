package com.garbage.management.data.model

import com.garbage.management.domain.model.User
import com.garbage.management.domain.model.UserRole
import com.google.gson.annotations.SerializedName

/**
 * Data transfer object representing a User returned by the backend API.
 */
data class UserDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("role")
    val role: String? = null,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("address")
    val address: String? = null,

    @SerializedName("employeeId")
    val employeeId: String? = null,

    @SerializedName("ward")
    val ward: String? = null,

    @SerializedName("driverId")
    val driverId: String? = null,

    @SerializedName("vehicleNumber")
    val vehicleNumber: String? = null,

    @SerializedName("vehicleType")
    val vehicleType: String? = null,

    @SerializedName("availability")
    val availability: String? = null,

    @SerializedName("token")
    val token: String? = null
) {
    fun toDomain(): User {
        val mappedRole = when (role?.uppercase()) {
            "OFFICER", "MUNICIPAL_OFFICER" -> UserRole.MUNICIPAL_OFFICER
            "DRIVER", "GARBAGE_DRIVER" -> UserRole.GARBAGE_DRIVER
            else -> UserRole.CITIZEN
        }
        return User(
            id = id ?: "",
            name = name ?: "User",
            email = email ?: "",
            phone = phone,
            role = mappedRole,
            address = address,
            employeeId = employeeId,
            ward = ward,
            driverId = driverId,
            vehicleNumber = vehicleNumber,
            token = token
        )
    }

    companion object {
        fun fromDomain(user: User): UserDto {
            return UserDto(
                id = user.id,
                name = user.name,
                email = user.email,
                role = user.role.name,
                phone = user.phone,
                address = user.address,
                employeeId = user.employeeId,
                ward = user.ward,
                driverId = user.driverId,
                vehicleNumber = user.vehicleNumber,
                token = user.token
            )
        }
    }
}

/**
 * Response payload for login and registration requests.
 */
data class AuthResponseDto(
    @SerializedName("user")
    val user: UserDto,

    @SerializedName("token")
    val token: String
)

data class LoginRequestDto(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("role")
    val role: String? = null
)

data class RegisterRequestDto(
    @SerializedName("name")
    val name: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("role")
    val role: String,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("address")
    val address: String? = null,

    @SerializedName("employeeId")
    val employeeId: String? = null,

    @SerializedName("ward")
    val ward: String? = null,

    @SerializedName("driverId")
    val driverId: String? = null,

    @SerializedName("vehicleNumber")
    val vehicleNumber: String? = null,

    @SerializedName("vehicleType")
    val vehicleType: String? = null
)

data class ApiResponseDto<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("count")
    val count: Int? = null,

    @SerializedName("unreadCount")
    val unreadCount: Int? = null
)
