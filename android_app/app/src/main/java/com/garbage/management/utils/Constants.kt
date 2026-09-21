package com.garbage.management.utils

/**
 * Centralized application constants.
 * Configure the backend URL here to switch between Android Emulator,
 * local development machine, or production server.
 */
object Constants {
    // Android Emulator connects to localhost via 10.0.2.2
    // For physical device testing, replace with your local PC IP (e.g., http://192.168.1.X:5000/api/)
    const val BASE_URL = "http://10.0.2.2:5000/api/"

    const val TIMEOUT_SECONDS = 30L

    // Navigation Routes
    const val ROUTE_SPLASH = "splash"
    const val ROUTE_LOGIN = "login"
    const val ROUTE_REGISTER = "register"
    const val ROUTE_FORGOT_PASSWORD = "forgot_password"
    const val ROUTE_CITIZEN_HOME = "citizen_home"
    const val ROUTE_REPORT_GARBAGE = "report_garbage"
    const val ROUTE_MY_COMPLAINTS = "my_complaints"
    const val ROUTE_COMPLAINT_DETAILS = "complaint_details"
    const val ROUTE_COMPLAINT_SUBMITTED = "complaint_submitted"
    const val ROUTE_NOTIFICATIONS = "notifications"
    const val ROUTE_NOTIFICATION_PREFERENCES = "notification_preferences"
    const val ROUTE_OFFICER_HOME = "officer_home"
    const val ROUTE_OFFICER_COMPLAINTS = "officer_complaints"
    const val ROUTE_OFFICER_COMPLAINT_DETAILS = "officer_complaint_details"
    const val ROUTE_OFFICER_PROFILE = "officer_profile"
    const val ROUTE_DRIVER_HOME = "driver_home"
    const val ROUTE_DRIVER_TASKS = "driver_tasks"
    const val ROUTE_DRIVER_TASK_DETAILS = "driver_task_details"
    const val ROUTE_DRIVER_PROFILE = "driver_profile"
    const val ROUTE_DRIVER_COLLECTION_HISTORY = "driver_collection_history"
}
