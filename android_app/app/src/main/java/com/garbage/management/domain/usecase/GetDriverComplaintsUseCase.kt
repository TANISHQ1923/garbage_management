package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.repository.ComplaintRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to retrieve all active and historical complaints assigned to a specific driver.
 */
class GetDriverComplaintsUseCase(
    private val repository: ComplaintRepository
) {
    operator fun invoke(driverId: String): Flow<List<GarbageComplaint>> {
        return repository.getDriverComplaints(driverId)
    }
}
