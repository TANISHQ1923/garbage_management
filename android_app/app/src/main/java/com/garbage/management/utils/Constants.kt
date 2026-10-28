package com.garbage.management.utils

/**
 * Centralized application constants.
 * Configure the backend URL here to switch between Android Emulator,
 * local development machine, or production server.
 */
object Constants {
    /**
     * Development host machine IP for physical devices connected via Wi-Fi.
     * Update this if your machine's Wi-Fi IP address changes.
     */
    const val DEV_MACHINE_IP = "10.20.10.221"
    const val SERVER_PORT = 5000

    /**
     * Automatically adapts the backend URL:
     * - Android Emulator connects to host loopback via 10.0.2.2.
     * - Physical Device connected via Wi-Fi connects to the development machine IP (DEV_MACHINE_IP).
     * - Physical Device connected via USB connects via 127.0.0.1 (via `adb reverse tcp:5000 tcp:5000`).
     */
    val BASE_URL: String
        get() = if (isEmulator()) {
            "http://10.0.2.2:$SERVER_PORT/api/"
        } else {
            "http://$DEV_MACHINE_IP:$SERVER_PORT/api/"
        }

    fun isEmulator(): Boolean {
        val build = (android.os.Build.FINGERPRINT ?: "").lowercase()
        val model = (android.os.Build.MODEL ?: "").lowercase()
        val brand = (android.os.Build.BRAND ?: "").lowercase()
        val device = (android.os.Build.DEVICE ?: "").lowercase()
        val product = (android.os.Build.PRODUCT ?: "").lowercase()
        val hardware = (android.os.Build.HARDWARE ?: "").lowercase()
        val manufacturer = (android.os.Build.MANUFACTURER ?: "").lowercase()

        return build.startsWith("generic")
                || build.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("emulator")
                || model.contains("android sdk built for")
                || model.contains("sdk")
                || (brand.startsWith("generic") && device.startsWith("generic"))
                || product.contains("sdk_google")
                || product.contains("google_sdk")
                || product.contains("sdk")
                || product.contains("emulator")
                || product.contains("simulator")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || manufacturer.contains("genymotion")
    }

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
