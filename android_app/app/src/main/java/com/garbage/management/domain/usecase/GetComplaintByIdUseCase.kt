package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.repository.ComplaintRepository
import com.garbage.management.utils.Resource

/**
 * Use case to fetch a single complaint by its unique reference ID.
 */
class GetComplaintByIdUseCase(
    private val repository: ComplaintRepository
) {
    suspend operator fun invoke(complaintId: String): Resource<GarbageComplaint> {
        if (complaintId.isBlank()) {
            return Resource.Error("Invalid Complaint ID.")
        }
        return repository.getComplaintById(complaintId)
    }
}
