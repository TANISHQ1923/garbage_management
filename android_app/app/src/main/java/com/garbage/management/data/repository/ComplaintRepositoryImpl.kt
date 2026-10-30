package com.garbage.management.data.repository

import com.garbage.management.data.local.LocalComplaintDataSource
import com.garbage.management.data.model.CreateComplaintRequestDto
import com.garbage.management.data.remote.ApiClient
import com.garbage.management.data.remote.RemoteComplaintDataSource
import com.garbage.management.data.remote.RemoteDriverDataSource
import com.garbage.management.data.remote.RemoteOfficerDataSource
import com.garbage.management.domain.model.CleanupFeedback
import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.model.GarbageDriver
import com.garbage.management.domain.model.OfficerNote
import com.garbage.management.domain.repository.ComplaintRepository
import com.garbage.management.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of ComplaintRepository connecting Citizen, Officer,
 * and Driver operations directly to the Node.js + MongoDB backend REST APIs.
 * Supports isLocalOnly flag for local unit testing and offline fallback.
 */
class ComplaintRepositoryImpl(
    private val remoteComplaintDataSource: RemoteComplaintDataSource,
    private val remoteOfficerDataSource: RemoteOfficerDataSource,
    private val remoteDriverDataSource: RemoteDriverDataSource,
    private val localDataSource: LocalComplaintDataSource? = null,
    private val isLocalOnly: Boolean = false
) : ComplaintRepository {

    /**
     * Backward-compatible constructor for local unit tests and mock-only workflows.
     */
    constructor(localDataSource: LocalComplaintDataSource) : this(
        remoteComplaintDataSource = RemoteComplaintDataSource(ApiClient.create()),
        remoteOfficerDataSource = RemoteOfficerDataSource(ApiClient.create()),
        remoteDriverDataSource = RemoteDriverDataSource(ApiClient.create()),
        localDataSource = localDataSource,
        isLocalOnly = true
    )

    override suspend fun submitComplaint(complaint: GarbageComplaint): Resource<GarbageComplaint> =
        withContext(Dispatchers.IO) {
            if (isLocalOnly && localDataSource != null) {
                localDataSource.addComplaint(complaint)
                return@withContext Resource.Success(complaint)
            }

            val request = CreateComplaintRequestDto(
                garbageType = complaint.garbageType.name,
                description = complaint.description,
                imageUrl = complaint.imageUri,
                videoUrl = complaint.videoUri,
                latitude = complaint.latitude,
                longitude = complaint.longitude,
                locationDescription = complaint.locationDescription,
                isEmergency = complaint.isEmergency
            )

            val remoteResult = remoteComplaintDataSource.submitComplaint(request)
            if (remoteResult is Resource.Success && remoteResult.data != null) {
                localDataSource?.addOrUpdateComplaint(remoteResult.data)
                remoteResult
            } else if (isLocalOnly && localDataSource != null) {
                localDataSource.addOrUpdateComplaint(complaint)
                Resource.Success(complaint)
            } else {
                remoteResult
            }
        }

    override fun getCitizenComplaints(citizenId: String): Flow<List<GarbageComplaint>> {
        if (isLocalOnly && localDataSource != null) {
            return localDataSource.getCitizenComplaints(citizenId)
        }

        return flow {
            val cached = localDataSource?.getCitizenComplaints(citizenId)?.firstOrNull() ?: emptyList()
            if (cached.isNotEmpty()) {
                emit(cached)
            }

            when (val remote = remoteComplaintDataSource.getMyComplaints()) {
                is Resource.Success -> {
                    val list = remote.data ?: emptyList()
                    list.forEach { localDataSource?.addOrUpdateComplaint(it) }
                    emit(list)
                }
                is Resource.Error -> throw IllegalStateException(remote.message ?: "Unable to load complaints.")
                is Resource.Loading -> Unit
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getComplaintById(complaintId: String): Resource<GarbageComplaint> =
        withContext(Dispatchers.IO) {
            if (isLocalOnly && localDataSource != null) {
                val cached = localDataSource.getComplaintById(complaintId)
                return@withContext if (cached != null) {
                    Resource.Success(cached)
                } else {
                    Resource.Error("Complaint with ID '$complaintId' not found.")
                }
            }

            val remoteResult = remoteComplaintDataSource.getComplaintById(complaintId)
            if (remoteResult is Resource.Success && remoteResult.data != null) {
                remoteResult
            } else {
                remoteResult
            }
        }

    override fun getAllComplaints(): Flow<List<GarbageComplaint>> {
        if (isLocalOnly && localDataSource != null) {
            return localDataSource.getAllComplaints()
        }

        return flow {
            val cached = localDataSource?.getAllComplaints()?.firstOrNull() ?: emptyList()
            if (cached.isNotEmpty()) {
                emit(cached)
            }

            when (val remote = remoteOfficerDataSource.getAllComplaints()) {
                is Resource.Success -> {
                    val list = remote.data ?: emptyList()
                    emit(list)
                }
                is Resource.Error -> throw IllegalStateException(remote.message ?: "Unable to load tasks.")
                is Resource.Loading -> Unit
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getPriorityQueue(): Resource<List<GarbageComplaint>> = withContext(Dispatchers.IO) {
        if (isLocalOnly && localDataSource != null) {
            val localQueue = localDataSource.getPriorityQueue()
            return@withContext Resource.Success(localQueue)
        }

        when (val remote = remoteOfficerDataSource.getPriorityQueue()) {
            is Resource.Success -> {
                val list = remote.data ?: emptyList()
                Resource.Success(list)
            }
            is Resource.Error -> {
                if (isLocalOnly && localDataSource != null) {
                    val fallback = localDataSource.getPriorityQueue()
                    Resource.Success(fallback)
                } else {
                    remote
                }
            }
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun assignDriver(
        complaintId: String,
        driver: GarbageDriver,
        officerId: String,
        officerName: String,
        noteText: String?
    ): Resource<GarbageComplaint> = withContext(Dispatchers.IO) {
        if (isLocalOnly && localDataSource != null) {
            val updated = localDataSource.assignDriver(complaintId, driver, officerId, officerName, noteText)
            return@withContext if (updated != null) {
                Resource.Success(updated)
            } else {
                Resource.Error("Failed to assign driver: Complaint '$complaintId' not found.")
            }
        }

        val remoteResult = remoteOfficerDataSource.assignDriver(
            complaintId = complaintId,
            driverId = driver.id,
            driverName = driver.name,
            vehicleNumber = driver.vehicleNumber,
            noteText = noteText
        )

        if (remoteResult is Resource.Success && remoteResult.data != null) {
            localDataSource?.assignDriver(complaintId, driver, officerId, officerName, noteText)
            remoteResult
        } else if (isLocalOnly && localDataSource != null) {
            val updated = localDataSource.assignDriver(complaintId, driver, officerId, officerName, noteText)
            if (updated != null) Resource.Success(updated) else remoteResult
        } else {
            remoteResult
        }
    }

    override suspend fun updateComplaintStatus(
        complaintId: String,
        newStatus: ComplaintStatus,
        officerId: String,
        officerName: String,
        noteText: String?
    ): Resource<GarbageComplaint> = withContext(Dispatchers.IO) {
        if (isLocalOnly && localDataSource != null) {
            val updated = localDataSource.updateComplaintStatus(complaintId, newStatus, officerId, officerName, noteText)
            return@withContext if (updated != null) {
                Resource.Success(updated)
            } else {
                Resource.Error("Failed to update status: Complaint '$complaintId' not found.")
            }
        }

        val remoteResult = remoteOfficerDataSource.updateStatus(
            complaintId = complaintId,
            newStatus = newStatus.name,
            noteText = noteText
        )

        if (remoteResult is Resource.Success && remoteResult.data != null) {
            localDataSource?.updateComplaintStatus(complaintId, newStatus, officerId, officerName, noteText)
            remoteResult
        } else if (isLocalOnly && localDataSource != null) {
            val updated = localDataSource.updateComplaintStatus(complaintId, newStatus, officerId, officerName, noteText)
            if (updated != null) Resource.Success(updated) else remoteResult
        } else {
            remoteResult
        }
    }

    override suspend fun addOfficerNote(
        complaintId: String,
        note: OfficerNote
    ): Resource<GarbageComplaint> = withContext(Dispatchers.IO) {
        if (isLocalOnly && localDataSource != null) {
            val updated = localDataSource.addOfficerNote(complaintId, note)
            return@withContext if (updated != null) {
                Resource.Success(updated)
            } else {
                Resource.Error("Failed to attach note: Complaint '$complaintId' not found.")
            }
        }

        val remoteResult = remoteOfficerDataSource.addOfficerNote(
            complaintId = complaintId,
            text = note.text
        )

        if (remoteResult is Resource.Success && remoteResult.data != null) {
            localDataSource?.addOfficerNote(complaintId, note)
            remoteResult
        } else if (isLocalOnly && localDataSource != null) {
            val updated = localDataSource.addOfficerNote(complaintId, note)
            if (updated != null) Resource.Success(updated) else remoteResult
        } else {
            remoteResult
        }
    }

    override fun getDriverComplaints(driverId: String): Flow<List<GarbageComplaint>> {
        if (isLocalOnly && localDataSource != null) {
            return localDataSource.getDriverComplaints(driverId)
        }

        return flow {
            val cached = localDataSource?.getDriverComplaints(driverId)?.firstOrNull() ?: emptyList()
            if (cached.isNotEmpty()) {
                emit(cached)
            }

            when (val remote = remoteDriverDataSource.getDriverTasks()) {
                is Resource.Success -> {
                    val list = remote.data ?: emptyList()
                    emit(list)
                }
                is Resource.Error -> throw IllegalStateException(remote.message ?: "Unable to load tasks.")
                is Resource.Loading -> Unit
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun startCollection(
        complaintId: String,
        driverId: String,
        driverName: String,
        beforeCleaningUri: String?
    ): Resource<GarbageComplaint> = withContext(Dispatchers.IO) {
        if (isLocalOnly && localDataSource != null) {
            val updated = localDataSource.startCollection(complaintId, driverId, driverName, beforeCleaningUri)
            return@withContext if (updated != null) {
                Resource.Success(updated)
            } else {
                Resource.Error("Failed to start collection: Complaint '$complaintId' not found.")
            }
        }

        val remoteResult = remoteDriverDataSource.startCollection(
            complaintId = complaintId,
            beforeCleaningImageUrl = beforeCleaningUri
        )

        if (remoteResult is Resource.Success && remoteResult.data != null) {
            localDataSource?.startCollection(complaintId, driverId, driverName, beforeCleaningUri)
            remoteResult
        } else if (isLocalOnly && localDataSource != null) {
            val updated = localDataSource.startCollection(complaintId, driverId, driverName, beforeCleaningUri)
            if (updated != null) Resource.Success(updated) else remoteResult
        } else {
            remoteResult
        }
    }

    override suspend fun updateCollectionEvidence(
        complaintId: String,
        beforeCleaningUri: String?,
        afterCleaningUri: String?
    ): Resource<GarbageComplaint> = withContext(Dispatchers.IO) {
        if (isLocalOnly && localDataSource != null) {
            val updated = localDataSource.updateCollectionEvidence(complaintId, beforeCleaningUri, afterCleaningUri)
            return@withContext if (updated != null) {
                Resource.Success(updated)
            } else {
                Resource.Error("Failed to update evidence: Complaint '$complaintId' not found.")
            }
        }

        val remoteResult = remoteDriverDataSource.updateEvidence(
            complaintId = complaintId,
            beforeCleaningImageUrl = beforeCleaningUri,
            afterCleaningImageUrl = afterCleaningUri
        )

        if (remoteResult is Resource.Success && remoteResult.data != null) {
            localDataSource?.updateCollectionEvidence(complaintId, beforeCleaningUri, afterCleaningUri)
            remoteResult
        } else if (isLocalOnly && localDataSource != null) {
            val updated = localDataSource.updateCollectionEvidence(complaintId, beforeCleaningUri, afterCleaningUri)
            if (updated != null) Resource.Success(updated) else remoteResult
        } else {
            remoteResult
        }
    }

    override suspend fun completeCollection(
        complaintId: String,
        driverId: String,
        driverName: String,
        afterCleaningUri: String,
        beforeCleaningUri: String?
    ): Resource<GarbageComplaint> = withContext(Dispatchers.IO) {
        if (isLocalOnly && localDataSource != null) {
            val updated = localDataSource.completeCollection(
                complaintId = complaintId,
                driverId = driverId,
                driverName = driverName,
                afterCleaningUri = afterCleaningUri,
                beforeCleaningUri = beforeCleaningUri
            )
            return@withContext if (updated != null) {
                Resource.Success(updated)
            } else {
                Resource.Error("Failed to complete collection: Complaint '$complaintId' not found.")
            }
        }

        val remoteResult = remoteDriverDataSource.completeCollection(
            complaintId = complaintId,
            afterCleaningImageUrl = afterCleaningUri,
            beforeCleaningImageUrl = beforeCleaningUri
        )

        if (remoteResult is Resource.Success && remoteResult.data != null) {
            localDataSource?.completeCollection(
                complaintId = complaintId,
                driverId = driverId,
                driverName = driverName,
                afterCleaningUri = afterCleaningUri,
                beforeCleaningUri = beforeCleaningUri
            )
            remoteResult
        } else if (isLocalOnly && localDataSource != null) {
            val updated = localDataSource.completeCollection(
                complaintId = complaintId,
                driverId = driverId,
                driverName = driverName,
                afterCleaningUri = afterCleaningUri,
                beforeCleaningUri = beforeCleaningUri
            )
            if (updated != null) Resource.Success(updated) else remoteResult
        } else {
            remoteResult
        }
    }

    override suspend fun submitFeedback(
        complaintId: String,
        rating: Int,
        comment: String?,
        citizenId: String?,
        citizenName: String?
    ): Resource<GarbageComplaint> = withContext(Dispatchers.IO) {
        val fallbackFeedback = CleanupFeedback(
            rating = rating,
            comment = comment ?: "",
            submittedAt = System.currentTimeMillis(),
            citizenId = citizenId ?: "",
            citizenName = citizenName ?: ""
        )

        if (isLocalOnly && localDataSource != null) {
            val updated = localDataSource.submitFeedback(complaintId, fallbackFeedback)
            return@withContext if (updated != null) {
                Resource.Success(updated)
            } else {
                Resource.Error("Complaint with ID '$complaintId' not found.")
            }
        }

        val remoteResult = remoteComplaintDataSource.submitFeedback(complaintId, rating, comment)
        if (remoteResult is Resource.Success && remoteResult.data != null) {
            val feedbackToSave = remoteResult.data.feedback ?: fallbackFeedback
            localDataSource?.submitFeedback(complaintId, feedbackToSave)
            remoteResult
        } else if (isLocalOnly && localDataSource != null) {
            val updated = localDataSource.submitFeedback(complaintId, fallbackFeedback)
            if (updated != null) Resource.Success(updated) else remoteResult
        } else {
            remoteResult
        }
    }
}
