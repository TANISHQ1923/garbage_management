package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.repository.ComplaintRepository
import com.garbage.management.utils.Resource

/**
 * Use case to retrieve the active complaint priority queue for municipal officers.
 */
class GetPriorityQueueUseCase(
    private val repository: ComplaintRepository
) {
    suspend operator fun invoke(): Resource<List<GarbageComplaint>> {
        return repository.getPriorityQueue()
    }
}
