package com.garbage.management.presentation.screens.citizen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.garbage.management.domain.model.AuthState
import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.presentation.components.ComplaintStatusBadge
import com.garbage.management.presentation.components.EmergencyPriorityBadge
import com.garbage.management.presentation.components.RoleBadge
import com.garbage.management.presentation.components.formatTimestamp
import com.garbage.management.presentation.components.getGarbageTypeIcon
import com.garbage.management.presentation.viewmodel.AuthViewModel
import com.garbage.management.presentation.viewmodel.ComplaintViewModel
import com.garbage.management.presentation.viewmodel.NotificationViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenHomeScreen(
    authViewModel: AuthViewModel,
    complaintViewModel: ComplaintViewModel,
    notificationViewModel: NotificationViewModel,
    onNavigateToReport: (isEmergency: Boolean) -> Unit,
    onNavigateToMyComplaints: () -> Unit,
    onNavigateToComplaintDetail: (String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val currentUser = (authState as? AuthState.Authenticated)?.user

    val complaintsState by complaintViewModel.complaintsListUiState.collectAsStateWithLifecycle()
    val notificationsState by notificationViewModel.notificationsUiState.collectAsStateWithLifecycle()
    val unreadCount = notificationsState.unreadCount

    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentUser?.id) {
        currentUser?.id?.let { citizenId ->
            complaintViewModel.loadCitizenComplaints(citizenId)
            notificationViewModel.loadNotifications(citizenId)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Hello, ${currentUser?.name?.substringBefore(" ") ?: "Citizen"}!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Smart City Citizen Portal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge {
                                    Text(text = "$unreadCount")
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(
                                imageVector = if (unreadCount > 0) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                contentDescription = "Notifications",
                                tint = if (unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    RoleBadge(
                        text = "Citizen",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(onClick = { authViewModel.logout(onLogout) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            // REAL-TIME STATUS OVERVIEW
            val activeCount = complaintsState.allComplaints.count {
                it.status == ComplaintStatus.SUBMITTED ||
                it.status == ComplaintStatus.ASSIGNED ||
                it.status == ComplaintStatus.PICKUP_IN_PROGRESS
            }
            val pendingCount = complaintsState.allComplaints.count {
                it.status == ComplaintStatus.SUBMITTED
            }
            val emergencyCount = complaintsState.allComplaints.count {
                it.isEmergency && it.status != ComplaintStatus.CLEANED && it.status != ComplaintStatus.VERIFIED
            }
            val completedCount = complaintsState.allComplaints.count {
                it.status == ComplaintStatus.CLEANED || it.status == ComplaintStatus.VERIFIED
            }

            Text(
                text = "Complaint Overview",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusSummaryMetricCard(
                    count = activeCount,
                    label = "Active Complaints",
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    textColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatusSummaryMetricCard(
                    count = pendingCount,
                    label = "Pending Requests",
                    containerColor = Color(0xFFFFF3E0),
                    textColor = Color(0xFFE65100),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusSummaryMetricCard(
                    count = emergencyCount,
                    label = "Emergency Requests",
                    containerColor = Color(0xFFFFEBEE),
                    textColor = Color(0xFFC62828),
                    modifier = Modifier.weight(1f)
                )
                StatusSummaryMetricCard(
                    count = completedCount,
                    label = "Completed",
                    containerColor = Color(0xFFE8F5E9),
                    textColor = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // QUICK ACTIONS (4 ACTIONS)
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Action 1: Report Garbage
                DashboardActionCard(
                    title = "Report Garbage",
                    subtitle = "Log waste or bin issue",
                    icon = Icons.Default.DeleteSweep,
                    badgeText = "Standard",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    onClick = { onNavigateToReport(false) },
                    modifier = Modifier.weight(1f)
                )

                // Action 2: Emergency Pickup
                DashboardActionCard(
                    title = "Emergency Pickup",
                    subtitle = "Priority hazard clearance",
                    icon = Icons.Default.NotificationsActive,
                    badgeText = "Priority",
                    containerColor = Color(0xFFFFEBEE),
                    iconTint = Color(0xFFC62828),
                    onClick = { onNavigateToReport(true) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Action 3: My Complaints
                DashboardActionCard(
                    title = "My Complaints",
                    subtitle = "Track status & history",
                    icon = Icons.AutoMirrored.Filled.Assignment,
                    badgeText = "${complaintsState.allComplaints.size} Total",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateToMyComplaints,
                    modifier = Modifier.weight(1f)
                )

                // Action 4: Notifications Center
                DashboardActionCard(
                    title = "Notifications",
                    subtitle = "Alerts & issue updates",
                    icon = if (unreadCount > 0) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                    badgeText = if (unreadCount > 0) "$unreadCount New" else "All Read",
                    containerColor = if (unreadCount > 0) Color(0xFFE0F2F1) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    iconTint = if (unreadCount > 0) Color(0xFF00695C) else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onNavigateToNotifications,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // FUTURE MODULE CARDS (Coming Soon)
            Text(
                text = "Smart City Services",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card D: Track Collection
                DashboardActionCard(
                    title = "Track Trucks",
                    subtitle = "Live garbage vehicle route",
                    icon = Icons.Default.LocalShipping,
                    badgeText = "Coming Soon",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Vehicle Tracking will be unlocked in Step 5 (IoT & Tracking).")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // Card E: Scan Dustbin QR
                DashboardActionCard(
                    title = "Scan Bin QR",
                    subtitle = "Instant smart bin check-in",
                    icon = Icons.Default.QrCodeScanner,
                    badgeText = "Coming Soon",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("QR Scanning will be unlocked in Step 5 (Smart Bins).")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Card F: Feedback
            DashboardActionCard(
                title = "Cleanliness Feedback & Rewards",
                subtitle = "Rate collection quality and earn cleanliness badges",
                icon = Icons.Default.Star,
                badgeText = "Available after cleanup",
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                iconTint = MaterialTheme.colorScheme.tertiary,
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Feedback is enabled once a complaint reaches 'Cleaned' status.")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // RECENT COMPLAINTS SECTION
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Reports",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (complaintsState.allComplaints.isNotEmpty()) {
                    Text(
                        text = "View All (${complaintsState.allComplaints.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable(onClick = onNavigateToMyComplaints)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (complaintsState.allComplaints.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No complaints yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Your submitted garbage reports will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val recentComplaints = complaintsState.allComplaints.take(5)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recentComplaints.forEach { complaint ->
                        RecentComplaintCard(
                            complaint = complaint,
                            onClick = { onNavigateToComplaintDetail(complaint.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DashboardActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeText: String,
    containerColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF2E382E),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecentComplaintCard(
    complaint: GarbageComplaint,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getGarbageTypeIcon(complaint.garbageType),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = complaint.id,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    ComplaintStatusBadge(status = complaint.status)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = complaint.garbageType.displayName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (complaint.isEmergency) {
                        Spacer(modifier = Modifier.width(6.dp))
                        EmergencyPriorityBadge(text = "Priority")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = complaint.locationDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun StatusSummaryMetricCard(
    count: Int,
    label: String,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
