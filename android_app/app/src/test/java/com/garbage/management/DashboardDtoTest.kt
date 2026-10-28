package com.garbage.management

import com.garbage.management.data.model.CitizenDashboardDto
import com.garbage.management.data.model.DriverDashboardDto
import com.garbage.management.data.model.OfficerDashboardDto
import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.DriverAvailability
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardDtoTest {

    private val gson = Gson()

    @Test
    fun testCitizenDashboardDtoParsingAndMapping() {
        val json = """
            {
                "citizenId": "CIT-SYN-01",
                "citizenName": "Aarav Sharma",
                "metrics": {
                    "totalComplaints": 4,
                    "activeComplaints": 2,
                    "pendingRequests": 1,
                    "emergencyRequests": 1,
                    "completedComplaints": 2,
                    "rejectedComplaints": 0
                },
                "recentComplaints": [
                    {
                        "id": "CMP-SYN-001",
                        "citizenId": "CIT-SYN-01",
                        "citizenName": "Aarav Sharma",
                        "garbageType": "OVERFLOWING_BIN",
                        "locationText": "Market Road, Sector 4",
                        "status": "SUBMITTED",
                        "emergency": true
                    }
                ],
                "unreadNotificationsCount": 3,
                "mode": "SYNTHETIC_DEMO"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, CitizenDashboardDto::class.java)

        // Verify DTO parsing
        assertEquals("CIT-SYN-01", dto.citizenId)
        assertEquals("Aarav Sharma", dto.citizenName)
        assertEquals(4, dto.metrics?.totalComplaints)
        assertEquals(2, dto.metrics?.activeComplaints)
        assertEquals("SYNTHETIC_DEMO", dto.mode)

        // Verify Domain mapping
        val domain = dto.toDomain()
        assertTrue(domain.isSynthetic)
        assertEquals("CIT-SYN-01", domain.citizenId)
        assertEquals("Aarav Sharma", domain.citizenName)
        assertEquals(4, domain.totalComplaints)
        assertEquals(2, domain.activeComplaints)
        assertEquals(1, domain.pendingRequests)
        assertEquals(1, domain.emergencyRequests)
        assertEquals(2, domain.completedComplaints)
        assertEquals(0, domain.rejectedComplaints)
        assertEquals(3, domain.unreadNotificationsCount)
        assertEquals(1, domain.recentComplaints.size)

        val complaint = domain.recentComplaints[0]
        assertEquals("CMP-SYN-001", complaint.id)
        assertEquals("Aarav Sharma", complaint.citizenName)
        assertEquals(ComplaintStatus.SUBMITTED, complaint.status)
        assertTrue(complaint.isEmergency)
    }

    @Test
    fun testOfficerDashboardDtoParsingAndMapping() {
        val json = """
            {
                "metrics": {
                    "total": 15,
                    "submitted": 5,
                    "assigned": 4,
                    "inProgress": 2,
                    "cleaned": 2,
                    "verified": 1,
                    "rejected": 1,
                    "emergency": 3,
                    "activeComplaints": 11
                },
                "driverAvailability": {
                    "total": 8,
                    "available": 5,
                    "busy": 2,
                    "offline": 1,
                    "drivers": [
                        {
                            "id": "DRV-1",
                            "driverId": "DRV-1",
                            "name": "Rajesh Driver",
                            "vehicleNumber": "DL-01-AB-1234",
                            "vehicleType": "Heavy Truck",
                            "availability": "AVAILABLE"
                        }
                    ]
                },
                "priorityComplaints": [
                    {
                        "id": "CMP-SYN-002",
                        "citizenId": "CIT-SYN-02",
                        "citizenName": "Priya Patel",
                        "garbageType": "ROADSIDE_GARBAGE",
                        "locationText": "Near Metro Gate 2",
                        "status": "ASSIGNED",
                        "emergency": true
                    }
                ],
                "recentComplaints": [],
                "mode": "SYNTHETIC_DEMO"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, OfficerDashboardDto::class.java)

        // Verify DTO parsing
        assertEquals(15, dto.metrics?.total)
        assertEquals(5, dto.metrics?.submitted)
        assertEquals(8, dto.driverAvailability?.total)
        assertEquals(5, dto.driverAvailability?.available)

        // Verify Domain mapping
        val domain = dto.toDomain()
        assertTrue(domain.isSynthetic)
        assertEquals(15, domain.totalComplaints)
        assertEquals(5, domain.submitted)
        assertEquals(4, domain.assigned)
        assertEquals(2, domain.inProgress)
        assertEquals(2, domain.cleaned)
        assertEquals(1, domain.verified)
        assertEquals(1, domain.rejected)
        assertEquals(3, domain.emergency)
        assertEquals(11, domain.activeComplaints)
        assertEquals(5, domain.availableDriversCount)
        assertEquals(8, domain.totalDriversCount)
        assertEquals(1, domain.drivers.size)
        assertEquals("Rajesh Driver", domain.drivers[0].name)
        assertEquals(1, domain.priorityComplaints.size)
        assertEquals("CMP-SYN-002", domain.priorityComplaints[0].id)
    }

    @Test
    fun testDriverDashboardDtoParsingAndMapping() {
        val json = """
            {
                "driverId": "DRV-DEMO-001",
                "driverInfo": {
                    "id": "DRV-DEMO-001",
                    "driverId": "DRV-DEMO-001",
                    "name": "Rajesh Kumar",
                    "vehicleNumber": "DL-01-AB-1234",
                    "vehicleType": "Heavy Compactor Truck",
                    "availability": "AVAILABLE"
                },
                "metrics": {
                    "totalAssignedTasks": 3,
                    "assignedTasks": 1,
                    "inProgressTasks": 1,
                    "completedTasks": 1,
                    "emergencyTasks": 1
                },
                "activeTask": {
                    "id": "CMP-SYN-ACTIVE",
                    "citizenId": "CIT-01",
                    "citizenName": "Test Citizen",
                    "garbageType": "HAZARDOUS_WASTE",
                    "locationText": "Industrial Zone",
                    "status": "PICKUP_IN_PROGRESS",
                    "emergency": true
                },
                "assignedTasks": [
                    {
                        "id": "CMP-SYN-003",
                        "citizenId": "CIT-SYN-03",
                        "citizenName": "Rohan Gupta",
                        "garbageType": "ILLEGAL_DUMPING",
                        "locationText": "Industrial Area, Phase 1",
                        "status": "ASSIGNED",
                        "emergency": false
                    }
                ],
                "collectionHistory": [
                    {
                        "id": "CMP-SYN-004",
                        "citizenId": "CIT-SYN-04",
                        "citizenName": "Sneha Rao",
                        "garbageType": "COMMERCIAL_WASTE",
                        "locationText": "City Center Mall",
                        "status": "CLEANED",
                        "emergency": false
                    }
                ],
                "unreadNotificationsCount": 2,
                "mode": "SYNTHETIC_DEMO"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, DriverDashboardDto::class.java)

        // Verify DTO parsing
        assertEquals("DRV-DEMO-001", dto.driverId)
        assertEquals("Rajesh Kumar", dto.driverInfo?.name)
        assertEquals(3, dto.metrics?.totalAssignedTasks)

        // Verify Domain mapping
        val domain = dto.toDomain()
        assertTrue(domain.isSynthetic)
        assertEquals("DRV-DEMO-001", domain.driverId)
        assertEquals("Rajesh Kumar", domain.driverName)
        assertEquals("DL-01-AB-1234", domain.vehicleNumber)
        assertEquals("Heavy Compactor Truck", domain.vehicleType)
        assertEquals(DriverAvailability.AVAILABLE, domain.availability)

        assertEquals(3, domain.totalAssignedTasks)
        assertEquals(1, domain.assignedTasksCount)
        assertEquals(1, domain.inProgressTasksCount)
        assertEquals(1, domain.completedTasksCount)
        assertEquals(1, domain.emergencyTasksCount)

        assertNotNull(domain.activeTask)
        assertEquals("CMP-SYN-ACTIVE", domain.activeTask?.id)
        assertEquals(ComplaintStatus.PICKUP_IN_PROGRESS, domain.activeTask?.status)

        assertEquals(1, domain.assignedTasks.size)
        assertEquals("CMP-SYN-003", domain.assignedTasks[0].id)
        assertEquals(1, domain.collectionHistory.size)
        assertEquals("CMP-SYN-004", domain.collectionHistory[0].id)
        assertEquals(2, domain.unreadNotificationsCount)
    }

    @Test
    fun testDashboardDtoFallbacksWhenFieldsOmitted() {
        val minimalCitizenJson = """
            {
                "citizenId": "CIT-MIN"
            }
        """.trimIndent()

        val citizenDto = gson.fromJson(minimalCitizenJson, CitizenDashboardDto::class.java)
        val citizenDomain = citizenDto.toDomain()

        assertFalse(citizenDomain.isSynthetic)
        assertEquals("CIT-MIN", citizenDomain.citizenId)
        assertEquals(0, citizenDomain.totalComplaints)
        assertTrue(citizenDomain.recentComplaints.isEmpty())

        val minimalOfficerJson = "{}"
        val officerDto = gson.fromJson(minimalOfficerJson, OfficerDashboardDto::class.java)
        val officerDomain = officerDto.toDomain()

        assertFalse(officerDomain.isSynthetic)
        assertEquals(0, officerDomain.totalComplaints)
        assertEquals(0, officerDomain.availableDriversCount)
        assertTrue(officerDomain.priorityComplaints.isEmpty())

        val minimalDriverJson = "{}"
        val driverDto = gson.fromJson(minimalDriverJson, DriverDashboardDto::class.java)
        val driverDomain = driverDto.toDomain()

        assertFalse(driverDomain.isSynthetic)
        assertEquals("", driverDomain.driverId)
        assertEquals("Driver", driverDomain.driverName)
        assertEquals(DriverAvailability.AVAILABLE, driverDomain.availability)
        assertEquals(0, driverDomain.totalAssignedTasks)
        assertNull(driverDomain.activeTask)
        assertTrue(driverDomain.assignedTasks.isEmpty())
        assertTrue(driverDomain.collectionHistory.isEmpty())
    }
}
