package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.repository.ComplaintRepository
import com.garbage.management.utils.Resource

/**
 * Use case to submit citizen cleanup feedback with validation rules.
 */
class SubmitFeedbackUseCase(
    private val repository: ComplaintRepository
) {
    suspend operator fun invoke(
        complaintId: String,
        rating: Int,
        comment: String? = null,
        citizenId: String? = null,
        citizenName: String? = null
    ): Resource<GarbageComplaint> {
        if (complaintId.isBlank()) {
            return Resource.Error("Complaint ID is required.")
        }
        if (rating < 1 || rating > 5) {
            return Resource.Error("Rating must be between 1 and 5 stars.")
        }
        val trimmedComment = comment?.trim()
        if (trimmedComment != null && trimmedComment.length > 500) {
            return Resource.Error("Comment cannot exceed 500 characters.")
        }
        return repository.submitFeedback(
            complaintId = complaintId,
            rating = rating,
            comment = trimmedComment,
            citizenId = citizenId,
            citizenName = citizenName
        )
    }
}
