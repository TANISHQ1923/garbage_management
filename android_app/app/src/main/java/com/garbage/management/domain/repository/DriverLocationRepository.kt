package com.garbage.management.domain.repository

import com.garbage.management.domain.model.DriverLocation
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing driver GPS location coordinates.
 */
interface DriverLocationRepository {
    fun getDriverLocation(driverId: String): Flow<DriverLocation?>
    suspend fun updateDriverLocation(location: DriverLocation)
}
