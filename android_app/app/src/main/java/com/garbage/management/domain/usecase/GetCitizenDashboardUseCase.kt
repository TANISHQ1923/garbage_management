package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.CitizenDashboardData
import com.garbage.management.domain.repository.DashboardRepository
import com.garbage.management.utils.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Use case to retrieve citizen dashboard metrics, recent complaints, and unread notification summary.
 */
class GetCitizenDashboardUseCase(
    private val repository: DashboardRepository
) {
    operator fun invoke(): Flow<Resource<CitizenDashboardData>> {
        return repository.getCitizenDashboard()
    }
}
