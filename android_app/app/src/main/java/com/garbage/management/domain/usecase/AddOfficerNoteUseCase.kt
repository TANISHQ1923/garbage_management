package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.model.OfficerNote
import com.garbage.management.domain.repository.ComplaintRepository
import com.garbage.management.utils.Resource
import java.util.UUID

/**
 * Use case allowing municipal officers to attach internal investigation/operational notes to a complaint.
 */
class AddOfficerNoteUseCase(
    private val complaintRepository: ComplaintRepository
) {
    suspend operator fun invoke(
        complaintId: String,
        officerId: String,
        officerName: String,
        text: String
    ): Resource<GarbageComplaint> {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            return Resource.Error("Note content cannot be empty.")
        }

        val note = OfficerNote(
            id = "NOTE-" + UUID.randomUUID().toString().take(8).uppercase(),
            officerId = officerId,
            officerName = officerName,
            text = cleanText,
            createdAt = System.currentTimeMillis()
        )

        return complaintRepository.addOfficerNote(complaintId, note)
    }
}
