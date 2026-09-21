package com.garbage.management.data.local

import com.garbage.management.domain.model.DriverAvailability
import com.garbage.management.domain.model.GarbageDriver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Local thread-safe in-memory data source representing municipal collection drivers.
 * Holds development driver records for assignment and vehicle fleet management.
 */
class LocalDriverDataSource {

    private val driversFlow = MutableStateFlow<List<GarbageDriver>>(
        listOf(
            GarbageDriver(
                id = "DRV-882",
                name = "Ramesh Kumar",
                phone = "+91 9876543212",
                vehicleNumber = "DL-01-GB-4040",
                isAvailable = true,
                availability = DriverAvailability.AVAILABLE,
                vehicleType = "Heavy Hydraulic Compactor",
                currentLocationDescription = "Ward 12 Main Depot"
            ),
            GarbageDriver(
                id = "DRV-001",
                name = "Suresh Verma",
                phone = "+91 9876500001",
                vehicleNumber = "MH-12-GC-1001",
                isAvailable = true,
                availability = DriverAvailability.AVAILABLE,
                vehicleType = "Medium Tipper Dumper",
                currentLocationDescription = "Sector 4 Civic Depot"
            ),
            GarbageDriver(
                id = "DRV-002",
                name = "Anita Yadav",
                phone = "+91 9876500002",
                vehicleNumber = "MH-12-GC-1002",
                isAvailable = true,
                availability = DriverAvailability.AVAILABLE,
                vehicleType = "Mini Collection Van",
                currentLocationDescription = "North Zone Transfer Station"
            ),
            GarbageDriver(
                id = "DRV-003",
                name = "Imran Khan",
                phone = "+91 9876500003",
                vehicleNumber = "MH-12-GC-1003",
                isAvailable = false,
                availability = DriverAvailability.BUSY,
                vehicleType = "Heavy Hydraulic Compactor",
                currentLocationDescription = "Central Market Hub (On Route)"
            ),
            GarbageDriver(
                id = "DRV-004",
                name = "Deepak Joshi",
                phone = "+91 9876500004",
                vehicleNumber = "MH-12-GC-1004",
                isAvailable = true,
                availability = DriverAvailability.AVAILABLE,
                vehicleType = "Medium Tipper Dumper",
                currentLocationDescription = "South Ward Recycling Station"
            )
        )
    )

    fun getAllDrivers(): Flow<List<GarbageDriver>> {
        return driversFlow.asStateFlow()
    }

    fun getDriverById(driverId: String): GarbageDriver? {
        return driversFlow.value.find { it.id.equals(driverId, ignoreCase = true) }
    }

    fun setDriverAvailability(driverId: String, isAvailable: Boolean) {
        val targetAvailability = if (isAvailable) DriverAvailability.AVAILABLE else DriverAvailability.BUSY
        setDriverAvailability(driverId, targetAvailability)
    }

    fun setDriverAvailability(driverId: String, availability: DriverAvailability) {
        driversFlow.update { currentList ->
            currentList.map { driver ->
                if (driver.id.equals(driverId, ignoreCase = true)) {
                    driver.copy(
                        isAvailable = (availability == DriverAvailability.AVAILABLE),
                        availability = availability
                    )
                } else {
                    driver
                }
            }
        }
    }
}
