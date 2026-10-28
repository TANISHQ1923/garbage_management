package com.garbage.management.domain.repository

import com.garbage.management.domain.model.CitizenDashboardData
import com.garbage.management.domain.model.DriverDashboardData
import com.garbage.management.domain.model.OfficerDashboardData
import com.garbage.management.utils.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for retrieving aggregated dashboard data.
 */
interface DashboardRepository {
    fun getCitizenDashboard(): Flow<Resource<CitizenDashboardData>>
    fun getOfficerDashboard(): Flow<Resource<OfficerDashboardData>>
    fun getDriverDashboard(): Flow<Resource<DriverDashboardData>>
}
