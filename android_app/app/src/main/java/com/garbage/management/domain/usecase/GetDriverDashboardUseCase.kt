package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.DriverDashboardData
import com.garbage.management.domain.repository.DashboardRepository
import com.garbage.management.utils.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Use case to retrieve garbage driver dashboard tasks, metrics, and collection history.
 */
class GetDriverDashboardUseCase(
    private val repository: DashboardRepository
) {
    operator fun invoke(): Flow<Resource<DriverDashboardData>> {
        return repository.getDriverDashboard()
    }
}
