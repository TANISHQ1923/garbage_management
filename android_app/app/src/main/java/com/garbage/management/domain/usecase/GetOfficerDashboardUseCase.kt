package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.OfficerDashboardData
import com.garbage.management.domain.repository.DashboardRepository
import com.garbage.management.utils.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Use case to retrieve municipal officer dashboard breakdown, priority complaints, and driver fleet stats.
 */
class GetOfficerDashboardUseCase(
    private val repository: DashboardRepository
) {
    operator fun invoke(): Flow<Resource<OfficerDashboardData>> {
        return repository.getOfficerDashboard()
    }
}
