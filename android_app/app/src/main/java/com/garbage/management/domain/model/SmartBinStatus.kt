package com.garbage.management.domain.model

data class SmartBinStatus(
    val binId: String = "", val label: String = "", val locationText: String = "",
    val fillPercent: Double? = null, val health: String = "OFFLINE", val readingReason: String? = null,
    val lastMeasurementAt: Long? = null, val lastContactAt: Long? = null, val lastValidAt: Long? = null,
    val sensorEmptyConfirmedAt: Long? = null, val pickupTaskId: String? = null,
    val pickupStatus: String? = null, val dispatchState: String? = null, val dispatchReason: String? = null,
    val nextRetryAt: Long? = null, val assignedDriverName: String? = null
)
