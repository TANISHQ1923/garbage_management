package com.garbage.management.data.local

import com.garbage.management.domain.model.User
import com.garbage.management.domain.model.UserRole
import java.util.UUID

/**
 * ============================================================================
 * DEVELOPMENT DEMO DATA SOURCE
 * ============================================================================
 * Provides mock user accounts for testing the three portal roles.
 * Acts as an offline/development fallback when the remote backend or MongoDB Atlas
 * is temporarily offline or disconnected.
 * ============================================================================
 */
class MockAuthDataSource {

    companion object {
        const val DEV_DEFAULT_PASSWORD = "password123"

        const val DEMO_CITIZEN_EMAIL = "citizen@example.com"
        const val DEMO_CITIZEN2_EMAIL = "citizen2@example.com"
        const val DEMO_CITIZEN3_EMAIL = "citizen3@example.com"

        const val DEMO_OFFICER_EMAIL = "officer@example.com"
        const val DEMO_OFFICER2_EMAIL = "officer2@example.com"

        const val DEMO_DRIVER_EMAIL = "driver@example.com"
        const val DEMO_DRIVER2_EMAIL = "driver2@example.com"
        const val DEMO_DRIVER3_EMAIL = "driver3@example.com"
        const val DEMO_DRIVER4_EMAIL = "driver4@example.com"
        const val DEMO_DRIVER5_EMAIL = "driver5@example.com"
    }

    private val mockUsers = mutableListOf(
        // Citizens
        User(
            id = "mock-citizen-001",
            name = "Aarav Sharma (Citizen)",
            email = DEMO_CITIZEN_EMAIL,
            phone = "+91 9876543210",
            role = UserRole.CITIZEN,
            address = "Sector 4, Green Park, Smart City"
        ),
        User(
            id = "mock-citizen-002",
            name = "Priya Singh (Citizen)",
            email = DEMO_CITIZEN2_EMAIL,
            phone = "+91 9876543220",
            role = UserRole.CITIZEN,
            address = "Tower B, Lotus Apartments, Ward 12"
        ),
        User(
            id = "mock-citizen-003",
            name = "Rahul Mehta (Citizen)",
            email = DEMO_CITIZEN3_EMAIL,
            phone = "+91 9876543230",
            role = UserRole.CITIZEN,
            address = "42 Heritage Colony, Central Zone"
        ),
        // Municipal Officers
        User(
            id = "mock-officer-001",
            name = "Vikram Patel (Officer)",
            email = DEMO_OFFICER_EMAIL,
            phone = "+91 9876543211",
            role = UserRole.MUNICIPAL_OFFICER,
            employeeId = "MC-OFF-2026",
            ward = "Ward 12 - Central Zone"
        ),
        User(
            id = "mock-officer-002",
            name = "Ananya Deshmukh (Officer)",
            email = DEMO_OFFICER2_EMAIL,
            phone = "+91 9876543221",
            role = UserRole.MUNICIPAL_OFFICER,
            employeeId = "MC-OFF-2027",
            ward = "Ward 14 - North Zone"
        ),
        // Garbage Drivers
        User(
            id = "mock-driver-001",
            name = "Ramesh Kumar (Driver)",
            email = DEMO_DRIVER_EMAIL,
            phone = "+91 9876543212",
            role = UserRole.GARBAGE_DRIVER,
            driverId = "DRV-882",
            vehicleNumber = "DL-01-GB-4040"
        ),
        User(
            id = "mock-driver-002",
            name = "Sunil Verma (Driver)",
            email = DEMO_DRIVER2_EMAIL,
            phone = "+91 9876543215",
            role = UserRole.GARBAGE_DRIVER,
            driverId = "DRV-104",
            vehicleNumber = "DL-01-GB-5050"
        ),
        User(
            id = "mock-driver-003",
            name = "Amitabh Ghosh (Driver)",
            email = DEMO_DRIVER3_EMAIL,
            phone = "+91 9876543216",
            role = UserRole.GARBAGE_DRIVER,
            driverId = "DRV-305",
            vehicleNumber = "DL-01-GB-6060"
        ),
        User(
            id = "mock-driver-004",
            name = "Rajesh Chauhan (Driver)",
            email = DEMO_DRIVER4_EMAIL,
            phone = "+91 9876543217",
            role = UserRole.GARBAGE_DRIVER,
            driverId = "DRV-412",
            vehicleNumber = "DL-01-GB-7070"
        ),
        User(
            id = "mock-driver-005",
            name = "Kavita Rao (Driver)",
            email = DEMO_DRIVER5_EMAIL,
            phone = "+91 9876543218",
            role = UserRole.GARBAGE_DRIVER,
            driverId = "DRV-520",
            vehicleNumber = "DL-01-GB-8080"
        )
    )

    private val userCredentials = mutableMapOf(
        DEMO_CITIZEN_EMAIL to DEV_DEFAULT_PASSWORD,
        DEMO_CITIZEN2_EMAIL to DEV_DEFAULT_PASSWORD,
        DEMO_CITIZEN3_EMAIL to DEV_DEFAULT_PASSWORD,
        DEMO_OFFICER_EMAIL to DEV_DEFAULT_PASSWORD,
        DEMO_OFFICER2_EMAIL to DEV_DEFAULT_PASSWORD,
        DEMO_DRIVER_EMAIL to DEV_DEFAULT_PASSWORD,
        DEMO_DRIVER2_EMAIL to DEV_DEFAULT_PASSWORD,
        DEMO_DRIVER3_EMAIL to DEV_DEFAULT_PASSWORD,
        DEMO_DRIVER4_EMAIL to DEV_DEFAULT_PASSWORD,
        DEMO_DRIVER5_EMAIL to DEV_DEFAULT_PASSWORD
    )

    fun findUserByEmail(email: String): User? {
        return mockUsers.find { it.email.equals(email.trim(), ignoreCase = true) }
    }

    fun authenticate(email: String, password: String, role: UserRole): User? {
        val cleanEmail = email.trim().lowercase()
        val storedPassword = userCredentials[cleanEmail] ?: return null
        if (storedPassword != password) {
            return null
        }
        val user = findUserByEmail(cleanEmail) ?: return null
        return if (user.role == role) user else null
    }

    fun registerUser(user: User, password: String): User {
        val cleanEmail = user.email.trim().lowercase()
        val existingIndex = mockUsers.indexOfFirst { it.email.equals(cleanEmail, ignoreCase = true) }
        val finalUser = user.copy(
            id = if (user.id.isBlank()) UUID.randomUUID().toString() else user.id,
            email = cleanEmail
        )

        if (existingIndex >= 0) {
            mockUsers[existingIndex] = finalUser
        } else {
            mockUsers.add(finalUser)
        }
        userCredentials[cleanEmail] = password
        return finalUser
    }
}
