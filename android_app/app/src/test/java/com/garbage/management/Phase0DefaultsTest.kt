package com.garbage.management

import com.garbage.management.data.local.LocalComplaintDataSource
import com.garbage.management.data.local.LocalNotificationDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase0DefaultsTest {
    @Test fun normalComplaintCacheStartsEmpty() = runBlocking {
        assertTrue(LocalComplaintDataSource().getAllComplaints().first().isEmpty())
    }

    @Test fun normalNotificationCacheStartsEmpty() = runBlocking {
        assertTrue(LocalNotificationDataSource().getNotifications("DRV-882").first().isEmpty())
    }

    @Test fun demoComplaintsRequireExplicitOptIn() = runBlocking {
        assertTrue(LocalComplaintDataSource(demoMode = true).getAllComplaints().first().isNotEmpty())
    }
}
