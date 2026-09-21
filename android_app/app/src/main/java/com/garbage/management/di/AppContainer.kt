package com.garbage.management.di

import android.content.Context
import com.garbage.management.data.local.LocalComplaintDataSource
import com.garbage.management.data.local.LocalDriverDataSource
import com.garbage.management.data.local.LocalDriverLocationDataSource
import com.garbage.management.data.local.LocalNotificationDataSource
import com.garbage.management.data.local.MockAuthDataSource
import com.garbage.management.data.local.NotificationPreferencesManager
import com.garbage.management.data.local.SessionManager
import com.garbage.management.data.remote.ApiClient
import com.garbage.management.data.remote.ApiService
import com.garbage.management.data.remote.RemoteAuthDataSource
import com.garbage.management.data.remote.RemoteComplaintDataSource
import com.garbage.management.data.remote.RemoteDriverDataSource
import com.garbage.management.data.remote.RemoteMediaDataSource
import com.garbage.management.data.remote.RemoteNotificationDataSource
import com.garbage.management.data.remote.RemoteOfficerDataSource
import com.garbage.management.data.repository.AuthRepositoryImpl
import com.garbage.management.data.repository.ComplaintRepositoryImpl
import com.garbage.management.data.repository.DriverLocationRepositoryImpl
import com.garbage.management.data.repository.MediaRepositoryImpl
import com.garbage.management.data.repository.NotificationRepositoryImpl
import com.garbage.management.domain.repository.AuthRepository
import com.garbage.management.domain.repository.ComplaintRepository
import com.garbage.management.domain.repository.DriverLocationRepository
import com.garbage.management.domain.repository.MediaRepository
import com.garbage.management.domain.repository.NotificationRepository
import com.garbage.management.domain.usecase.AddOfficerNoteUseCase
import com.garbage.management.domain.usecase.AssignDriverUseCase
import com.garbage.management.domain.usecase.CompleteCollectionUseCase
import com.garbage.management.domain.usecase.CreateNotificationUseCase
import com.garbage.management.domain.usecase.GetAllComplaintsUseCase
import com.garbage.management.domain.usecase.GetAvailableDriversUseCase
import com.garbage.management.domain.usecase.GetCitizenComplaintsUseCase
import com.garbage.management.domain.usecase.GetComplaintByIdUseCase
import com.garbage.management.domain.usecase.GetDriverCollectionHistoryUseCase
import com.garbage.management.domain.usecase.GetDriverComplaintsUseCase
import com.garbage.management.domain.usecase.GetDriverLocationUseCase
import com.garbage.management.domain.usecase.GetNotificationsUseCase
import com.garbage.management.domain.usecase.GetUnreadNotificationCountUseCase
import com.garbage.management.domain.usecase.LoginUseCase
import com.garbage.management.domain.usecase.LogoutUseCase
import com.garbage.management.domain.usecase.MarkAllNotificationsReadUseCase
import com.garbage.management.domain.usecase.MarkNotificationReadUseCase
import com.garbage.management.domain.usecase.RegisterUseCase
import com.garbage.management.domain.usecase.StartCollectionUseCase
import com.garbage.management.domain.usecase.SubmitComplaintUseCase
import com.garbage.management.domain.usecase.UpdateComplaintStatusUseCase
import com.garbage.management.domain.usecase.UpdateDriverLocationUseCase
import com.garbage.management.utils.SystemNotificationHelper

/**
 * Dependency container providing singletons for network, storage, repositories, and use cases.
 * Connects the Android presentation layer to the Node.js + MongoDB Atlas REST API backend.
 */
class AppContainer(private val context: Context) {

    // =========================================================================
    // LOCAL STORAGE & SESSION MANAGEMENT
    // =========================================================================
    val sessionManager: SessionManager by lazy {
        SessionManager(context)
    }

    val mockAuthDataSource: MockAuthDataSource by lazy {
        MockAuthDataSource()
    }

    val notificationPreferencesManager: NotificationPreferencesManager by lazy {
        NotificationPreferencesManager(context)
    }

    val systemNotificationHelper: SystemNotificationHelper by lazy {
        SystemNotificationHelper(context)
    }

    // =========================================================================
    // NETWORKING & REMOTE DATA SOURCES
    // =========================================================================
    val apiService: ApiService by lazy {
        ApiClient.create(sessionManager = sessionManager)
    }

    val remoteAuthDataSource: RemoteAuthDataSource by lazy {
        RemoteAuthDataSource(apiService = apiService)
    }

    val remoteComplaintDataSource: RemoteComplaintDataSource by lazy {
        RemoteComplaintDataSource(apiService = apiService)
    }

    val remoteOfficerDataSource: RemoteOfficerDataSource by lazy {
        RemoteOfficerDataSource(apiService = apiService)
    }

    val remoteDriverDataSource: RemoteDriverDataSource by lazy {
        RemoteDriverDataSource(apiService = apiService)
    }

    val remoteNotificationDataSource: RemoteNotificationDataSource by lazy {
        RemoteNotificationDataSource(apiService = apiService)
    }

    val remoteMediaDataSource: RemoteMediaDataSource by lazy {
        RemoteMediaDataSource(apiService = apiService)
    }

    // Local data sources retained for offline rendering & caching
    val localComplaintDataSource: LocalComplaintDataSource by lazy {
        LocalComplaintDataSource()
    }

    val localNotificationDataSource: LocalNotificationDataSource by lazy {
        LocalNotificationDataSource()
    }

    val localDriverDataSource: LocalDriverDataSource by lazy {
        LocalDriverDataSource()
    }

    val localDriverLocationDataSource: LocalDriverLocationDataSource by lazy {
        LocalDriverLocationDataSource()
    }

    // =========================================================================
    // REPOSITORIES (CONNECTED TO REST API)
    // =========================================================================
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            remoteDataSource = remoteAuthDataSource,
            sessionManager = sessionManager,
            apiService = apiService,
            mockDataSource = mockAuthDataSource
        )
    }

    val complaintRepository: ComplaintRepository by lazy {
        ComplaintRepositoryImpl(
            remoteComplaintDataSource = remoteComplaintDataSource,
            remoteOfficerDataSource = remoteOfficerDataSource,
            remoteDriverDataSource = remoteDriverDataSource,
            localDataSource = localComplaintDataSource
        )
    }

    val notificationRepository: NotificationRepository by lazy {
        NotificationRepositoryImpl(
            remoteDataSource = remoteNotificationDataSource,
            localDataSource = localNotificationDataSource
        )
    }

    val driverLocationRepository: DriverLocationRepository by lazy {
        DriverLocationRepositoryImpl(
            remoteDataSource = remoteDriverDataSource,
            localDataSource = localDriverLocationDataSource
        )
    }

    val mediaRepository: MediaRepository by lazy {
        MediaRepositoryImpl(remoteMediaDataSource = remoteMediaDataSource)
    }

    // =========================================================================
    // AUTHENTICATION USE CASES
    // =========================================================================
    val loginUseCase: LoginUseCase by lazy {
        LoginUseCase(repository = authRepository)
    }

    val registerUseCase: RegisterUseCase by lazy {
        RegisterUseCase(repository = authRepository)
    }

    val logoutUseCase: LogoutUseCase by lazy {
        LogoutUseCase(repository = authRepository)
    }

    // =========================================================================
    // CITIZEN COMPLAINT USE CASES
    // =========================================================================
    val submitComplaintUseCase: SubmitComplaintUseCase by lazy {
        SubmitComplaintUseCase(repository = complaintRepository)
    }

    val getCitizenComplaintsUseCase: GetCitizenComplaintsUseCase by lazy {
        GetCitizenComplaintsUseCase(repository = complaintRepository)
    }

    val getComplaintByIdUseCase: GetComplaintByIdUseCase by lazy {
        GetComplaintByIdUseCase(repository = complaintRepository)
    }

    // =========================================================================
    // NOTIFICATION USE CASES
    // =========================================================================
    val getNotificationsUseCase: GetNotificationsUseCase by lazy {
        GetNotificationsUseCase(repository = notificationRepository)
    }

    val createNotificationUseCase: CreateNotificationUseCase by lazy {
        CreateNotificationUseCase(repository = notificationRepository)
    }

    val markNotificationReadUseCase: MarkNotificationReadUseCase by lazy {
        MarkNotificationReadUseCase(repository = notificationRepository)
    }

    val markAllNotificationsReadUseCase: MarkAllNotificationsReadUseCase by lazy {
        MarkAllNotificationsReadUseCase(repository = notificationRepository)
    }

    val getUnreadNotificationCountUseCase: GetUnreadNotificationCountUseCase by lazy {
        GetUnreadNotificationCountUseCase(repository = notificationRepository)
    }

    // =========================================================================
    // MUNICIPAL OFFICER USE CASES
    // =========================================================================
    val getAllComplaintsUseCase: GetAllComplaintsUseCase by lazy {
        GetAllComplaintsUseCase(repository = complaintRepository)
    }

    val getAvailableDriversUseCase: GetAvailableDriversUseCase by lazy {
        GetAvailableDriversUseCase(driverDataSource = localDriverDataSource)
    }

    val assignDriverUseCase: AssignDriverUseCase by lazy {
        AssignDriverUseCase(
            complaintRepository = complaintRepository,
            notificationRepository = notificationRepository
        )
    }

    val updateComplaintStatusUseCase: UpdateComplaintStatusUseCase by lazy {
        UpdateComplaintStatusUseCase(
            complaintRepository = complaintRepository,
            notificationRepository = notificationRepository
        )
    }

    val addOfficerNoteUseCase: AddOfficerNoteUseCase by lazy {
        AddOfficerNoteUseCase(complaintRepository = complaintRepository)
    }

    // =========================================================================
    // GARBAGE DRIVER USE CASES
    // =========================================================================
    val getDriverComplaintsUseCase: GetDriverComplaintsUseCase by lazy {
        GetDriverComplaintsUseCase(repository = complaintRepository)
    }

    val startCollectionUseCase: StartCollectionUseCase by lazy {
        StartCollectionUseCase(
            complaintRepository = complaintRepository,
            notificationRepository = notificationRepository,
            driverDataSource = localDriverDataSource
        )
    }

    val completeCollectionUseCase: CompleteCollectionUseCase by lazy {
        CompleteCollectionUseCase(
            complaintRepository = complaintRepository,
            notificationRepository = notificationRepository,
            driverDataSource = localDriverDataSource
        )
    }

    val getDriverCollectionHistoryUseCase: GetDriverCollectionHistoryUseCase by lazy {
        GetDriverCollectionHistoryUseCase(repository = complaintRepository)
    }

    val updateDriverLocationUseCase: UpdateDriverLocationUseCase by lazy {
        UpdateDriverLocationUseCase(repository = driverLocationRepository)
    }

    val getDriverLocationUseCase: GetDriverLocationUseCase by lazy {
        GetDriverLocationUseCase(repository = driverLocationRepository)
    }
}
