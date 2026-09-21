package com.garbage.management

import com.garbage.management.data.model.AiAnalysisDto
import com.garbage.management.data.model.ClassificationDto
import com.garbage.management.data.model.ComplaintDto
import com.garbage.management.data.model.SeverityDto
import com.garbage.management.domain.model.AiAnalysis
import com.garbage.management.domain.model.AiClassification
import com.garbage.management.domain.model.AiSeverity
import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.model.GarbageType
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ComplaintAiAnalysisTest {

    private val gson = Gson()

    @Test
    fun testComplaintDtoParsingWithoutAiAnalysis() {
        val json = """
            {
                "id": "CMP-1001",
                "citizenId": "CIT-01",
                "citizenName": "Aarav",
                "garbageType": "OVERFLOWING_BIN",
                "locationText": "Park Avenue",
                "status": "SUBMITTED"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, ComplaintDto::class.java)
        val domain = dto.toDomain()

        assertEquals("CMP-1001", domain.id)
        assertNull(domain.aiAnalysis)
        assertNull(domain.aiAnalysisStatus)
        assertEquals(ComplaintStatus.SUBMITTED, domain.status)
    }

    @Test
    fun testComplaintDtoParsingWithFullAiAnalysis() {
        val json = """
            {
                "id": "CMP-1002",
                "citizenId": "CIT-02",
                "citizenName": "Bhavna",
                "garbageType": "ROADSIDE_GARBAGE",
                "locationText": "Ring Road",
                "status": "SUBMITTED",
                "aiAnalysisStatus": "COMPLETED",
                "aiAnalysis": {
                    "classification": {
                        "category": "PLASTIC",
                        "confidence": null
                    },
                    "severity": {
                        "level": "HIGH",
                        "confidence": null
                    },
                    "modelStatus": "DEVELOPMENT_BASELINE",
                    "analyzedAt": 1773738000000
                }
            }
        """.trimIndent()

        val dto = gson.fromJson(json, ComplaintDto::class.java)
        val domain = dto.toDomain()

        assertNotNull(domain.aiAnalysis)
        assertEquals("COMPLETED", domain.aiAnalysisStatus)
        assertEquals("PLASTIC", domain.aiAnalysis?.classification?.category)
        assertNull(domain.aiAnalysis?.classification?.confidence)
        assertEquals("HIGH", domain.aiAnalysis?.severity?.level)
        assertNull(domain.aiAnalysis?.severity?.confidence)
        assertEquals("DEVELOPMENT_BASELINE", domain.aiAnalysis?.modelStatus)
        assertEquals(1773738000000L, domain.aiAnalysis?.analyzedAt)
    }

    @Test
    fun testAiAnalysisStatusPendingOrUnavailable() {
        val json = """
            {
                "id": "CMP-1003",
                "citizenId": "CIT-03",
                "citizenName": "Charan",
                "garbageType": "EVENT_WASTE",
                "locationText": "Stadium Ground",
                "status": "SUBMITTED",
                "aiAnalysisStatus": "AI_UNAVAILABLE",
                "aiAnalysis": null
            }
        """.trimIndent()

        val dto = gson.fromJson(json, ComplaintDto::class.java)
        val domain = dto.toDomain()

        assertEquals("AI_UNAVAILABLE", domain.aiAnalysisStatus)
        assertNull(domain.aiAnalysis)
    }

    @Test
    fun testDomainModelDefaultValuesCompatibility() {
        val complaint = GarbageComplaint(
            id = "CMP-DEFAULT",
            citizenId = "CIT-99",
            citizenName = "Test Citizen",
            garbageType = GarbageType.OVERFLOWING_BIN,
            locationDescription = "Sector 12"
        )

        assertNull(complaint.aiAnalysis)
        assertNull(complaint.aiAnalysisStatus)
        assertEquals(ComplaintStatus.SUBMITTED, complaint.status)
    }
}
