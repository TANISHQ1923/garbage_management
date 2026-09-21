package com.garbage.management.domain.model

/**
 * Domain entity representing the local development GPS coordinates of a garbage collection driver.
 */
data class DriverLocation(
    val driverId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)
