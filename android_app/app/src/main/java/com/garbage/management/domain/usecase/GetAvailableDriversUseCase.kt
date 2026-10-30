package com.garbage.management.domain.usecase

import com.garbage.management.data.remote.RemoteOfficerDataSource
import com.garbage.management.domain.model.GarbageDriver
import com.garbage.management.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetAvailableDriversUseCase(private val remoteDataSource: RemoteOfficerDataSource) {
    operator fun invoke(onlyAvailable: Boolean = false): Flow<List<GarbageDriver>> = flow {
        when (val result = remoteDataSource.getDrivers()) {
            is Resource.Success -> emit((result.data ?: emptyList()).filter { !onlyAvailable || it.isAvailable })
            is Resource.Error -> throw IllegalStateException(result.message ?: "Unable to load drivers.")
            is Resource.Loading -> Unit
        }
    }
}
