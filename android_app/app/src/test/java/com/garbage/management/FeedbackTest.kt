package com.garbage.management

import com.garbage.management.data.local.LocalComplaintDataSource
import com.garbage.management.data.model.CleanupFeedbackDto
import com.garbage.management.data.model.ComplaintDto
import com.garbage.management.data.repository.ComplaintRepositoryImpl
import com.garbage.management.domain.model.CleanupFeedback
import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.model.GarbageType
import com.garbage.management.domain.usecase.SubmitFeedbackUseCase
import com.garbage.management.utils.Resource
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackTest {

    private val gson = Gson()

    @Test
    fun testComplaintDtoParsingWithoutFeedback() {
        val json = """
            {
                "id": "CMP-1001",
                "citizenId": "CIT-01",
                "citizenName": "Aarav",
                "garbageType": "OVERFLOWING_BIN",
                "locationText": "Park Avenue",
                "status": "VERIFIED"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, ComplaintDto::class.java)
        val domain = dto.toDomain()

        assertEquals("CMP-1001", domain.id)
        assertNull(domain.feedback)
        assertEquals(ComplaintStatus.VERIFIED, domain.status)
    }

    @Test
    fun testComplaintDtoParsingWithFeedback() {
        val json = """
            {
                "id": "CMP-1002",
                "citizenId": "CIT-02",
                "citizenName": "Bhavna",
                "garbageType": "ROADSIDE_GARBAGE",
                "locationText": "Ring Road",
                "status": "VERIFIED",
                "feedback": {
                    "rating": 5,
                    "comment": "Quick and spotless cleanup!",
                    "submittedAt": 1773738000000,
                    "citizenId": "CIT-02",
                    "citizenName": "Bhavna"
                }
            }
        """.trimIndent()

        val dto = gson.fromJson(json, ComplaintDto::class.java)
        val domain = dto.toDomain()

        assertNotNull(domain.feedback)
        assertEquals(5, domain.feedback?.rating)
        assertEquals("Quick and spotless cleanup!", domain.feedback?.comment)
        assertEquals(1773738000000L, domain.feedback?.submittedAt)
        assertEquals("CIT-02", domain.feedback?.citizenId)
        assertEquals("Bhavna", domain.feedback?.citizenName)
    }

    @Test
    fun testCleanupFeedbackDtoToDomainNullRating() {
        val dto = CleanupFeedbackDto(rating = null, comment = "No rating")
        assertNull(dto.toDomain())
    }

    @Test
    fun testSubmitFeedbackUseCaseValidation() = runBlocking {
        val localDataSource = LocalComplaintDataSource()
        val repository = ComplaintRepositoryImpl(localDataSource)
        val useCase = SubmitFeedbackUseCase(repository)

        // 1. Blank complaintId
        val resBlank = useCase(complaintId = "", rating = 5)
        assertTrue(resBlank is Resource.Error)
        assertEquals("Complaint ID is required.", (resBlank as Resource.Error).message)

        // 2. Rating below 1
        val resLow = useCase(complaintId = "SGM-2026-004128", rating = 0)
        assertTrue(resLow is Resource.Error)
        assertEquals("Rating must be between 1 and 5 stars.", (resLow as Resource.Error).message)

        // 3. Rating above 5
        val resHigh = useCase(complaintId = "SGM-2026-004128", rating = 6)
        assertTrue(resHigh is Resource.Error)
        assertEquals("Rating must be between 1 and 5 stars.", (resHigh as Resource.Error).message)

        // 4. Comment exceeds 500 characters
        val longComment = "x".repeat(501)
        val resLong = useCase(complaintId = "SGM-2026-004128", rating = 4, comment = longComment)
        assertTrue(resLong is Resource.Error)
        assertEquals("Comment cannot exceed 500 characters.", (resLong as Resource.Error).message)

        // 5. Valid submission on preloaded mock complaint
        val validRes = useCase(
            complaintId = "SGM-2026-004128",
            rating = 5,
            comment = "   Great service!   ",
            citizenId = "mock-citizen-001",
            citizenName = "Aarav Sharma"
        )
        assertTrue(validRes is Resource.Success)
        val updated = (validRes as Resource.Success).data
        assertNotNull(updated)
        assertNotNull(updated?.feedback)
        assertEquals(5, updated?.feedback?.rating)
        assertEquals("Great service!", updated?.feedback?.comment)
        assertEquals("mock-citizen-001", updated?.feedback?.citizenId)
    }
}
