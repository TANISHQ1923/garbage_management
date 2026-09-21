package com.garbage.management.data.local

import com.garbage.management.domain.model.DriverLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Thread-safe in-memory data source for driver GPS coordinates.
 * Simulates local device location cache for development testing.
 */
class LocalDriverLocationDataSource {

    private val locationsFlow = MutableStateFlow<Map<String, DriverLocation>>(
        mapOf(
            "DRV-882" to DriverLocation(
                driverId = "DRV-882",
                latitude = 28.6139,
                longitude = 77.2090,
                timestamp = System.currentTimeMillis() - 1800000 // 30 minutes ago
            )
        )
    )

    fun getDriverLocation(driverId: String): Flow<DriverLocation?> {
        return locationsFlow.map { map ->
            map[driverId] ?: map.values.find { it.driverId.equals(driverId, ignoreCase = true) }
        }
    }

    fun updateDriverLocation(location: DriverLocation) {
        locationsFlow.update { currentMap ->
            currentMap + (location.driverId to location)
        }
    }
}
