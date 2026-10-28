package com.garbage.management.data.local

import com.garbage.management.domain.model.CleanupFeedback
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

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * In-memory & disk-backed thread-safe local data source for garbage complaints.
 * Preloaded with realistic initial complaints and persisted locally so complaints survive disconnections and app restarts.
 */
class LocalComplaintDataSource(
    private val context: Context? = null
) {

    private val gson = Gson()
    private val cacheFileName = "persisted_complaints.json"

    private fun loadFromDisk(): List<GarbageComplaint>? {
        if (context == null) return null
        return try {
            val file = File(context.filesDir, cacheFileName)
            if (file.exists()) {
                val json = file.readText()
                val type = object : TypeToken<List<GarbageComplaint>>() {}.type
                val loaded: List<GarbageComplaint>? = gson.fromJson(json, type)
                if (!loaded.isNullOrEmpty()) loaded else null
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun persistToDisk(list: List<GarbageComplaint>) {
        if (context == null) return
        try {
            val file = File(context.filesDir, cacheFileName)
            val json = gson.toJson(list)
            file.writeText(json)
        } catch (_: Exception) {}
    }

    private val defaultComplaints = listOf(
        GarbageComplaint(
            id = "SGM-2026-004510",
            citizenId = "mock-citizen-002",
            citizenName = "Priya Sen (Citizen)",
            citizenPhone = "+91 9876543299",
            garbageType = GarbageType.OTHER_WASTE,
            description = "Chemical containers and broken medical glass dumped behind clinic lane. Poses immediate health hazard.",
            imageUri = null,
            videoUri = null,
            latitude = 18.4682,
            longitude = 73.8647,
            locationDescription = "KK Market Main Road, Bibwewadi, Pune",
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
            latitude = 18.4678,
            longitude = 73.8661,
            locationDescription = "Chintamani Nagar Gate 2, Bibwewadi, Pune",
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
            latitude = 18.4712,
            longitude = 73.8665,
            locationDescription = "Mahavir Nagar Ground, Bibwewadi, Pune",
            isEmergency = true,
            status = ComplaintStatus.ASSIGNED,
            assignedDriverId = "DRV-882",
            assignedDriverName = "Ramesh Kumar",
            assignedVehicleNumber = "MH-12-GB-4040",
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
                    note = "Assigned to driver Ramesh Kumar (MH-12-GB-4040)"
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
            latitude = 18.4628,
            longitude = 73.8694,
            locationDescription = "Upper Indira Nagar Flyover Underpass, Bibwewadi, Pune",
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
            latitude = 18.4721,
            longitude = 73.8615,
            locationDescription = "Swami Vivekanand Road Junction, Bibwewadi, Pune",
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
            latitude = 18.4822,
            longitude = 73.8518,
            locationDescription = "Sahakar Nagar Service Road Gate 1, Pune",
            isEmergency = false,
            status = ComplaintStatus.VERIFIED,
            assignedDriverId = "DRV-882",
            assignedDriverName = "Ramesh Kumar",
            assignedVehicleNumber = "MH-12-GB-4040",
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
            description = "Secondary community garbage bin near primary school gate is overflowing.",
            imageUri = null,
            videoUri = null,
            latitude = 18.4655,
            longitude = 73.8672,
            locationDescription = "Bibwewadi Primary School Gate, Pune",
            isEmergency = false,
            status = ComplaintStatus.ASSIGNED,
            assignedDriverId = "DRV-882",
            assignedDriverName = "Ramesh Kumar",
            assignedVehicleNumber = "MH-12-GB-4040",
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
                    note = "Assigned to driver Ramesh Kumar (MH-12-GB-4040)"
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
            latitude = 18.4756,
            longitude = 73.8582,
            locationDescription = "Padmavati Temple Road, Ward 19, Pune",
            isEmergency = false,
            status = ComplaintStatus.CLEANED,
            assignedDriverId = "DRV-882",
            assignedDriverName = "Ramesh Kumar",
            assignedVehicleNumber = "MH-12-GB-4040",
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

    private val complaintsFlow = MutableStateFlow<List<GarbageComplaint>>(
        loadFromDisk() ?: defaultComplaints
    )

    fun addComplaint(complaint: GarbageComplaint) {
        complaintsFlow.update { current ->
            listOf(complaint) + current
        }
        persistToDisk(complaintsFlow.value)
    }

    fun addOrUpdateComplaint(complaint: GarbageComplaint) {
        complaintsFlow.update { current ->
            val idx = current.indexOfFirst { it.id.equals(complaint.id, ignoreCase = true) }
            if (idx != -1) {
                current.toMutableList().apply { set(idx, complaint) }
            } else {
                listOf(complaint) + current
            }
        }
        persistToDisk(complaintsFlow.value)
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
        persistToDisk(complaintsFlow.value)
    }

    fun getCitizenComplaints(citizenId: String): Flow<List<GarbageComplaint>> {
        return complaintsFlow.map { list ->
            val isDemo = citizenId.equals("67b300000000000000000001", ignoreCase = true) ||
                    citizenId.equals("mock-citizen-001", ignoreCase = true) ||
                    citizenId.equals("65f100000000000000000001", ignoreCase = true)
            list.filter {
                it.citizenId.equals(citizenId, ignoreCase = true) ||
                        (isDemo && (it.citizenId.equals("mock-citizen-001", ignoreCase = true) ||
                                it.citizenId.equals("65f100000000000000000001", ignoreCase = true)))
            }
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

    fun submitFeedback(complaintId: String, feedback: CleanupFeedback): GarbageComplaint? {
        var updatedComplaint: GarbageComplaint? = null
        complaintsFlow.update { currentList ->
            currentList.map { complaint ->
                if (complaint.id.equals(complaintId, ignoreCase = true)) {
                    val updated = complaint.copy(
                        feedback = feedback,
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

    fun getPriorityQueue(now: Long = System.currentTimeMillis()): List<GarbageComplaint> {
        return complaintsFlow.value
            .filter {
                it.status == ComplaintStatus.SUBMITTED ||
                it.status == ComplaintStatus.ASSIGNED ||
                it.status == ComplaintStatus.PICKUP_IN_PROGRESS
            }
            .map { complaint ->
                val emergencyPoints = if (complaint.isEmergency) 50 else 0
                val ageHours = (now - complaint.createdAt).coerceAtLeast(0) / (1000.0 * 60 * 60)
                val agePoints = when {
                    ageHours >= 24 -> 25
                    ageHours >= 12 -> 15
                    ageHours >= 6 -> 8
                    else -> 0
                }
                val statusPoints = when (complaint.status) {
                    ComplaintStatus.SUBMITTED -> 20
                    ComplaintStatus.ASSIGNED -> 10
                    ComplaintStatus.PICKUP_IN_PROGRESS -> 5
                    else -> 0
                }
                val score = (emergencyPoints + agePoints + statusPoints).coerceIn(0, 100)
                val level = when {
                    score >= 70 -> "CRITICAL"
                    score >= 50 -> "HIGH"
                    score >= 25 -> "MEDIUM"
                    else -> "LOW"
                }
                complaint.copy(priorityScore = score, priorityLevel = level)
            }
            .sortedWith(
                compareByDescending<GarbageComplaint> { it.priorityScore ?: 0 }
                    .thenBy { it.createdAt }
                    .thenBy { it.id }
            )
    }
}
