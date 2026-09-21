package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.repository.ComplaintRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to retrieve all complaints across the municipality for the officer portal.
 */
class GetAllComplaintsUseCase(
    private val repository: ComplaintRepository
) {
    operator fun invoke(): Flow<List<GarbageComplaint>> {
        return repository.getAllComplaints()
    }
}
