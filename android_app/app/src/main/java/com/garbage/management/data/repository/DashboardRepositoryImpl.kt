package com.garbage.management.data.repository

import com.garbage.management.data.local.LocalComplaintDataSource
import com.garbage.management.data.local.SessionManager
import com.garbage.management.data.remote.RemoteDashboardDataSource
import com.garbage.management.domain.model.CitizenDashboardData
import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.DriverAvailability
import com.garbage.management.domain.model.DriverDashboardData
import com.garbage.management.domain.model.OfficerDashboardData
import com.garbage.management.domain.repository.DashboardRepository
import com.garbage.management.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Concrete implementation of DashboardRepository connecting to the backend dashboard APIs,
 * surfacing backend errors without synthetic fallback.
 */
class DashboardRepositoryImpl(
    private val remoteDataSource: RemoteDashboardDataSource,
    private val localDataSource: LocalComplaintDataSource? = null,
    private val sessionManager: SessionManager? = null
) : DashboardRepository {

    override fun getCitizenDashboard(): Flow<Resource<CitizenDashboardData>> = flow {
        emit(Resource.Loading())
        val result = remoteDataSource.getCitizenDashboard()
        if (result is Resource.Success && result.data != null) {
            result.data.recentComplaints.forEach { localDataSource?.addOrUpdateComplaint(it) }
            result.data.allComplaints.forEach { localDataSource?.addOrUpdateComplaint(it) }
            emit(result)
        } else {
            emit(result)
        }
    }.flowOn(Dispatchers.IO)

    override fun getOfficerDashboard(): Flow<Resource<OfficerDashboardData>> = flow {
        emit(Resource.Loading())
        val result = remoteDataSource.getOfficerDashboard()
        if (result is Resource.Success && result.data != null) {
            emit(result)
        } else {
            emit(result)
        }
    }.flowOn(Dispatchers.IO)

    override fun getDriverDashboard(): Flow<Resource<DriverDashboardData>> = flow {
        emit(Resource.Loading())
        val result = remoteDataSource.getDriverDashboard()
        if (result is Resource.Success && result.data != null) {
            emit(result)
        } else {
            emit(result)
        }
    }.flowOn(Dispatchers.IO)

}
