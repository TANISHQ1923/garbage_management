package com.garbage.management

import com.garbage.management.data.local.LocalComplaintDataSource
import com.garbage.management.data.local.LocalDriverDataSource
import com.garbage.management.data.local.LocalDriverLocationDataSource
import com.garbage.management.data.local.LocalNotificationDataSource
import com.garbage.management.data.repository.ComplaintRepositoryImpl
import com.garbage.management.data.repository.DriverLocationRepositoryImpl
import com.garbage.management.data.repository.NotificationRepositoryImpl
import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.DriverAvailability
import com.garbage.management.domain.model.DriverLocation
import com.garbage.management.domain.model.NotificationType
import com.garbage.management.domain.usecase.CompleteCollectionUseCase
import com.garbage.management.domain.usecase.GetDriverCollectionHistoryUseCase
import com.garbage.management.domain.usecase.GetDriverComplaintsUseCase
import com.garbage.management.domain.usecase.GetDriverLocationUseCase
import com.garbage.management.domain.usecase.HistoryFilter
import com.garbage.management.domain.usecase.StartCollectionUseCase
import com.garbage.management.domain.usecase.StatusTransitionValidator
import com.garbage.management.domain.usecase.UpdateDriverLocationUseCase
import com.garbage.management.utils.Resource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DriverWorkflowTest {

    private lateinit var complaintDataSource: LocalComplaintDataSource
    private lateinit var driverDataSource: LocalDriverDataSource
    private lateinit var notificationDataSource: LocalNotificationDataSource
    private lateinit var locationDataSource: LocalDriverLocationDataSource

    private lateinit var complaintRepository: ComplaintRepositoryImpl
    private lateinit var notificationRepository: NotificationRepositoryImpl
    private lateinit var locationRepository: DriverLocationRepositoryImpl

    private lateinit var startCollectionUseCase: StartCollectionUseCase
    private lateinit var completeCollectionUseCase: CompleteCollectionUseCase
    private lateinit var getDriverComplaintsUseCase: GetDriverComplaintsUseCase
    private lateinit var getDriverCollectionHistoryUseCase: GetDriverCollectionHistoryUseCase
    private lateinit var updateDriverLocationUseCase: UpdateDriverLocationUseCase
    private lateinit var getDriverLocationUseCase: GetDriverLocationUseCase

    @Before
    fun setup() {
        complaintDataSource = LocalComplaintDataSource()
        driverDataSource = LocalDriverDataSource()
        notificationDataSource = LocalNotificationDataSource()
        locationDataSource = LocalDriverLocationDataSource()

        complaintRepository = ComplaintRepositoryImpl(complaintDataSource)
        notificationRepository = NotificationRepositoryImpl(notificationDataSource)
        locationRepository = DriverLocationRepositoryImpl(locationDataSource)

        startCollectionUseCase = StartCollectionUseCase(
            complaintRepository = complaintRepository,
            notificationRepository = notificationRepository,
            driverDataSource = driverDataSource
        )
        completeCollectionUseCase = CompleteCollectionUseCase(
            complaintRepository = complaintRepository,
            notificationRepository = notificationRepository,
            driverDataSource = driverDataSource
        )
        getDriverComplaintsUseCase = GetDriverComplaintsUseCase(complaintRepository)
        getDriverCollectionHistoryUseCase = GetDriverCollectionHistoryUseCase(complaintRepository)
        updateDriverLocationUseCase = UpdateDriverLocationUseCase(locationRepository)
        getDriverLocationUseCase = GetDriverLocationUseCase(locationRepository)
    }

    @Test
    fun testStatusTransitionValidator_DriverRules() {
        // ASSIGNED -> PICKUP_IN_PROGRESS allowed for driver
        assertTrue(StatusTransitionValidator.canDriverTransition(ComplaintStatus.ASSIGNED, ComplaintStatus.PICKUP_IN_PROGRESS))

        // PICKUP_IN_PROGRESS -> CLEANED allowed for driver
        assertTrue(StatusTransitionValidator.canDriverTransition(ComplaintStatus.PICKUP_IN_PROGRESS, ComplaintStatus.CLEANED))

        // Driver cannot verify cleanup
        assertFalse(StatusTransitionValidator.canDriverTransition(ComplaintStatus.CLEANED, ComplaintStatus.VERIFIED))

        // Driver cannot reject complaints
        assertFalse(StatusTransitionValidator.canDriverTransition(ComplaintStatus.ASSIGNED, ComplaintStatus.REJECTED))

        // Driver cannot transition SUBMITTED directly
        assertFalse(StatusTransitionValidator.canDriverTransition(ComplaintStatus.SUBMITTED, ComplaintStatus.PICKUP_IN_PROGRESS))
        assertFalse(StatusTransitionValidator.canDriverTransition(ComplaintStatus.SUBMITTED, ComplaintStatus.CLEANED))
    }

    @Test
    fun testStartCollection_SuccessAndDriverStateTransition() = runBlocking {
        val complaintId = "SGM-2026-003890" // Preloaded assigned to DRV-882
        val driverId = "DRV-882"
        val driverName = "Ramesh Kumar"

        val result = startCollectionUseCase(
            complaintId = complaintId,
            driverId = driverId,
            driverName = driverName,
            beforeCleaningUri = "content://media/external/images/media/101"
        )

        assertTrue("StartCollection should succeed", result is Resource.Success)
        val updatedComplaint = (result as Resource.Success).data
        assertNotNull(updatedComplaint)
        assertEquals(ComplaintStatus.PICKUP_IN_PROGRESS, updatedComplaint?.status)
        assertEquals("content://media/external/images/media/101", updatedComplaint?.beforeCleaningImageUri)

        // Verify driver became BUSY
        val driver = driverDataSource.getDriverById(driverId)
        assertEquals(DriverAvailability.BUSY, driver?.availability)
        assertFalse(driver?.isAvailable == true)

        // Verify notification generated for citizen
        val notifications = notificationRepository.getNotifications(updatedComplaint!!.citizenId).first()
        val startNotif = notifications.find { it.type == NotificationType.PICKUP_STARTED && it.complaintId == complaintId }
        assertNotNull("Citizen should receive PICKUP_STARTED notification", startNotif)
    }

    @Test
    fun testCompleteCollection_RequiresAfterCleaningPhoto() = runBlocking {
        val complaintId = "SGM-2026-003890"
        val driverId = "DRV-882"
        val driverName = "Ramesh Kumar"

        // First start collection
        startCollectionUseCase(complaintId, driverId, driverName)

        // Attempt completion with empty photo URI -> should fail
        val failedResult = completeCollectionUseCase(
            complaintId = complaintId,
            driverId = driverId,
            driverName = driverName,
            afterCleaningUri = ""
        )

        assertTrue("CompleteCollection should fail without after photo", failedResult is Resource.Error)
        assertTrue(failedResult.message!!.contains("After-cleaning photo evidence is strictly required"))
    }

    @Test
    fun testCompleteCollection_SuccessWorkflow() = runBlocking {
        val complaintId = "SGM-2026-003890"
        val driverId = "DRV-882"
        val driverName = "Ramesh Kumar"
        val afterUri = "content://media/external/images/media/102"

        // 1. Start collection
        startCollectionUseCase(complaintId, driverId, driverName)

        // 2. Complete collection
        val result = completeCollectionUseCase(
            complaintId = complaintId,
            driverId = driverId,
            driverName = driverName,
            afterCleaningUri = afterUri
        )

        assertTrue("CompleteCollection should succeed with after photo", result is Resource.Success)
        val completedComplaint = (result as Resource.Success).data
        assertEquals(ComplaintStatus.CLEANED, completedComplaint?.status)
        assertEquals(afterUri, completedComplaint?.afterCleaningImageUri)

        // Verify driver restored to AVAILABLE
        val driver = driverDataSource.getDriverById(driverId)
        assertEquals(DriverAvailability.AVAILABLE, driver?.availability)
        assertTrue(driver?.isAvailable == true)

        // Verify citizen received CLEANUP_COMPLETED notification
        val notifications = notificationRepository.getNotifications(completedComplaint!!.citizenId).first()
        val completedNotif = notifications.find { it.type == NotificationType.CLEANUP_COMPLETED && it.complaintId == complaintId }
        assertNotNull("Citizen should receive CLEANUP_COMPLETED notification", completedNotif)

        // Complaint should NOT be automatically VERIFIED
        assertEquals(ComplaintStatus.CLEANED, completedComplaint.status)
    }

    @Test
    fun testDriverLocation_UpdateAndObserve() = runBlocking {
        val driverId = "DRV-882"
        val lat = 28.6150
        val lng = 77.2100

        updateDriverLocationUseCase(driverId, lat, lng)
        val loc = getDriverLocationUseCase(driverId).first()

        assertNotNull(loc)
        assertEquals(lat, loc!!.latitude, 0.0001)
        assertEquals(lng, loc.longitude, 0.0001)
        assertEquals(driverId, loc.driverId)
    }

    @Test
    fun testGetDriverComplaints_FiltersOnlyDriverTasks() = runBlocking {
        val complaints = getDriverComplaintsUseCase("DRV-882").first()
        assertTrue("DRV-882 should have assigned complaints", complaints.isNotEmpty())
        complaints.forEach {
            assertEquals("DRV-882", it.assignedDriverId)
        }
    }
}
