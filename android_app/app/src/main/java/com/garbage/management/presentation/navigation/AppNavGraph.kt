package com.garbage.management.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.garbage.management.domain.model.AuthState
import com.garbage.management.domain.model.UserRole
import com.garbage.management.presentation.screens.auth.ForgotPasswordScreen
import com.garbage.management.presentation.screens.auth.LoginScreen
import com.garbage.management.presentation.screens.auth.RegisterScreen
import com.garbage.management.presentation.screens.citizen.CitizenHomeScreen
import com.garbage.management.presentation.screens.citizen.ComplaintDetailsScreen
import com.garbage.management.presentation.screens.citizen.ComplaintSubmittedScreen
import com.garbage.management.presentation.screens.citizen.MyComplaintsScreen
import com.garbage.management.presentation.screens.citizen.NotificationPreferencesScreen
import com.garbage.management.presentation.screens.citizen.NotificationsScreen
import com.garbage.management.presentation.screens.citizen.ReportGarbageScreen
import com.garbage.management.presentation.screens.driver.DriverCollectionHistoryScreen
import com.garbage.management.presentation.screens.driver.DriverHomeScreen
import com.garbage.management.presentation.screens.driver.DriverProfileScreen
import com.garbage.management.presentation.screens.driver.DriverTaskDetailsScreen
import com.garbage.management.presentation.screens.driver.DriverTasksScreen
import com.garbage.management.presentation.screens.officer.OfficerComplaintDetailsScreen
import com.garbage.management.presentation.screens.officer.OfficerComplaintsScreen
import com.garbage.management.presentation.screens.officer.OfficerHomeScreen
import com.garbage.management.presentation.screens.officer.OfficerProfileScreen
import com.garbage.management.presentation.screens.splash.SplashScreen
import com.garbage.management.presentation.viewmodel.AuthViewModel
import com.garbage.management.presentation.viewmodel.ComplaintViewModel
import com.garbage.management.presentation.viewmodel.DriverViewModel
import com.garbage.management.presentation.viewmodel.NotificationViewModel
import com.garbage.management.presentation.viewmodel.OfficerViewModel
import com.garbage.management.utils.Constants

@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    complaintViewModel: ComplaintViewModel,
    notificationViewModel: NotificationViewModel,
    officerViewModel: OfficerViewModel,
    driverViewModel: DriverViewModel,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // SPLASH
        composable(route = Screen.Splash.route) {
            SplashScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToRoleHome = { role ->
                    val targetRoute = when (role) {
                        UserRole.CITIZEN -> Screen.CitizenHome.route
                        UserRole.MUNICIPAL_OFFICER -> Screen.OfficerHome.route
                        UserRole.GARBAGE_DRIVER -> Screen.DriverHome.route
                    }
                    navController.navigate(targetRoute) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // LOGIN
        composable(route = Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { user ->
                    val targetRoute = when (user.role) {
                        UserRole.CITIZEN -> Screen.CitizenHome.route
                        UserRole.MUNICIPAL_OFFICER -> Screen.OfficerHome.route
                        UserRole.GARBAGE_DRIVER -> Screen.DriverHome.route
                    }
                    navController.navigate(targetRoute) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }

        // REGISTER
        composable(route = Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // FORGOT PASSWORD
        composable(route = Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // CITIZEN DASHBOARD
        composable(route = Screen.CitizenHome.route) {
            CitizenHomeScreen(
                authViewModel = authViewModel,
                complaintViewModel = complaintViewModel,
                notificationViewModel = notificationViewModel,
                onNavigateToReport = { isEmergency ->
                    navController.navigate(Screen.ReportGarbage.createRoute(isEmergency))
                },
                onNavigateToMyComplaints = {
                    navController.navigate(Screen.MyComplaints.route)
                },
                onNavigateToComplaintDetail = { complaintId ->
                    navController.navigate(Screen.ComplaintDetails.createRoute(complaintId))
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.CitizenHome.route) { inclusive = true }
                    }
                }
            )
        }

        // REPORT GARBAGE FORM
        composable(
            route = Screen.ReportGarbage.route,
            arguments = listOf(
                navArgument("isEmergency") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val isEmergency = backStackEntry.arguments?.getBoolean("isEmergency") ?: false
            ReportGarbageScreen(
                authViewModel = authViewModel,
                complaintViewModel = complaintViewModel,
                initialEmergency = isEmergency,
                onReportSubmitted = { complaintId ->
                    navController.navigate(Screen.ComplaintSubmitted.createRoute(complaintId)) {
                        popUpTo(Constants.ROUTE_CITIZEN_HOME) { inclusive = false }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // COMPLAINT SUBMISSION CELEBRATION
        composable(
            route = Screen.ComplaintSubmitted.route,
            arguments = listOf(
                navArgument("complaintId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
            ComplaintSubmittedScreen(
                complaintId = complaintId,
                viewModel = complaintViewModel,
                onViewComplaint = { id ->
                    navController.navigate(Screen.ComplaintDetails.createRoute(id)) {
                        popUpTo(Screen.CitizenHome.route) { inclusive = false }
                    }
                },
                onBackToDashboard = {
                    navController.navigate(Screen.CitizenHome.route) {
                        popUpTo(Screen.CitizenHome.route) { inclusive = false }
                    }
                }
            )
        }

        // MY COMPLAINTS LIST
        composable(route = Screen.MyComplaints.route) {
            MyComplaintsScreen(
                authViewModel = authViewModel,
                complaintViewModel = complaintViewModel,
                onNavigateToComplaintDetail = { complaintId ->
                    navController.navigate(Screen.ComplaintDetails.createRoute(complaintId))
                },
                onNavigateToReport = {
                    navController.navigate(Screen.ReportGarbage.createRoute(false))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // COMPLAINT DETAILS (CITIZEN VIEW)
        composable(
            route = Screen.ComplaintDetails.route,
            arguments = listOf(
                navArgument("complaintId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
            ComplaintDetailsScreen(
                complaintId = complaintId,
                viewModel = complaintViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // CITIZEN NOTIFICATIONS CENTER
        composable(route = Screen.Notifications.route) {
            NotificationsScreen(
                authViewModel = authViewModel,
                notificationViewModel = notificationViewModel,
                onNavigateToComplaintDetail = { complaintId ->
                    navController.navigate(Screen.ComplaintDetails.createRoute(complaintId))
                },
                onNavigateToPreferences = {
                    navController.navigate(Screen.NotificationPreferences.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // NOTIFICATION PREFERENCES
        composable(route = Screen.NotificationPreferences.route) {
            NotificationPreferencesScreen(
                notificationViewModel = notificationViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // -----------------------------------------------------------------------------------------
        // MUNICIPAL OFFICER PORTAL ROUTES (GUARDED BY ROLE ACCESS)
        // -----------------------------------------------------------------------------------------

        // MUNICIPAL OFFICER HOME / CONTROL CENTER
        composable(route = Screen.OfficerHome.route) {
            OfficerRouteGuard(authViewModel = authViewModel, navController = navController) {
                OfficerHomeScreen(
                    authViewModel = authViewModel,
                    officerViewModel = officerViewModel,
                    onNavigateToAllComplaints = { filter ->
                        navController.navigate(Screen.OfficerComplaints.createRoute(filter))
                    },
                    onNavigateToComplaintDetail = { complaintId ->
                        navController.navigate(Screen.OfficerComplaintDetails.createRoute(complaintId))
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.OfficerProfile.route)
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.OfficerHome.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        // ALL CITIZEN COMPLAINTS (OFFICER VIEW)
        composable(
            route = Screen.OfficerComplaints.route,
            arguments = listOf(
                navArgument("filter") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val filter = backStackEntry.arguments?.getString("filter")
            OfficerRouteGuard(authViewModel = authViewModel, navController = navController) {
                OfficerComplaintsScreen(
                    viewModel = officerViewModel,
                    initialFilter = filter,
                    onNavigateToComplaintDetail = { complaintId ->
                        navController.navigate(Screen.OfficerComplaintDetails.createRoute(complaintId))
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // OFFICER COMPLAINT DETAILS & ACTIONS
        composable(
            route = Screen.OfficerComplaintDetails.route,
            arguments = listOf(
                navArgument("complaintId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
            OfficerRouteGuard(authViewModel = authViewModel, navController = navController) {
                OfficerComplaintDetailsScreen(
                    complaintId = complaintId,
                    authViewModel = authViewModel,
                    officerViewModel = officerViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // OFFICER PROFILE
        composable(route = Screen.OfficerProfile.route) {
            OfficerRouteGuard(authViewModel = authViewModel, navController = navController) {
                OfficerProfileScreen(
                    authViewModel = authViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.OfficerHome.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        // -----------------------------------------------------------------------------------------
        // GARBAGE DRIVER PORTAL ROUTES (GUARDED BY ROLE ACCESS)
        // -----------------------------------------------------------------------------------------

        // DRIVER DASHBOARD
        composable(route = Screen.DriverHome.route) {
            DriverRouteGuard(authViewModel = authViewModel, navController = navController) {
                DriverHomeScreen(
                    authViewModel = authViewModel,
                    driverViewModel = driverViewModel,
                    notificationViewModel = notificationViewModel,
                    onNavigateToTasks = {
                        navController.navigate(Screen.DriverTasks.route)
                    },
                    onNavigateToTaskDetails = { complaintId ->
                        navController.navigate(Screen.DriverTaskDetails.createRoute(complaintId))
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.DriverCollectionHistory.route)
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.DriverProfile.route)
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Screen.Notifications.route)
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.DriverHome.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        // DRIVER ASSIGNED TASKS LIST
        composable(route = Screen.DriverTasks.route) {
            DriverRouteGuard(authViewModel = authViewModel, navController = navController) {
                DriverTasksScreen(
                    authViewModel = authViewModel,
                    driverViewModel = driverViewModel,
                    onNavigateToTaskDetails = { complaintId ->
                        navController.navigate(Screen.DriverTaskDetails.createRoute(complaintId))
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // DRIVER TASK DETAILS & COLLECTION WORKFLOW
        composable(
            route = Screen.DriverTaskDetails.route,
            arguments = listOf(
                navArgument("complaintId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
            DriverRouteGuard(authViewModel = authViewModel, navController = navController) {
                DriverTaskDetailsScreen(
                    complaintId = complaintId,
                    authViewModel = authViewModel,
                    driverViewModel = driverViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // DRIVER PROFILE & VEHICLE MANAGEMENT
        composable(route = Screen.DriverProfile.route) {
            DriverRouteGuard(authViewModel = authViewModel, navController = navController) {
                DriverProfileScreen(
                    authViewModel = authViewModel,
                    driverViewModel = driverViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.DriverHome.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        // DRIVER DAILY COLLECTION HISTORY
        composable(route = Screen.DriverCollectionHistory.route) {
            DriverRouteGuard(authViewModel = authViewModel, navController = navController) {
                DriverCollectionHistoryScreen(
                    authViewModel = authViewModel,
                    driverViewModel = driverViewModel,
                    onNavigateToTaskDetails = { complaintId ->
                        navController.navigate(Screen.DriverTaskDetails.createRoute(complaintId))
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

/**
 * Navigation guard ensuring that only users with MUNICIPAL_OFFICER role can view municipal screens.
 * Redirects unauthorized users to their respective home screen or login.
 */
@Composable
private fun OfficerRouteGuard(
    authViewModel: AuthViewModel,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val currentUser = (authState as? AuthState.Authenticated)?.user

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Unauthenticated -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthState.Authenticated -> {
                if (currentUser?.role != UserRole.MUNICIPAL_OFFICER) {
                    val fallback = when (currentUser?.role) {
                        UserRole.CITIZEN -> Screen.CitizenHome.route
                        UserRole.GARBAGE_DRIVER -> Screen.DriverHome.route
                        else -> Screen.Login.route
                    }
                    navController.navigate(fallback) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> Unit
        }
    }

    if (currentUser?.role == UserRole.MUNICIPAL_OFFICER) {
        content()
    }
}

/**
 * Navigation guard ensuring that only users with GARBAGE_DRIVER role can view driver screens.
 * Redirects unauthorized users to their respective home screen or login.
 */
@Composable
private fun DriverRouteGuard(
    authViewModel: AuthViewModel,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val currentUser = (authState as? AuthState.Authenticated)?.user

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Unauthenticated -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            is AuthState.Authenticated -> {
                if (currentUser?.role != UserRole.GARBAGE_DRIVER) {
                    val fallback = when (currentUser?.role) {
                        UserRole.CITIZEN -> Screen.CitizenHome.route
                        UserRole.MUNICIPAL_OFFICER -> Screen.OfficerHome.route
                        else -> Screen.Login.route
                    }
                    navController.navigate(fallback) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> Unit
        }
    }

    if (currentUser?.role == UserRole.GARBAGE_DRIVER) {
        content()
    }
}
