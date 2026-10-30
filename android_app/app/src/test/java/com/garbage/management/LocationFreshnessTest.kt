package com.garbage.management

import com.garbage.management.utils.LocationFreshness
import org.junit.Assert.*
import org.junit.Test

class LocationFreshnessTest {
    @Test fun acceptsFreshRealFixAndAgeBoundary() {
        assertTrue(LocationFreshness.isFresh(90_000_000_000L, 100_000_000_000L))
        assertTrue(LocationFreshness.isFresh(40_000_000_000L, 100_000_000_000L))
    }
    @Test fun rejectsStaleFutureAndMissingCaptureTimes() {
        assertFalse(LocationFreshness.isFresh(39_999_999_999L, 100_000_000_000L))
        assertFalse(LocationFreshness.isFresh(100_000_000_001L, 100_000_000_000L))
        assertFalse(LocationFreshness.isFresh(0L, 100_000_000_000L))
    }
}
