package com.garbage.management.data.repository

import com.garbage.management.data.local.LocalDriverLocationDataSource
import com.garbage.management.data.remote.ApiClient
import com.garbage.management.data.remote.RemoteDriverDataSource
import com.garbage.management.domain.model.DriverLocation
import com.garbage.management.domain.repository.DriverLocationRepository
import com.garbage.management.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of DriverLocationRepository communicating with the
 * backend REST API while maintaining immediate local coordinate state.
 */
class DriverLocationRepositoryImpl(
    private val remoteDataSource: RemoteDriverDataSource,
    private val localDataSource: LocalDriverLocationDataSource,
    private val isLocalOnly: Boolean = false
) : DriverLocationRepository {

    constructor(localDataSource: LocalDriverLocationDataSource) : this(
        remoteDataSource = RemoteDriverDataSource(ApiClient.create()),
        localDataSource = localDataSource,
        isLocalOnly = true
    )

    override fun getDriverLocation(driverId: String): Flow<DriverLocation?> {
        if (isLocalOnly) {
            return localDataSource.getDriverLocation(driverId)
        }

        return flow {
            val cached = localDataSource.getDriverLocation(driverId).firstOrNull()
            if (cached != null) {
                emit(cached)
            }

            when (val remote = remoteDataSource.getDriverLocation(driverId)) {
                is Resource.Success -> {
                    val loc = remote.data
                    if (loc != null) {
                        localDataSource.updateDriverLocation(loc)
                        emit(loc)
                    }
                }
                is Resource.Error -> {
                    // Cached location emitted
                }
                is Resource.Loading -> Unit
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun updateDriverLocation(location: DriverLocation) = withContext(Dispatchers.IO) {
        localDataSource.updateDriverLocation(location)
        if (!isLocalOnly) {
            remoteDataSource.updateDriverLocation(
                latitude = location.latitude,
                longitude = location.longitude
            )
        }
        Unit
    }
}
