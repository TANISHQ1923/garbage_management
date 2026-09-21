package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.DriverLocation
import com.garbage.management.domain.repository.DriverLocationRepository

/**
 * Use case to persist the driver's current device coordinates locally.
 */
class UpdateDriverLocationUseCase(
    private val repository: DriverLocationRepository
) {
    suspend operator fun invoke(driverId: String, latitude: Double, longitude: Double) {
        repository.updateDriverLocation(
            DriverLocation(
                driverId = driverId,
                latitude = latitude,
                longitude = longitude,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
