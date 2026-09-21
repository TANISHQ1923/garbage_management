package com.garbage.management.presentation.screens.officer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.garbage.management.presentation.components.EmergencyPriorityBadge
import com.garbage.management.presentation.components.RoleBadge
import com.garbage.management.presentation.components.formatTimestamp
import com.garbage.management.presentation.components.getGarbageTypeIcon
import com.garbage.management.presentation.viewmodel.AuthViewModel
import com.garbage.management.presentation.viewmodel.OfficerMetrics
import com.garbage.management.presentation.viewmodel.OfficerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficerHomeScreen(
    authViewModel: AuthViewModel,
    officerViewModel: OfficerViewModel,
    onNavigateToAllComplaints: (filter: String?) -> Unit,
    onNavigateToComplaintDetail: (complaintId: String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val currentUser = (authState as? AuthState.Authenticated)?.user
    val dashboardState by officerViewModel.dashboardState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Municipal Control Center",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = currentUser?.ward ?: "Central Municipal Zone",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Officer Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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
        if (dashboardState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. OFFICER IDENTITY HEADER
                item {
                    OfficerHeaderCard(
                        officerName = currentUser?.name ?: "Municipal Officer",
                        employeeId = currentUser?.employeeId ?: "MC-OFF-2026",
                        ward = currentUser?.ward ?: "Ward 12"
                    )
                }

                // 2. SUMMARY METRICS
                item {
                    Text(
                        text = "Municipal Overview",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OfficerMetricsGrid(
                        metrics = dashboardState.metrics,
                        onMetricClick = { filter -> onNavigateToAllComplaints(filter) }
                    )
                }

                // 3. PRIORITY & EMERGENCY REQUESTS (Rule-based local operational priority)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Priority & Emergency Requests",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        TextButton(onClick = { onNavigateToAllComplaints("Emergency") }) {
                            Text("View All (${dashboardState.priorityComplaints.size})")
                        }
                    }

                    if (dashboardState.priorityComplaints.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No active emergency or high-priority requests",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(dashboardState.priorityComplaints.take(5)) { complaint ->
                                OfficerPriorityCard(
                                    complaint = complaint,
                                    onClick = { onNavigateToComplaintDetail(complaint.id) }
                                )
                            }
                        }
                    }
                }

                // 4. PENDING ASSIGNMENTS (Complaints in SUBMITTED state waiting for driver)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PendingActions,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pending Assignment",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        TextButton(onClick = { onNavigateToAllComplaints("Submitted") }) {
                            Text("View All (${dashboardState.pendingAssignments.size})")
                        }
                    }

                    if (dashboardState.pendingAssignments.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "All submitted complaints have been assigned",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            dashboardState.pendingAssignments.take(3).forEach { complaint ->
                                OfficerComplaintListItem(
                                    complaint = complaint,
                                    onClick = { onNavigateToComplaintDetail(complaint.id) }
                                )
                            }
                        }
                    }
                }

                // 5. RECENT COMPLAINTS
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Submissions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        TextButton(onClick = { onNavigateToAllComplaints("All") }) {
                            Text("See All")
                        }
                    }

                    if (dashboardState.recentComplaints.isEmpty()) {
                        Text(
                            text = "No complaints available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            dashboardState.recentComplaints.forEach { complaint ->
                                OfficerComplaintListItem(
                                    complaint = complaint,
                                    onClick = { onNavigateToComplaintDetail(complaint.id) }
                                )
                            }
                        }
                    }
                }

                // 6. QUICK ACTIONS
                item {
                    Text(
                        text = "Quick Operations",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OfficerQuickActionTile(
                            title = "All Citizen Complaints",
                            subtitle = "Browse, search, and filter all complaints across zones",
                            icon = Icons.AutoMirrored.Filled.Assignment,
                            onClick = { onNavigateToAllComplaints("All") }
                        )
                        OfficerQuickActionTile(
                            title = "Emergency Operations",
                            subtitle = "Review flagged priority and high-hazard incidents",
                            icon = Icons.Default.Report,
                            iconTint = MaterialTheme.colorScheme.error,
                            onClick = { onNavigateToAllComplaints("Emergency") }
                        )
                        OfficerQuickActionTile(
                            title = "Dispatch & Pending Vehicles",
                            subtitle = "Assign collection trucks to newly submitted reports",
                            icon = Icons.Default.LocalShipping,
                            onClick = { onNavigateToAllComplaints("Submitted") }
                        )
                        OfficerQuickActionTile(
                            title = "Officer Profile & Authority",
                            subtitle = "View municipal identification, ward jurisdiction, and session",
                            icon = Icons.Default.Shield,
                            onClick = onNavigateToProfile
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OfficerHeaderCard(
    officerName: String,
    employeeId: String,
    ward: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Engineering,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = officerName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    RoleBadge(text = "Officer")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Emp ID: $employeeId • $ward",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun OfficerMetricsGrid(
    metrics: OfficerMetrics,
    onMetricClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OfficerMetricTile(
                title = "TOTAL",
                count = metrics.total,
                icon = Icons.AutoMirrored.Filled.Assignment,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                onClick = { onMetricClick("All") },
                modifier = Modifier.weight(1f)
            )
            OfficerMetricTile(
                title = "EMERGENCY",
                count = metrics.emergency,
                icon = Icons.Default.Warning,
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.error,
                onClick = { onMetricClick("Emergency") },
                modifier = Modifier.weight(1f)
            )
            OfficerMetricTile(
                title = "SUBMITTED",
                count = metrics.submitted,
                icon = Icons.Default.PendingActions,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                onClick = { onMetricClick("Submitted") },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OfficerMetricTile(
                title = "ASSIGNED",
                count = metrics.assigned,
                icon = Icons.Default.AssignmentInd,
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                onClick = { onMetricClick("Assigned") },
                modifier = Modifier.weight(1f)
            )
            OfficerMetricTile(
                title = "IN PROGRESS",
                count = metrics.inProgress,
                icon = Icons.Default.LocalShipping,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                onClick = { onMetricClick("In Progress") },
                modifier = Modifier.weight(1f)
            )
            OfficerMetricTile(
                title = "CLEANED",
                count = metrics.cleaned,
                icon = Icons.Default.CheckCircle,
                containerColor = Color(0xFFE8F5E9),
                contentColor = Color(0xFF2E7D32),
                onClick = { onMetricClick("Cleaned") },
                modifier = Modifier.weight(1f)
            )
            OfficerMetricTile(
                title = "VERIFIED",
                count = metrics.verified,
                icon = Icons.Default.Verified,
                containerColor = Color(0xFFE0F2F1),
                contentColor = Color(0xFF00695C),
                onClick = { onMetricClick("Verified") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun OfficerMetricTile(
    title: String,
    count: Int,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun OfficerPriorityCard(
    complaint: GarbageComplaint,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (complaint.isEmergency)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        modifier = Modifier.width(260.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = complaint.id,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (complaint.isEmergency) {
                    EmergencyPriorityBadge()
                } else {
                    OfficerStatusBadge(status = complaint.status)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = getGarbageTypeIcon(complaint.garbageType),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = complaint.garbageType.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = complaint.locationDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = complaint.citizenName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTimestamp(complaint.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun OfficerComplaintListItem(
    complaint: GarbageComplaint,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (complaint.isEmergency)
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getGarbageTypeIcon(complaint.garbageType),
                        contentDescription = null,
                        tint = if (complaint.isEmergency)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = complaint.id,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OfficerStatusBadge(status = complaint.status)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = complaint.locationDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = complaint.citizenName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (complaint.assignedDriverName != null) {
                        Text(
                            text = "Driver: ${complaint.assignedDriverName}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun OfficerStatusBadge(
    status: ComplaintStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        ComplaintStatus.SUBMITTED -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        ComplaintStatus.ASSIGNED -> Color(0xFFE3F2FD) to Color(0xFF0D47A1)
        ComplaintStatus.PICKUP_IN_PROGRESS -> Color(0xFFEDE7F6) to Color(0xFF4A148C)
        ComplaintStatus.CLEANED -> Color(0xFFE8F5E9) to Color(0xFF1B5E20)
        ComplaintStatus.VERIFIED -> Color(0xFFE0F2F1) to Color(0xFF004D40)
        ComplaintStatus.REJECTED -> Color(0xFFFFEBEE) to Color(0xFFB71C1C)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = status.displayName,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun OfficerQuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
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

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
