package com.garbage.management

import com.garbage.management.data.model.ComplaintDto
import com.garbage.management.data.model.DispatchResultDto
import com.garbage.management.data.model.ApiResponseDto
import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.DispatchResult
import com.garbage.management.domain.model.GarbageType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DispatchResultTest {

    private val gson = Gson()

    @Test
    fun testDispatchResultDtoToDomainMapping_assigned() {
        val dto = DispatchResultDto(
            assigned = true,
            assignmentStatus = "ASSIGNED",
            assignedDriverId = "DRV-DEMO-003",
            assignedDriverName = "Amitabh Yadav",
            assignedVehicleNumber = "MH-DEMO-003",
            distanceKm = 1.45,
            message = "Complaint submitted and automatically assigned to nearby driver Amitabh Yadav (1.45 km)."
        )

        val domain: DispatchResult = dto.toDomain()

        assertEquals("ASSIGNED", domain.assignmentStatus)
        assertEquals("DRV-DEMO-003", domain.assignedDriverId)
        assertEquals("Amitabh Yadav", domain.assignedDriverName)
        assertEquals("MH-DEMO-003", domain.assignedVehicleNumber)
        assertEquals(1.45, domain.distanceKm!!, 0.001)
        assertTrue(domain.message!!.contains("Amitabh Yadav"))
    }

    @Test
    fun testDispatchResultDtoToDomainMapping_pendingDriver() {
        val dto = DispatchResultDto(
            assigned = false,
            assignmentStatus = "PENDING_DRIVER",
            assignedDriverId = null,
            assignedDriverName = null,
            assignedVehicleNumber = null,
            distanceKm = null,
            message = "No available drivers nearby. Queued in priority queue."
        )

        val domain: DispatchResult = dto.toDomain()

        assertEquals("PENDING_DRIVER", domain.assignmentStatus)
        assertNull(domain.assignedDriverId)
        assertNull(domain.assignedDriverName)
        assertNull(domain.assignedVehicleNumber)
        assertNull(domain.distanceKm)
        assertTrue(domain.message!!.contains("No available drivers"))
    }

    @Test
    fun testComplaintDtoWithDispatchResultToDomain() {
        val json = """
            {
                "complaintId": "SGM-DISPATCH-001",
                "citizenId": "65f100000000000000000001",
                "citizenName": "Aarav Sharma",
                "garbageType": "OVERFLOWING_BIN",
                "status": "ASSIGNED",
                "assignedDriverId": "DRV-DEMO-003",
                "assignedDriverName": "Amitabh Yadav",
                "assignedVehicleNumber": "MH-DEMO-003",
                "latitude": 28.6139,
                "longitude": 77.2090,
                "locationText": "Sector 4 Community Park",
                "emergency": false,
                "priorityScore": 75,
                "priorityLevel": "HIGH",
                "dispatchResult": {
                    "assigned": true,
                    "assignmentStatus": "ASSIGNED",
                    "assignedDriverId": "DRV-DEMO-003",
                    "assignedDriverName": "Amitabh Yadav",
                    "assignedVehicleNumber": "MH-DEMO-003",
                    "distanceKm": 0.82,
                    "message": "Assigned to Amitabh Yadav"
                }
            }
        """.trimIndent()

        val complaintDto = gson.fromJson(json, ComplaintDto::class.java)
        val domain = complaintDto.toDomain()

        assertEquals("SGM-DISPATCH-001", domain.id)
        assertEquals(ComplaintStatus.ASSIGNED, domain.status)
        assertEquals("Amitabh Yadav", domain.assignedDriverName)
        assertEquals("MH-DEMO-003", domain.assignedVehicleNumber)
        assertNotNull(domain.dispatchResult)
        assertEquals("ASSIGNED", domain.dispatchResult?.assignmentStatus)
        assertEquals(0.82, domain.dispatchResult?.distanceKm!!, 0.001)
    }

    @Test
    fun testApiResponseDtoWithDispatchResultDeserialization() {
        val json = """
            {
                "success": true,
                "message": "Complaint submitted and automatically assigned to nearby driver Amitabh Yadav.",
                "data": {
                    "complaintId": "SGM-TEST-202",
                    "status": "ASSIGNED",
                    "garbageType": "ROADSIDE_GARBAGE"
                },
                "dispatchResult": {
                    "assigned": true,
                    "assignmentStatus": "ASSIGNED",
                    "assignedDriverId": "DRV-DEMO-003",
                    "assignedDriverName": "Amitabh Yadav",
                    "distanceKm": 1.2
                }
            }
        """.trimIndent()

        val type = object : TypeToken<ApiResponseDto<ComplaintDto>>() {}.type
        val response: ApiResponseDto<ComplaintDto> = gson.fromJson(json, type)

        assertTrue(response.success)
        assertNotNull(response.data)
        assertEquals("SGM-TEST-202", response.data?.complaintId)
        assertNotNull(response.dispatchResult)
        assertEquals("ASSIGNED", response.dispatchResult?.assignmentStatus)
        assertEquals("Amitabh Yadav", response.dispatchResult?.assignedDriverName)
        assertEquals(1.2, response.dispatchResult?.distanceKm!!, 0.001)
    }

    @Test
    fun testDistinguishesAssignedVersusPendingState() {
        val assignedComplaint = ComplaintDto(
            complaintId = "CMP-A",
            status = "ASSIGNED",
            assignedDriverName = "Manoj Kale",
            assignedVehicleNumber = "MH-DEMO-006",
            dispatchResult = DispatchResultDto(
                assigned = true,
                assignmentStatus = "ASSIGNED",
                assignedDriverId = "DRV-DEMO-006",
                assignedDriverName = "Manoj Kale"
            )
        ).toDomain()

        val pendingComplaint = ComplaintDto(
            complaintId = "CMP-B",
            status = "SUBMITTED",
            assignedDriverName = null,
            dispatchResult = DispatchResultDto(
                assigned = false,
                assignmentStatus = "PENDING_DRIVER"
            )
        ).toDomain()

        // Assigned state assertions
        assertEquals(ComplaintStatus.ASSIGNED, assignedComplaint.status)
        assertNotNull(assignedComplaint.assignedDriverName)
        assertEquals("ASSIGNED", assignedComplaint.dispatchResult?.assignmentStatus)

        // Pending state assertions
        assertEquals(ComplaintStatus.SUBMITTED, pendingComplaint.status)
        assertNull(pendingComplaint.assignedDriverName)
        assertEquals("PENDING_DRIVER", pendingComplaint.dispatchResult?.assignmentStatus)
    }
}
