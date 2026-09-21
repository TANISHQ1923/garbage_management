package com.garbage.management.domain.repository

import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.model.GarbageDriver
import com.garbage.management.domain.model.OfficerNote
import com.garbage.management.utils.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining operations for managing garbage complaints.
 */
interface ComplaintRepository {
    suspend fun submitComplaint(complaint: GarbageComplaint): Resource<GarbageComplaint>
    fun getCitizenComplaints(citizenId: String): Flow<List<GarbageComplaint>>
    suspend fun getComplaintById(complaintId: String): Resource<GarbageComplaint>
    fun getAllComplaints(): Flow<List<GarbageComplaint>>
    suspend fun assignDriver(
        complaintId: String,
        driver: GarbageDriver,
        officerId: String,
        officerName: String,
        noteText: String? = null
    ): Resource<GarbageComplaint>
    suspend fun updateComplaintStatus(
        complaintId: String,
        newStatus: ComplaintStatus,
        officerId: String,
        officerName: String,
        noteText: String? = null
    ): Resource<GarbageComplaint>
    suspend fun addOfficerNote(
        complaintId: String,
        note: OfficerNote
    ): Resource<GarbageComplaint>
    fun getDriverComplaints(driverId: String): Flow<List<GarbageComplaint>>
    suspend fun startCollection(
        complaintId: String,
        driverId: String,
        driverName: String,
        beforeCleaningUri: String? = null
    ): Resource<GarbageComplaint>
    suspend fun updateCollectionEvidence(
        complaintId: String,
        beforeCleaningUri: String? = null,
        afterCleaningUri: String? = null
    ): Resource<GarbageComplaint>
    suspend fun completeCollection(
        complaintId: String,
        driverId: String,
        driverName: String,
        afterCleaningUri: String,
        beforeCleaningUri: String? = null
    ): Resource<GarbageComplaint>
}

