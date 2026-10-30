package com.garbage.management

import com.garbage.management.data.model.ComplaintDto
import com.garbage.management.domain.model.SmartBinStatus
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class SmartBinDtoTest {
    @Test fun sensorTaskNeedsNoCitizenAndKeepsEvidenceSeparate() {
        val task = Gson().fromJson("""{"complaintId":"BIN-TASK","source":"SENSOR","binId":"BIN1","status":"ASSIGNED","sensorEmptyConfirmedAt":1234,"latitude":18.5,"longitude":73.8}""", ComplaintDto::class.java).toDomain()
        assertEquals("SENSOR", task.source)
        assertEquals("BIN1", task.binId)
        assertEquals("", task.citizenId)
        assertEquals("Smart bin", task.citizenName)
        assertEquals(1234L, task.sensorEmptyConfirmedAt)
        assertEquals("ASSIGNED", task.status.name)
    }

    @Test fun staleBinKeepsLastKnownFillAndPendingReason() {
        val bin = Gson().fromJson("""{"binId":"BIN1","label":"Test","fillPercent":82.5,"health":"STALE","dispatchState":"PENDING_DRIVER","dispatchReason":"NO_AVAILABLE_DRIVER","lastMeasurementAt":1000}""", SmartBinStatus::class.java)
        assertEquals(82.5, bin.fillPercent!!, 0.001)
        assertEquals("STALE", bin.health)
        assertEquals("NO_AVAILABLE_DRIVER", bin.dispatchReason)
    }
}
