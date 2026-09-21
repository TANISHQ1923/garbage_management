package com.garbage.management.presentation.navigation

import com.garbage.management.utils.Constants

/**
 * Sealed hierarchy defining all navigation destinations in the application.
 */
sealed class Screen(val route: String) {
    data object Splash : Screen(Constants.ROUTE_SPLASH)
    data object Login : Screen(Constants.ROUTE_LOGIN)
    data object Register : Screen(Constants.ROUTE_REGISTER)
    data object ForgotPassword : Screen(Constants.ROUTE_FORGOT_PASSWORD)
    data object CitizenHome : Screen(Constants.ROUTE_CITIZEN_HOME)

    data object ReportGarbage : Screen("${Constants.ROUTE_REPORT_GARBAGE}?isEmergency={isEmergency}") {
        fun createRoute(isEmergency: Boolean = false) = "${Constants.ROUTE_REPORT_GARBAGE}?isEmergency=$isEmergency"
    }

    data object MyComplaints : Screen(Constants.ROUTE_MY_COMPLAINTS)

    data object ComplaintDetails : Screen("${Constants.ROUTE_COMPLAINT_DETAILS}/{complaintId}") {
        fun createRoute(complaintId: String) = "${Constants.ROUTE_COMPLAINT_DETAILS}/$complaintId"
    }

    data object ComplaintSubmitted : Screen("${Constants.ROUTE_COMPLAINT_SUBMITTED}/{complaintId}") {
        fun createRoute(complaintId: String) = "${Constants.ROUTE_COMPLAINT_SUBMITTED}/$complaintId"
    }

    data object Notifications : Screen(Constants.ROUTE_NOTIFICATIONS)
    data object NotificationPreferences : Screen(Constants.ROUTE_NOTIFICATION_PREFERENCES)

    data object OfficerHome : Screen(Constants.ROUTE_OFFICER_HOME)
    data object OfficerComplaints : Screen("${Constants.ROUTE_OFFICER_COMPLAINTS}?filter={filter}") {
        fun createRoute(filter: String? = null) = if (filter.isNullOrBlank()) Constants.ROUTE_OFFICER_COMPLAINTS else "${Constants.ROUTE_OFFICER_COMPLAINTS}?filter=$filter"
    }
    data object OfficerComplaintDetails : Screen("${Constants.ROUTE_OFFICER_COMPLAINT_DETAILS}/{complaintId}") {
        fun createRoute(complaintId: String) = "${Constants.ROUTE_OFFICER_COMPLAINT_DETAILS}/$complaintId"
    }
    data object OfficerProfile : Screen(Constants.ROUTE_OFFICER_PROFILE)
    data object DriverHome : Screen(Constants.ROUTE_DRIVER_HOME)
    data object DriverTasks : Screen(Constants.ROUTE_DRIVER_TASKS)
    data object DriverTaskDetails : Screen("${Constants.ROUTE_DRIVER_TASK_DETAILS}/{complaintId}") {
        fun createRoute(complaintId: String) = "${Constants.ROUTE_DRIVER_TASK_DETAILS}/$complaintId"
    }
    data object DriverProfile : Screen(Constants.ROUTE_DRIVER_PROFILE)
    data object DriverCollectionHistory : Screen(Constants.ROUTE_DRIVER_COLLECTION_HISTORY)
}
