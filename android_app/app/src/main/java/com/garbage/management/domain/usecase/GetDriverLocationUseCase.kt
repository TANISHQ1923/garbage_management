package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.DriverLocation
import com.garbage.management.domain.repository.DriverLocationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to observe the driver's current locally saved GPS coordinates.
 */
class GetDriverLocationUseCase(
    private val repository: DriverLocationRepository
) {
    operator fun invoke(driverId: String): Flow<DriverLocation?> {
        return repository.getDriverLocation(driverId)
    }
}
