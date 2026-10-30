package com.garbage.management

import com.garbage.management.data.local.LocalComplaintDataSource
import com.garbage.management.data.model.ComplaintDto
import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.model.GarbageType
import com.garbage.management.domain.repository.ComplaintRepository
import com.garbage.management.domain.usecase.GetPriorityQueueUseCase
import com.garbage.management.presentation.viewmodel.PriorityCalculator
import com.garbage.management.utils.Resource
import com.google.gson.Gson
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PriorityQueueTest {

    private val gson = Gson()

    @Test
    fun testComplaintDtoPriorityMapping() {
        val json = """
            {
                "id": "CMP-9001",
                "citizenId": "CIT-01",
                "citizenName": "Aarav Sharma",
                "garbageType": "OVERFLOWING_BIN",
                "locationText": "Market Square",
                "status": "SUBMITTED",
                "isEmergency": true,
                "priorityScore": 95,
                "priorityLevel": "CRITICAL"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, ComplaintDto::class.java)
        val domain = dto.toDomain()

        assertEquals("CMP-9001", domain.id)
        assertEquals(95, domain.priorityScore)
        assertEquals("CRITICAL", domain.priorityLevel)
        assertTrue(domain.isEmergency)
    }

    @Test
    fun testEmergencyBonusCalculation() {
        val now = 1_000_000_000_000L
        val emergencyComplaint = createComplaint(
            id = "CMP-EMERGENCY",
            isEmergency = true,
            createdAt = now - (1 * 3600 * 1000L), // 1 hour old (+0 age bonus)
            status = ComplaintStatus.SUBMITTED // +20 status bonus
        )
        val regularComplaint = createComplaint(
            id = "CMP-REGULAR",
            isEmergency = false,
            createdAt = now - (1 * 3600 * 1000L),
            status = ComplaintStatus.SUBMITTED
        )

        // Emergency: 50 (emergency) + 0 (age) + 20 (status) = 70 (CRITICAL)
        val emergencyScore = PriorityCalculator.calculateScore(emergencyComplaint, now)
        assertEquals(70, emergencyScore)
        assertEquals("CRITICAL", PriorityCalculator.getPriorityLevel(emergencyScore))

        // Regular: 0 + 0 + 20 = 20 (LOW)
        val regularScore = PriorityCalculator.calculateScore(regularComplaint, now)
        assertEquals(20, regularScore)
        assertEquals("LOW", PriorityCalculator.getPriorityLevel(regularScore))
    }

    @Test
    fun testAgeBonusTiersDoNotAccumulate() {
        val now = 1_000_000_000_000L
        val hourMs = 3600 * 1000L

        // Less than 6 hours -> +0
        val c2h = createComplaint("C-2H", createdAt = now - (2 * hourMs), status = ComplaintStatus.SUBMITTED)
        assertEquals(20, PriorityCalculator.calculateScore(c2h, now)) // 0 + 20 = 20

        // 6 to 12 hours -> +8
        val c8h = createComplaint("C-8H", createdAt = now - (8 * hourMs), status = ComplaintStatus.SUBMITTED)
        assertEquals(28, PriorityCalculator.calculateScore(c8h, now)) // 8 + 20 = 28
        assertEquals("MEDIUM", PriorityCalculator.getPriorityLevel(28))

        // 12 to 24 hours -> +15
        val c18h = createComplaint("C-18H", createdAt = now - (18 * hourMs), status = ComplaintStatus.SUBMITTED)
        assertEquals(35, PriorityCalculator.calculateScore(c18h, now)) // 15 + 20 = 35
        assertEquals("MEDIUM", PriorityCalculator.getPriorityLevel(35))

        // 24 hours or older -> +25 (must NOT accumulate with 15 or 8)
        val c30h = createComplaint("C-30H", createdAt = now - (30 * hourMs), status = ComplaintStatus.SUBMITTED)
        assertEquals(45, PriorityCalculator.calculateScore(c30h, now)) // 25 + 20 = 45
        assertEquals("MEDIUM", PriorityCalculator.getPriorityLevel(45))
    }

    @Test
    fun testStatusBonuses() {
        val now = 1_000_000_000_000L
        val hourMs = 3600 * 1000L
        val recentTime = now - hourMs // age bonus = 0

        val submitted = createComplaint("S1", status = ComplaintStatus.SUBMITTED, createdAt = recentTime)
        val assigned = createComplaint("A1", status = ComplaintStatus.ASSIGNED, createdAt = recentTime)
        val inProgress = createComplaint("P1", status = ComplaintStatus.PICKUP_IN_PROGRESS, createdAt = recentTime)
        val cleaned = createComplaint("C1", status = ComplaintStatus.CLEANED, createdAt = recentTime)
        val verified = createComplaint("V1", status = ComplaintStatus.VERIFIED, createdAt = recentTime)
        val rejected = createComplaint("R1", status = ComplaintStatus.REJECTED, createdAt = recentTime)

        assertEquals(20, PriorityCalculator.calculateScore(submitted, now))
        assertEquals(10, PriorityCalculator.calculateScore(assigned, now))
        assertEquals(5, PriorityCalculator.calculateScore(inProgress, now))
        assertEquals(0, PriorityCalculator.calculateScore(cleaned, now))
        assertEquals(0, PriorityCalculator.calculateScore(verified, now))
        assertEquals(0, PriorityCalculator.calculateScore(rejected, now))
    }

    @Test
    fun testMaxScoreClampingAndPriorityLabels() {
        val now = 1_000_000_000_000L
        // Emergency (+50) + >=24h (+25) + SUBMITTED (+20) = 95
        val maxEligible = createComplaint(
            "C-MAX",
            isEmergency = true,
            createdAt = now - (48 * 3600 * 1000L),
            status = ComplaintStatus.SUBMITTED
        )
        val score = PriorityCalculator.calculateScore(maxEligible, now)
        assertEquals(95, score)
        assertEquals("CRITICAL", PriorityCalculator.getPriorityLevel(score))

        assertEquals("CRITICAL", PriorityCalculator.getPriorityLevel(70))
        assertEquals("HIGH", PriorityCalculator.getPriorityLevel(69))
        assertEquals("HIGH", PriorityCalculator.getPriorityLevel(50))
        assertEquals("MEDIUM", PriorityCalculator.getPriorityLevel(49))
        assertEquals("MEDIUM", PriorityCalculator.getPriorityLevel(25))
        assertEquals("LOW", PriorityCalculator.getPriorityLevel(24))
        assertEquals("LOW", PriorityCalculator.getPriorityLevel(0))
    }

    @Test
    fun testActiveDispatchQueueExclusions() {
        assertFalse(PriorityCalculator.isEligibleForActiveQueue(ComplaintStatus.VERIFIED))
        assertFalse(PriorityCalculator.isEligibleForActiveQueue(ComplaintStatus.REJECTED))
        assertFalse(PriorityCalculator.isEligibleForActiveQueue(ComplaintStatus.CLEANED))
        assertTrue(PriorityCalculator.isEligibleForActiveQueue(ComplaintStatus.SUBMITTED))
        assertTrue(PriorityCalculator.isEligibleForActiveQueue(ComplaintStatus.ASSIGNED))
        assertTrue(PriorityCalculator.isEligibleForActiveQueue(ComplaintStatus.PICKUP_IN_PROGRESS))
    }

    @Test
    fun testLocalPriorityQueueSortingAndDeduplication() {
        val now = 1_000_000_000_000L
        val hourMs = 3600 * 1000L
        val dataSource = LocalComplaintDataSource(demoMode = true)

        val queue = dataSource.getPriorityQueue(now)

        // Must not contain CLEANED, VERIFIED, or REJECTED
        for (item in queue) {
            assertTrue(
                "Queue item ${item.id} has invalid status ${item.status}",
                item.status == ComplaintStatus.SUBMITTED ||
                        item.status == ComplaintStatus.ASSIGNED ||
                        item.status == ComplaintStatus.PICKUP_IN_PROGRESS
            )
            assertNotNull(item.priorityScore)
            assertNotNull(item.priorityLevel)
        }

        // Must be sorted in descending order of priority score
        for (i in 0 until queue.size - 1) {
            val current = queue[i]
            val next = queue[i + 1]
            val scoreCurrent = current.priorityScore ?: 0
            val scoreNext = next.priorityScore ?: 0
            assertTrue(
                "Queue not sorted by score: ${current.id}($scoreCurrent) should be >= ${next.id}($scoreNext)",
                scoreCurrent >= scoreNext
            )
            if (scoreCurrent == scoreNext) {
                // Older complaint first (createdAt ascending)
                assertTrue(
                    "Tie breaker failed: ${current.id}(${current.createdAt}) should be <= ${next.id}(${next.createdAt})",
                    current.createdAt <= next.createdAt
                )
            }
        }
    }

    @Test
    fun testGetPriorityQueueUseCaseAndRepository() = runBlocking {
        val localDataSource = LocalComplaintDataSource(demoMode = true)
        val repository = com.garbage.management.data.repository.ComplaintRepositoryImpl(localDataSource)
        val useCase = GetPriorityQueueUseCase(repository)

        val result = useCase()
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertNotNull(data)
        assertTrue(data!!.isNotEmpty())

        // Verify all items are active queue eligible and sorted by priority score descending
        for (i in 0 until data.size - 1) {
            val cur = data[i]
            val next = data[i + 1]
            assertTrue(cur.priorityScore ?: 0 >= next.priorityScore ?: 0)
            assertTrue(
                cur.status == ComplaintStatus.SUBMITTED ||
                cur.status == ComplaintStatus.ASSIGNED ||
                cur.status == ComplaintStatus.PICKUP_IN_PROGRESS
            )
        }
    }

    private fun createComplaint(
        id: String,
        isEmergency: Boolean = false,
        createdAt: Long = 1_000_000_000_000L,
        status: ComplaintStatus = ComplaintStatus.SUBMITTED,
        priorityScore: Int? = null,
        priorityLevel: String? = null
    ): GarbageComplaint {
        return GarbageComplaint(
            id = id,
            citizenId = "CIT-01",
            citizenName = "Citizen Test",
            citizenPhone = "9876543210",
            garbageType = GarbageType.OVERFLOWING_BIN,
            locationDescription = "Test Location",
            latitude = 12.9716,
            longitude = 77.5946,
            status = status,
            createdAt = createdAt,
            updatedAt = createdAt,
            isEmergency = isEmergency,
            priorityScore = priorityScore,
            priorityLevel = priorityLevel
        )
    }
}
