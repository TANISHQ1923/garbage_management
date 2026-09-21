package com.garbage.management.data.model

import com.garbage.management.domain.model.DriverAvailability
import com.garbage.management.domain.model.DriverLocation
import com.garbage.management.domain.model.GarbageDriver
import com.google.gson.annotations.SerializedName

/**
 * Data transfer object representing a Garbage Driver from the backend.
 */
data class DriverDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("driverId")
    val driverId: String? = null,

    @SerializedName("employeeId")
    val employeeId: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("vehicleNumber")
    val vehicleNumber: String? = null,

    @SerializedName("vehicleType")
    val vehicleType: String? = null,

    @SerializedName("availability")
    val availability: String? = null,

    @SerializedName("currentLocation")
    val currentLocation: DriverLocationDto? = null
) {
    fun toDomain(): GarbageDriver {
        val mappedAvailability = when (availability?.uppercase()) {
            "BUSY" -> DriverAvailability.BUSY
            "OFFLINE" -> DriverAvailability.OFFLINE
            else -> DriverAvailability.AVAILABLE
        }

        return GarbageDriver(
            id = driverId ?: id ?: "",
            name = name ?: "Driver",
            phone = phone ?: "",
            vehicleNumber = vehicleNumber ?: "DL-01-GB-XXXX",
            isAvailable = mappedAvailability == DriverAvailability.AVAILABLE,
            availability = mappedAvailability,
            vehicleType = vehicleType ?: "Heavy Compactor Truck",
            currentLocationDescription = if (currentLocation != null) {
                "Lat: ${"%.4f".format(currentLocation.latitude)}, Lng: ${"%.4f".format(currentLocation.longitude)}"
            } else {
                "Ward Depot"
            }
        )
    }
}

data class DriverLocationDto(
    @SerializedName("driverId")
    val driverId: String? = null,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("timestamp")
    val timestamp: Long? = null
) {
    fun toDomain(fallbackDriverId: String = ""): DriverLocation {
        return DriverLocation(
            driverId = driverId ?: fallbackDriverId,
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp ?: System.currentTimeMillis()
        )
    }
}

data class UpdateLocationRequestDto(
    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double
)
