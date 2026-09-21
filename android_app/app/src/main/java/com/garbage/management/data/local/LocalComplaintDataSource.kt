package com.garbage.management.data.local

import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.ComplaintStatusHistory
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.model.GarbageDriver
import com.garbage.management.domain.model.GarbageType
import com.garbage.management.domain.model.OfficerNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory thread-safe local data source for garbage complaints.
 * Preloaded with realistic initial complaints representing diverse municipal operational states.
 */
class LocalComplaintDataSource {

    private val complaintsFlow = MutableStateFlow<List<GarbageComplaint>>(
        listOf(
            GarbageComplaint(
                id = "SGM-2026-004510",
                citizenId = "mock-citizen-002",
                citizenName = "Priya Sen (Citizen)",
                citizenPhone = "+91 9876543299",
                garbageType = GarbageType.OTHER_WASTE,
                description = "Chemical containers and broken medical glass dumped behind clinic lane. Poses immediate health hazard.",
                imageUri = null,
                videoUri = null,
                latitude = 28.6180,
                longitude = 77.2140,
                locationDescription = "Hospital Lane near Block B Clinic, Ward 12",
                isEmergency = true,
                status = ComplaintStatus.SUBMITTED,
                statusHistory = listOf(
                    ComplaintStatusHistory(
                        status = ComplaintStatus.SUBMITTED,
                        timestamp = System.currentTimeMillis() - 3600000 * 2,
                        note = "Emergency hazardous waste report logged by citizen"
                    )
                ),
                createdAt = System.currentTimeMillis() - 3600000 * 2, // 2 hours ago
                updatedAt = System.currentTimeMillis() - 3600000 * 2
            ),
            GarbageComplaint(
                id = "SGM-2026-004128",
                citizenId = "mock-citizen-001",
                citizenName = "Aarav Sharma (Citizen)",
                citizenPhone = "+91 9876543210",
                garbageType = GarbageType.OVERFLOWING_BIN,
                description = "Public bin near community park entrance has been overflowing for 2 days. Animals are scattering waste.",
                imageUri = null,
                videoUri = null,
                latitude = 28.6139,
                longitude = 77.2090,
                locationDescription = "Sector 4 Community Park Gate 2, Green Park",
                isEmergency = false,
                status = ComplaintStatus.SUBMITTED,
                statusHistory = listOf(
                    ComplaintStatusHistory(
                        status = ComplaintStatus.SUBMITTED,
                        timestamp = System.currentTimeMillis() - 3600000 * 5,
                        note = "Garbage report submitted successfully"
                    )
                ),
                createdAt = System.currentTimeMillis() - 3600000 * 5, // 5 hours ago
                updatedAt = System.currentTimeMillis() - 3600000 * 5
            ),
            GarbageComplaint(
                id = "SGM-2026-003890",
                citizenId = "mock-citizen-001",
                citizenName = "Aarav Sharma (Citizen)",
                citizenPhone = "+91 9876543210",
                garbageType = GarbageType.FESTIVAL_WASTE,
                description = "Large volume of celebration packaging, plastic cups, and decorations left on the public ground.",
                imageUri = null,
                videoUri = null,
                latitude = 28.6152,
                longitude = 77.2105,
                locationDescription = "Market Square Main Junction, Smart City",
                isEmergency = true,
                status = ComplaintStatus.ASSIGNED,
                assignedDriverId = "DRV-882",
                assignedDriverName = "Ramesh Kumar",
                assignedVehicleNumber = "DL-01-GB-4040",
                assignedAt = System.currentTimeMillis() - 86400000 * 1 + 3600000,
                statusHistory = listOf(
                    ComplaintStatusHistory(
                        status = ComplaintStatus.SUBMITTED,
                        timestamp = System.currentTimeMillis() - 86400000 * 1,
                        note = "Emergency garbage report submitted successfully"
                    ),
                    ComplaintStatusHistory(
                        status = ComplaintStatus.ASSIGNED,
                        timestamp = System.currentTimeMillis() - 86400000 * 1 + 3600000,
                        note = "Assigned to driver Ramesh Kumar (DL-01-GB-4040)"
                    )
                ),
                officerNotes = listOf(
                    OfficerNote(
                        id = "NOTE-001",
                        officerId = "mock-officer-001",
                        officerName = "Vikram Patel (Officer)",
                        text = "High pedestrian zone; scheduled early morning collection.",
                        createdAt = System.currentTimeMillis() - 86400000 * 1 + 3600000
                    )
                ),
                createdAt = System.currentTimeMillis() - 86400000 * 1, // 1 day ago
                updatedAt = System.currentTimeMillis() - 86400000 * 1
            ),
            GarbageComplaint(
                id = "SGM-2026-003500",
                citizenId = "mock-citizen-003",
                citizenName = "Rajesh Gupta",
                citizenPhone = "+91 9876543233",
                garbageType = GarbageType.FLOOD_WASTE,
                description = "Carcass reported under highway flyover. Needs prompt municipal disposal team.",
                imageUri = null,
                videoUri = null,
                latitude = 28.6090,
                longitude = 77.2010,
                locationDescription = "Ring Road underpass near flyover pillar 18",
                isEmergency = true,
                status = ComplaintStatus.PICKUP_IN_PROGRESS,
                assignedDriverId = "DRV-002",
                assignedDriverName = "Anita Yadav",
                assignedVehicleNumber = "MH-12-GC-1002",
                assignedAt = System.currentTimeMillis() - 3600000 * 6,
                statusHistory = listOf(
                    ComplaintStatusHistory(
                        status = ComplaintStatus.SUBMITTED,
                        timestamp = System.currentTimeMillis() - 3600000 * 8,
                        note = "Complaint submitted"
                    ),
                    ComplaintStatusHistory(
                        status = ComplaintStatus.ASSIGNED,
                        timestamp = System.currentTimeMillis() - 3600000 * 6,
                        note = "Assigned to Anita Yadav (MH-12-GC-1002)"
                    ),
                    ComplaintStatusHistory(
                        status = ComplaintStatus.PICKUP_IN_PROGRESS,
                        timestamp = System.currentTimeMillis() - 3600000 * 1,
                        note = "Collection vehicle arrived at location; operation underway."
                    )
                ),
                createdAt = System.currentTimeMillis() - 3600000 * 8,
                updatedAt = System.currentTimeMillis() - 3600000 * 1
            ),
            GarbageComplaint(
                id = "SGM-2026-002145",
                citizenId = "mock-citizen-001",
                citizenName = "Aarav Sharma (Citizen)",
                citizenPhone = "+91 9876543210",
                garbageType = GarbageType.ROADSIDE_GARBAGE,
                description = "Construction debris and discarded dry waste dumped along road boundary.",
                imageUri = null,
                videoUri = null,
                latitude = 28.6110,
                longitude = 77.2045,
                locationDescription = "North Avenue crossroad near Metro Pillar 42",
                isEmergency = false,
                status = ComplaintStatus.CLEANED,
                assignedDriverId = "DRV-001",
                assignedDriverName = "Suresh Verma",
                assignedVehicleNumber = "MH-12-GC-1001",
                assignedAt = System.currentTimeMillis() - 86400000 * 2,
                statusHistory = listOf(
                    ComplaintStatusHistory(
                        status = ComplaintStatus.SUBMITTED,
                        timestamp = System.currentTimeMillis() - 86400000 * 3,
                        note = "Garbage report submitted successfully"
                    ),
                    ComplaintStatusHistory(
                        status = ComplaintStatus.ASSIGNED,
                        timestamp = System.currentTimeMillis() - 86400000 * 2,
                        note = "Pickup vehicle assigned"
                    ),
                    ComplaintStatusHistory(
                        status = ComplaintStatus.CLEANED,
                        timestamp = System.currentTimeMillis() - 86400000 * 1,
                        note = "Collection completed and site cleared"
                    )
                ),
                createdAt = System.currentTimeMillis() - 86400000 * 3, // 3 days ago
                updatedAt = System.currentTimeMillis() - 86400000 * 1
            ),
            GarbageComplaint(
                id = "SGM-2026-001920",
                citizenId = "mock-citizen-001",
                citizenName = "Aarav Sharma (Citizen)",
                citizenPhone = "+91 9876543210",
                garbageType = GarbageType.EVENT_WASTE,
                description = "Old CRT monitors and discarded computer cabinets cleared from corner pavement.",
                imageUri = null,
                videoUri = null,
                latitude = 28.6175,
                longitude = 77.2180,
                locationDescription = "Tech Enclave Service Road Gate 1",
                isEmergency = false,
                status = ComplaintStatus.VERIFIED,
                assignedDriverId = "DRV-882",
                assignedDriverName = "Ramesh Kumar",
                assignedVehicleNumber = "DL-01-GB-4040",
                assignedAt = System.currentTimeMillis() - 86400000 * 5,
                statusHistory = listOf(
                    ComplaintStatusHistory(
                        status = ComplaintStatus.SUBMITTED,
                        timestamp = System.currentTimeMillis() - 86400000 * 6,
                        note = "Garbage report submitted"
                    ),
                    ComplaintStatusHistory(
                        status = ComplaintStatus.ASSIGNED,
                        timestamp = System.currentTimeMillis() - 86400000 * 5,
                        note = "Assigned to driver Ramesh Kumar"
                    ),
                    ComplaintStatusHistory(
                        status = ComplaintStatus.PICKUP_IN_PROGRESS,
                        timestamp = System.currentTimeMillis() - 86400000 * 4,
                        note = "Vehicle on site for e-waste pickup"
                    ),
                    ComplaintStatusHistory(
                        status = ComplaintStatus.CLEANED,
                        timestamp = System.currentTimeMillis() - 86400000 * 3,
                        note = "E-waste dispatched to recycling facility"
                    ),
                    ComplaintStatusHistory(
                        status = ComplaintStatus.VERIFIED,
                        timestamp = System.currentTimeMillis() - 86400000 * 2,
                        note = "Area inspection verified by Officer Vikram Patel"
                    )
                ),
                createdAt = System.currentTimeMillis() - 86400000 * 6,
                updatedAt = System.currentTimeMillis() - 86400000 * 2
            ),
            GarbageComplaint(
                id = "SGM-2026-004620",
                citizenId = "mock-citizen-001",
                citizenName = "Aarav Sharma (Citizen)",
                citizenPhone = "+91 9876543210",
                garbageType = GarbageType.OVERFLOWING_BIN,
                description = "Secondary community garbage bin near Sector 4 primary school gate is overflowing.",
                imageUri = null,
                videoUri = null,
                latitude = 28.6145,
                longitude = 77.2085,
                locationDescription = "Sector 4 Primary School Gate, Green Park",
                isEmergency = false,
                status = ComplaintStatus.ASSIGNED,
                assignedDriverId = "DRV-882",
                assignedDriverName = "Ramesh Kumar",
                assignedVehicleNumber = "DL-01-GB-4040",
                assignedAt = System.currentTimeMillis() - 3600000 * 3,
                statusHistory = listOf(
                    ComplaintStatusHistory(
                        status = ComplaintStatus.SUBMITTED,
                        timestamp = System.currentTimeMillis() - 3600000 * 4,
                        note = "Complaint reported by citizen"
                    ),
                    ComplaintStatusHistory(
                        status = ComplaintStatus.ASSIGNED,
                        timestamp = System.currentTimeMillis() - 3600000 * 3,
                        note = "Assigned to driver Ramesh Kumar (DL-01-GB-4040)"
                    )
                ),
                createdAt = System.currentTimeMillis() - 3600000 * 4,
                updatedAt = System.currentTimeMillis() - 3600000 * 3
            ),
            GarbageComplaint(
                id = "SGM-2026-004310",
                citizenId = "mock-citizen-002",
                citizenName = "Priya Sen (Citizen)",
                citizenPhone = "+91 9876543299",
                garbageType = GarbageType.ROADSIDE_GARBAGE,
                description = "Fallen tree branches and household waste cleared from alleyway.",
                imageUri = null,
                videoUri = null,
                latitude = 28.6160,
                longitude = 77.2120,
                locationDescription = "Clinic Lane East Exit, Ward 12",
                isEmergency = false,
                status = ComplaintStatus.CLEANED,
                assignedDriverId = "DRV-882",
                assignedDriverName = "Ramesh Kumar",
                assignedVehicleNumber = "DL-01-GB-4040",
                assignedAt = System.currentTimeMillis() - 3600000 * 6,
                statusHistory = listOf(
                    ComplaintStatusHistory(
                        status = ComplaintStatus.SUBMITTED,
                        timestamp = System.currentTimeMillis() - 3600000 * 8,
                        note = "Complaint logged"
                    ),
                    ComplaintStatusHistory(
                        status = ComplaintStatus.ASSIGNED,
                        timestamp = System.currentTimeMillis() - 3600000 * 6,
                        note = "Assigned to driver Ramesh Kumar"
                    ),
                    ComplaintStatusHistory(
                        status = ComplaintStatus.PICKUP_IN_PROGRESS,
                        timestamp = System.currentTimeMillis() - 3600000 * 4,
                        note = "Driver started garbage collection."
                    ),
                    ComplaintStatusHistory(
                        status = ComplaintStatus.CLEANED,
                        timestamp = System.currentTimeMillis() - 3600000 * 2,
                        note = "Driver marked garbage collection as completed."
                    )
                ),
                createdAt = System.currentTimeMillis() - 3600000 * 8,
                updatedAt = System.currentTimeMillis() - 3600000 * 2
            )
        )
    )

    fun addComplaint(complaint: GarbageComplaint) {
        complaintsFlow.update { current ->
            listOf(complaint) + current
        }
    }

    fun updateComplaint(updated: GarbageComplaint) {
        complaintsFlow.update { currentList ->
            currentList.map { complaint ->
                if (complaint.id.equals(updated.id, ignoreCase = true)) {
                    updated
                } else {
                    complaint
                }
            }
        }
    }

    fun getCitizenComplaints(citizenId: String): Flow<List<GarbageComplaint>> {
        return complaintsFlow.map { list ->
            list.filter { it.citizenId.equals(citizenId, ignoreCase = true) }
        }
    }

    fun getComplaintById(complaintId: String): GarbageComplaint? {
        return complaintsFlow.value.find { it.id.equals(complaintId, ignoreCase = true) }
    }

    fun getAllComplaints(): Flow<List<GarbageComplaint>> {
        return complaintsFlow.asStateFlow()
    }

    fun assignDriver(
        complaintId: String,
        driver: GarbageDriver,
        officerId: String,
        officerName: String,
        noteText: String? = null
    ): GarbageComplaint? {
        var updatedComplaint: GarbageComplaint? = null
        complaintsFlow.update { currentList ->
            currentList.map { complaint ->
                if (complaint.id.equals(complaintId, ignoreCase = true)) {
                    val now = System.currentTimeMillis()
                    val newHistory = complaint.statusHistory + ComplaintStatusHistory(
                        status = ComplaintStatus.ASSIGNED,
                        timestamp = now,
                        note = noteText ?: "Assigned to driver ${driver.name} (${driver.vehicleNumber}) by Officer $officerName"
                    )
                    val newNotes = if (!noteText.isNullOrBlank()) {
                        complaint.officerNotes + OfficerNote(
                            id = "NOTE-" + System.currentTimeMillis(),
                            officerId = officerId,
                            officerName = officerName,
                            text = noteText,
                            createdAt = now
                        )
                    } else {
                        complaint.officerNotes
                    }

                    val updated = complaint.copy(
                        status = ComplaintStatus.ASSIGNED,
                        assignedDriverId = driver.id,
                        assignedDriverName = driver.name,
                        assignedVehicleNumber = driver.vehicleNumber,
                        assignedAt = now,
                        statusHistory = newHistory,
                        officerNotes = newNotes,
                        updatedAt = now
                    )
                    updatedComplaint = updated
                    updated
                } else {
                    complaint
                }
            }
        }
        return updatedComplaint
    }

    fun updateComplaintStatus(
        complaintId: String,
        newStatus: ComplaintStatus,
        officerId: String,
        officerName: String,
        noteText: String? = null
    ): GarbageComplaint? {
        var updatedComplaint: GarbageComplaint? = null
        complaintsFlow.update { currentList ->
            currentList.map { complaint ->
                if (complaint.id.equals(complaintId, ignoreCase = true)) {
                    val now = System.currentTimeMillis()
                    val defaultNote = when (newStatus) {
                        ComplaintStatus.ASSIGNED -> "Vehicle assigned by Officer $officerName"
                        ComplaintStatus.PICKUP_IN_PROGRESS -> "Collection team is actively clearing waste at location."
                        ComplaintStatus.CLEANED -> "Waste collected and site cleaned."
                        ComplaintStatus.VERIFIED -> "Cleanup verified and completed by Officer $officerName."
                        ComplaintStatus.REJECTED -> "Complaint reviewed and rejected by Officer $officerName."
                        ComplaintStatus.SUBMITTED -> "Status updated to submitted."
                    }
                    val historyNote = noteText?.takeIf { it.isNotBlank() } ?: defaultNote

                    val newHistory = complaint.statusHistory + ComplaintStatusHistory(
                        status = newStatus,
                        timestamp = now,
                        note = historyNote
                    )

                    val newNotes = if (!noteText.isNullOrBlank()) {
                        complaint.officerNotes + OfficerNote(
                            id = "NOTE-" + System.currentTimeMillis(),
                            officerId = officerId,
                            officerName = officerName,
                            text = noteText,
                            createdAt = now
                        )
                    } else {
                        complaint.officerNotes
                    }

                    val updated = complaint.copy(
                        status = newStatus,
                        statusHistory = newHistory,
                        officerNotes = newNotes,
                        updatedAt = now
                    )
                    updatedComplaint = updated
                    updated
                } else {
                    complaint
                }
            }
        }
        return updatedComplaint
    }

    fun addOfficerNote(complaintId: String, note: OfficerNote): GarbageComplaint? {
        var updatedComplaint: GarbageComplaint? = null
        complaintsFlow.update { currentList ->
            currentList.map { complaint ->
                if (complaint.id.equals(complaintId, ignoreCase = true)) {
                    val updated = complaint.copy(
                        officerNotes = complaint.officerNotes + note,
                        updatedAt = System.currentTimeMillis()
                    )
                    updatedComplaint = updated
                    updated
                } else {
                    complaint
                }
            }
        }
        return updatedComplaint
    }

    fun getDriverComplaints(driverId: String): Flow<List<GarbageComplaint>> {
        return complaintsFlow.map { list ->
            list.filter {
                it.assignedDriverId != null &&
                        (it.assignedDriverId.equals(driverId, ignoreCase = true) ||
                                (driverId.equals("mock-driver-001", ignoreCase = true) && it.assignedDriverId.equals("DRV-882", ignoreCase = true)))
            }
        }
    }

    fun startCollection(
        complaintId: String,
        driverId: String,
        driverName: String,
        beforeCleaningUri: String? = null
    ): GarbageComplaint? {
        var updatedComplaint: GarbageComplaint? = null
        complaintsFlow.update { currentList ->
            currentList.map { complaint ->
                if (complaint.id.equals(complaintId, ignoreCase = true)) {
                    val now = System.currentTimeMillis()
                    val newHistory = complaint.statusHistory + ComplaintStatusHistory(
                        status = ComplaintStatus.PICKUP_IN_PROGRESS,
                        timestamp = now,
                        note = "Driver $driverName started garbage collection."
                    )
                    val updated = complaint.copy(
                        status = ComplaintStatus.PICKUP_IN_PROGRESS,
                        beforeCleaningImageUri = beforeCleaningUri ?: complaint.beforeCleaningImageUri,
                        statusHistory = newHistory,
                        updatedAt = now
                    )
                    updatedComplaint = updated
                    updated
                } else {
                    complaint
                }
            }
        }
        return updatedComplaint
    }

    fun updateCollectionEvidence(
        complaintId: String,
        beforeCleaningUri: String? = null,
        afterCleaningUri: String? = null
    ): GarbageComplaint? {
        var updatedComplaint: GarbageComplaint? = null
        complaintsFlow.update { currentList ->
            currentList.map { complaint ->
                if (complaint.id.equals(complaintId, ignoreCase = true)) {
                    val updated = complaint.copy(
                        beforeCleaningImageUri = beforeCleaningUri ?: complaint.beforeCleaningImageUri,
                        afterCleaningImageUri = afterCleaningUri ?: complaint.afterCleaningImageUri,
                        updatedAt = System.currentTimeMillis()
                    )
                    updatedComplaint = updated
                    updated
                } else {
                    complaint
                }
            }
        }
        return updatedComplaint
    }

    fun completeCollection(
        complaintId: String,
        driverId: String,
        driverName: String,
        afterCleaningUri: String,
        beforeCleaningUri: String? = null
    ): GarbageComplaint? {
        var updatedComplaint: GarbageComplaint? = null
        complaintsFlow.update { currentList ->
            currentList.map { complaint ->
                if (complaint.id.equals(complaintId, ignoreCase = true)) {
                    val now = System.currentTimeMillis()
                    val newHistory = complaint.statusHistory + ComplaintStatusHistory(
                        status = ComplaintStatus.CLEANED,
                        timestamp = now,
                        note = "Driver marked garbage collection as completed."
                    )
                    val updated = complaint.copy(
                        status = ComplaintStatus.CLEANED,
                        beforeCleaningImageUri = beforeCleaningUri ?: complaint.beforeCleaningImageUri,
                        afterCleaningImageUri = afterCleaningUri,
                        statusHistory = newHistory,
                        updatedAt = now
                    )
                    updatedComplaint = updated
                    updated
                } else {
                    complaint
                }
            }
        }
        return updatedComplaint
    }
}
