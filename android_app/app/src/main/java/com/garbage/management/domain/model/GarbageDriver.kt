package com.garbage.management.domain.model

enum class DriverAvailability(val displayName: String) {
    AVAILABLE("Available"),
    BUSY("Busy"),
    OFFLINE("Offline")
}

/**
 * Domain entity representing a municipal waste collection vehicle driver.
 * Used for local assignment and dispatch tracking.
 */
data class GarbageDriver(
    val id: String,
    val name: String,
    val phone: String,
    val vehicleNumber: String,
    val isAvailable: Boolean = true,
    val availability: DriverAvailability = if (isAvailable) DriverAvailability.AVAILABLE else DriverAvailability.BUSY,
    val vehicleType: String = "Heavy Compactor Truck",
    val currentLocationDescription: String = "Ward Depot"
)
