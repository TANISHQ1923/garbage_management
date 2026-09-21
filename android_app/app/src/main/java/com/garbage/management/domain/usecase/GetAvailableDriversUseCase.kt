package com.garbage.management.domain.usecase

import com.garbage.management.data.local.LocalDriverDataSource
import com.garbage.management.domain.model.GarbageDriver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case to query active waste collection drivers for vehicle assignment.
 */
class GetAvailableDriversUseCase(
    private val driverDataSource: LocalDriverDataSource
) {
    operator fun invoke(onlyAvailable: Boolean = false): Flow<List<GarbageDriver>> {
        return driverDataSource.getAllDrivers().map { list ->
            if (onlyAvailable) list.filter { it.isAvailable } else list
        }
    }
}
