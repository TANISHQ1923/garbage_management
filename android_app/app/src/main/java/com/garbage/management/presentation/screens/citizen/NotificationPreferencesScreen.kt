package com.garbage.management.presentation.screens.citizen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.garbage.management.presentation.components.AppTopBar
import com.garbage.management.presentation.viewmodel.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPreferencesScreen(
    notificationViewModel: NotificationViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by notificationViewModel.preferencesUiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Notification Preferences",
                onBackClick = onNavigateBack
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Manage Alert Channels",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Choose which categories of smart garbage alerts you wish to receive.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // PREFERENCE 1: COMPLAINT UPDATES
            PreferenceSwitchCard(
                title = "Complaint Submission & Updates",
                description = "Get notified when your garbage reports are submitted and review actions begin.",
                isChecked = state.preferences.complaintUpdatesEnabled,
                onCheckedChange = { notificationViewModel.setComplaintUpdates(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // PREFERENCE 2: EMERGENCY UPDATES
            PreferenceSwitchCard(
                title = "Emergency Pickup Alerts",
                description = "Receive high-priority confirmations and municipal alerts for emergency requests.",
                isChecked = state.preferences.emergencyUpdatesEnabled,
                onCheckedChange = { notificationViewModel.setEmergencyUpdates(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // PREFERENCE 3: CLEANUP UPDATES
            PreferenceSwitchCard(
                title = "Cleanup Completion & Verification",
                description = "Receive notification when a reported site has been cleaned and verified by municipal officers.",
                isChecked = state.preferences.cleanupUpdatesEnabled,
                onCheckedChange = { notificationViewModel.setCleanupUpdates(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // PREFERENCE 4: SYSTEM NOTIFICATIONS
            PreferenceSwitchCard(
                title = "General System Notifications",
                description = "Announcements regarding smart city waste drives, schedule changes, and maintenance.",
                isChecked = state.preferences.systemNotificationsEnabled,
                onCheckedChange = { notificationViewModel.setSystemNotifications(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Preferences are stored securely on your device using DataStore. Changes take effect immediately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
fun PreferenceSwitchCard(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
