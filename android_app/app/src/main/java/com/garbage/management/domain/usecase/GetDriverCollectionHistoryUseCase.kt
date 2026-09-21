package com.garbage.management.domain.usecase

import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.repository.ComplaintRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

enum class HistoryFilter(val displayName: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    ALL("All Completed")
}

/**
 * Use case to retrieve completed garbage collections handled by a driver,
 * supporting timeframe filtering by Today, Yesterday, and This Week.
 */
class GetDriverCollectionHistoryUseCase(
    private val repository: ComplaintRepository
) {
    operator fun invoke(driverId: String, filter: HistoryFilter = HistoryFilter.TODAY): Flow<List<GarbageComplaint>> {
        return repository.getDriverComplaints(driverId).map { complaints ->
            val completed = complaints.filter {
                it.status == ComplaintStatus.CLEANED || it.status == ComplaintStatus.VERIFIED
            }

            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()

            // Start of today
            cal.timeInMillis = now
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startOfToday = cal.timeInMillis

            // Start of yesterday
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val startOfYesterday = cal.timeInMillis

            // Start of this week (7 days ago)
            val startOfWeek = startOfToday - (6 * 86400000L)

            val filtered = when (filter) {
                HistoryFilter.TODAY -> completed.filter { it.updatedAt >= startOfToday }
                HistoryFilter.YESTERDAY -> completed.filter { it.updatedAt in startOfYesterday until startOfToday }
                HistoryFilter.THIS_WEEK -> completed.filter { it.updatedAt >= startOfWeek }
                HistoryFilter.ALL -> completed
            }

            filtered.sortedByDescending { it.updatedAt }
        }
    }
}
