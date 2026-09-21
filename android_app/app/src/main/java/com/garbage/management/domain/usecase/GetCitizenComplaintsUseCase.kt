package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.repository.ComplaintRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case to retrieve complaints submitted by a specific citizen.
 */
class GetCitizenComplaintsUseCase(
    private val repository: ComplaintRepository
) {
    operator fun invoke(citizenId: String): Flow<List<GarbageComplaint>> {
        return repository.getCitizenComplaints(citizenId).map { list ->
            list.sortedByDescending { it.createdAt }
        }
    }
}
