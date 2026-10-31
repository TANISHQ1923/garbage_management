package com.garbage.management

import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.model.GarbageType
import com.garbage.management.presentation.components.displayId
import com.garbage.management.presentation.components.formatDisplayComplaintId
import org.junit.Assert.assertEquals
import org.junit.Test

class ComplaintIdFormatTest {

    @Test
    fun `standard short id retains format`() {
        assertEquals("SGM-101542", formatDisplayComplaintId("SGM-101542"))
        assertEquals("SGM-999999", formatDisplayComplaintId("SGM-999999"))
    }

    @Test
    fun `legacy long date format simplifies to short reference`() {
        assertEquals("SGM-102881", formatDisplayComplaintId("SGM-2026-102881"))
        assertEquals("SGM-004128", formatDisplayComplaintId("SGM-2026-004128"))
        assertEquals("SGM-003890", formatDisplayComplaintId("SGM-2026-003890"))
    }

    @Test
    fun `legacy CMP timestamp format simplifies to short reference`() {
        assertEquals("SGM-551912", formatDisplayComplaintId("CMP-551912-8624"))
    }

    @Test
    fun `blank or null id falls back safely`() {
        assertEquals("SGM-000000", formatDisplayComplaintId(null))
        assertEquals("SGM-000000", formatDisplayComplaintId(""))
    }

    @Test
    fun `domain complaint displayId extension property works`() {
        val complaint = GarbageComplaint(
            id = "SGM-2026-102881",
            citizenId = "citizen-1",
            citizenName = "Aarav Sharma",
            garbageType = GarbageType.OVERFLOWING_BIN,
            locationDescription = "Main Road"
        )
        assertEquals("SGM-102881", complaint.displayId)
    }
}