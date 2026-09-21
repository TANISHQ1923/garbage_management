package com.garbage.management

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.garbage.management.presentation.navigation.AppNavGraph
import com.garbage.management.presentation.theme.SmartGarbageTheme
import com.garbage.management.presentation.viewmodel.AuthViewModel
import com.garbage.management.presentation.viewmodel.AuthViewModelFactory
import com.garbage.management.presentation.viewmodel.ComplaintViewModel
import com.garbage.management.presentation.viewmodel.ComplaintViewModelFactory
import com.garbage.management.presentation.viewmodel.DriverViewModel
import com.garbage.management.presentation.viewmodel.DriverViewModelFactory
import com.garbage.management.presentation.viewmodel.NotificationViewModel
import com.garbage.management.presentation.viewmodel.NotificationViewModelFactory
import com.garbage.management.presentation.viewmodel.OfficerViewModel
import com.garbage.management.presentation.viewmodel.OfficerViewModelFactory

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory((application as SmartGarbageApp).appContainer)
    }

    private val complaintViewModel: ComplaintViewModel by viewModels {
        ComplaintViewModelFactory((application as SmartGarbageApp).appContainer)
    }

    private val notificationViewModel: NotificationViewModel by viewModels {
        NotificationViewModelFactory((application as SmartGarbageApp).appContainer)
    }

    private val officerViewModel: OfficerViewModel by viewModels {
        OfficerViewModelFactory((application as SmartGarbageApp).appContainer)
    }

    private val driverViewModel: DriverViewModel by viewModels {
        DriverViewModelFactory((application as SmartGarbageApp).appContainer)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as SmartGarbageApp).appContainer.systemNotificationHelper.createNotificationChannel()

        enableEdgeToEdge()
        setContent {
            SmartGarbageTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavGraph(
                        navController = navController,
                        authViewModel = authViewModel,
                        complaintViewModel = complaintViewModel,
                        notificationViewModel = notificationViewModel,
                        officerViewModel = officerViewModel,
                        driverViewModel = driverViewModel
                    )
                }
            }
        }
    }
}
