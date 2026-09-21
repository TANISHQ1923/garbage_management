package com.garbage.management.presentation.screens.officer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.garbage.management.domain.model.AuthState
import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.domain.model.GarbageDriver
import com.garbage.management.domain.usecase.StatusTransitionValidator
import com.garbage.management.presentation.components.EmergencyPriorityBadge
import com.garbage.management.presentation.components.UriImageViewer
import com.garbage.management.presentation.components.formatTimestamp
import com.garbage.management.presentation.components.getComplaintStatusDescription
import com.garbage.management.presentation.components.getGarbageTypeIcon
import com.garbage.management.presentation.viewmodel.AuthViewModel
import com.garbage.management.presentation.viewmodel.OfficerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficerComplaintDetailsScreen(
    complaintId: String,
    authViewModel: AuthViewModel,
    officerViewModel: OfficerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val currentUser = (authState as? AuthState.Authenticated)?.user
    val detailState by officerViewModel.detailState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog States
    var showDriverDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showImagePreviewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(complaintId) {
        officerViewModel.loadComplaintDetails(complaintId)
    }

    LaunchedEffect(detailState.successMessage) {
        detailState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            officerViewModel.clearMessages()
        }
    }

    LaunchedEffect(detailState.error) {
        detailState.error?.let {
            snackbarHostState.showSnackbar(it)
            officerViewModel.clearMessages()
        }
    }

    val complaint = detailState.complaint

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(complaint?.id ?: "Complaint Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { officerViewModel.loadComplaintDetails(complaintId) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        if (detailState.isLoading && complaint == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (complaint == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Complaint '$complaintId' not found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onNavigateBack) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. STATUS & OPERATIONAL ACTIONS BAR
                item {
                    OfficerStatusActionBar(
                        complaint = complaint,
                        onUpdateStatusClick = { showStatusDialog = true },
                        onAssignDriverClick = { showDriverDialog = true }
                    )
                }

                // 2. SUMMARY & EMERGENCY HEADER
                item {
                    OfficerOverviewCard(complaint = complaint)
                }

                // 3. CITIZEN CONTACT CARD
                item {
                    OfficerCitizenContactCard(
                        citizenName = complaint.citizenName,
                        citizenPhone = complaint.citizenPhone,
                        citizenId = complaint.citizenId,
                        onDialClick = { phone ->
                            dialPhone(context, phone)
                        }
                    )
                }

                // 4. ASSIGNED DRIVER CARD
                item {
                    OfficerAssignedDriverCard(
                        complaint = complaint,
                        onAssignDriverClick = { showDriverDialog = true }
                    )
                }

                // 5. LOCATION DETAILS & MAP INTENT
                item {
                    OfficerLocationCard(
                        complaint = complaint,
                        onOpenMapClick = { lat, lng ->
                            openGeoMapIntent(context, lat, lng, complaint.locationDescription)
                        }
                    )
                }

                // 6. MEDIA ATTACHMENTS (IMAGE & VIDEO) & COLLECTION EVIDENCE
                item {
                    OfficerMediaSection(
                        imageUri = complaint.imageUri,
                        videoUri = complaint.videoUri,
                        beforeCleaningImageUri = complaint.beforeCleaningImageUri,
                        afterCleaningImageUri = complaint.afterCleaningImageUri,
                        onImageClick = { showImagePreviewDialog = true },
                        onPlayVideoClick = { uri ->
                            openVideoIntent(context, uri)
                        }
                    )
                }

                // 7. AI WASTE ANALYSIS SECTION (ASSISTIVE INTELLIGENCE)
                if (complaint.aiAnalysis != null || complaint.aiAnalysisStatus != null || !complaint.imageUri.isNullOrBlank()) {
                    item {
                        OfficerAiAnalysisSection(
                            aiAnalysis = complaint.aiAnalysis,
                            aiStatus = complaint.aiAnalysisStatus
                        )
                    }
                }

                // 8. INTERNAL OFFICER NOTES
                item {
                    OfficerNotesSection(
                        notes = complaint.officerNotes,
                        onAddNoteClick = { showNoteDialog = true }
                    )
                }

                // 8. CHRONOLOGICAL STATUS HISTORY
                item {
                    OfficerStatusHistorySection(history = complaint.statusHistory)
                }
            }
        }
    }

    // DIALOG: ASSIGN / CHANGE DRIVER
    if (showDriverDialog && complaint != null && currentUser != null) {
        OfficerAssignDriverDialog(
            currentStatus = complaint.status,
            currentlyAssignedDriverId = complaint.assignedDriverId,
            availableDrivers = detailState.availableDrivers,
            isAssigning = detailState.isAssigning,
            onDismiss = { showDriverDialog = false },
            onAssign = { selectedDriver, note ->
                officerViewModel.assignDriver(
                    complaintId = complaint.id,
                    driver = selectedDriver,
                    officer = currentUser,
                    noteText = note,
                    onSuccess = { showDriverDialog = false }
                )
            }
        )
    }

    // DIALOG: UPDATE STATUS
    if (showStatusDialog && complaint != null && currentUser != null) {
        val allowedStatuses = StatusTransitionValidator.getAllowedNextStatuses(complaint.status)
        OfficerUpdateStatusDialog(
            currentStatus = complaint.status,
            allowedStatuses = allowedStatuses,
            isUpdating = detailState.isUpdatingStatus,
            onDismiss = { showStatusDialog = false },
            onConfirmUpdate = { selectedStatus, note ->
                officerViewModel.updateStatus(
                    complaintId = complaint.id,
                    newStatus = selectedStatus,
                    officer = currentUser,
                    noteText = note,
                    onSuccess = { showStatusDialog = false }
                )
            }
        )
    }

    // DIALOG: ADD INTERNAL NOTE
    if (showNoteDialog && complaint != null && currentUser != null) {
        OfficerAddNoteDialog(
            isAdding = detailState.isAddingNote,
            onDismiss = { showNoteDialog = false },
            onConfirmAdd = { text ->
                officerViewModel.addOfficerNote(
                    complaintId = complaint.id,
                    officer = currentUser,
                    noteText = text,
                    onSuccess = { showNoteDialog = false }
                )
            }
        )
    }

    // FULL SCREEN IMAGE PREVIEW MODAL
    if (showImagePreviewDialog && !complaint?.imageUri.isNullOrBlank()) {
        Dialog(onDismissRequest = { showImagePreviewDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Complaint Photo Preview",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { showImagePreviewDialog = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        UriImageViewer(
                            uriString = complaint?.imageUri,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// SECTION COMPOSABLES
// ------------------------------------------------------------------------------------------------

@Composable
fun OfficerStatusActionBar(
    complaint: GarbageComplaint,
    onUpdateStatusClick: () -> Unit,
    onAssignDriverClick: () -> Unit
) {
    val allowedNextStatuses = StatusTransitionValidator.getAllowedNextStatuses(complaint.status)
    val canUpdateStatus = allowedNextStatuses.isNotEmpty()
    val canAssignDriver = complaint.status != ComplaintStatus.VERIFIED && complaint.status != ComplaintStatus.REJECTED

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current State",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    OfficerStatusBadge(status = complaint.status)
                }

                if (complaint.isEmergency) {
                    EmergencyPriorityBadge()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = getComplaintStatusDescription(complaint.status),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onUpdateStatusClick,
                    enabled = canUpdateStatus,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Update,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Update Status")
                }

                OutlinedButton(
                    onClick = onAssignDriverClick,
                    enabled = canAssignDriver,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AssignmentInd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (complaint.assignedDriverId != null) "Change Driver" else "Assign Driver")
                }
            }
        }
    }
}

@Composable
fun OfficerOverviewCard(complaint: GarbageComplaint) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Incident Classification",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = getGarbageTypeIcon(complaint.garbageType),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = complaint.garbageType.displayName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Submitted ${formatTimestamp(complaint.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!complaint.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Citizen Report Description",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = complaint.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun OfficerCitizenContactCard(
    citizenName: String,
    citizenPhone: String?,
    citizenId: String,
    onDialClick: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = citizenName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = citizenPhone ?: "No phone registered",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!citizenPhone.isNullOrBlank()) {
                IconButton(onClick = { onDialClick(citizenPhone) }) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call Citizen",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun OfficerAssignedDriverCard(
    complaint: GarbageComplaint,
    onAssignDriverClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dispatch & Collection Vehicle",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (complaint.status != ComplaintStatus.VERIFIED && complaint.status != ComplaintStatus.REJECTED) {
                    TextButton(onClick = onAssignDriverClick) {
                        Text(if (complaint.assignedDriverId != null) "Reassign" else "Assign")
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (complaint.assignedDriverName != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Engineering,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = complaint.assignedDriverName,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Vehicle: ${complaint.assignedVehicleNumber ?: "N/A"} • ID: ${complaint.assignedDriverId ?: "N/A"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (complaint.assignedAt != null) {
                            Text(
                                text = "Assigned ${formatTimestamp(complaint.assignedAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No driver or vehicle assigned yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun OfficerLocationCard(
    complaint: GarbageComplaint,
    onOpenMapClick: (Double, Double) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Location & Coordinates",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                if (complaint.latitude != null && complaint.longitude != null) {
                    TextButton(onClick = { onOpenMapClick(complaint.latitude, complaint.longitude) }) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open in Maps")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = complaint.locationDescription,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    if (complaint.latitude != null && complaint.longitude != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "GPS: %.5f, %.5f".format(complaint.latitude, complaint.longitude),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Manual location description only (No GPS coordinates captured)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OfficerMediaSection(
    imageUri: String?,
    videoUri: String?,
    beforeCleaningImageUri: String? = null,
    afterCleaningImageUri: String? = null,
    onImageClick: () -> Unit,
    onPlayVideoClick: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Collection Evidence",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Field documentation submitted by reporting citizen and collection vehicle driver.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Citizen Report Photo
            Text(
                text = "1. Citizen Report Photo",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (!imageUri.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onImageClick() }
                ) {
                    UriImageViewer(uriString = imageUri, modifier = Modifier.fillMaxSize())
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(topStart = 8.dp),
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Text(
                            text = "Tap to enlarge",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "No citizen report image attached.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // 2. Driver Before Cleaning Photo
            if (!beforeCleaningImageUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "2. Driver Before Cleaning Photo",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                UriImageViewer(
                    uriString = beforeCleaningImageUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            // 3. Driver After Cleaning Photo
            if (!afterCleaningImageUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "3. Driver After Cleaning Photo (Completion Evidence)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF2E7D32)
                )
                Spacer(modifier = Modifier.height(6.dp))
                UriImageViewer(
                    uriString = afterCleaningImageUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            // Video Evidence
            if (!videoUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    onClick = { onPlayVideoClick(videoUri) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Play Video",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Video attachment available",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Tap to open in device player",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OfficerNotesSection(
    notes: List<com.garbage.management.domain.model.OfficerNote>,
    onAddNoteClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.StickyNote2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Internal Municipal Notes (${notes.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(onClick = onAddNoteClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                        contentDescription = "Add Internal Note",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "Confidential notes visible only to municipal personnel.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (notes.isEmpty()) {
                Text(
                    text = "No internal notes recorded yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    notes.forEach { note ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = note.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "By ${note.officerName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = formatTimestamp(note.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OfficerStatusHistorySection(
    history: List<com.garbage.management.domain.model.ComplaintStatusHistory>
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Status History Timeline",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column {
                history.forEachIndexed { index, item ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            if (index < history.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(36.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.padding(bottom = if (index < history.size - 1) 12.dp else 0.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.status.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = formatTimestamp(item.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!item.note.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// DIALOGS
// ------------------------------------------------------------------------------------------------

@Composable
fun OfficerAssignDriverDialog(
    currentStatus: ComplaintStatus,
    currentlyAssignedDriverId: String?,
    availableDrivers: List<GarbageDriver>,
    isAssigning: Boolean,
    onDismiss: () -> Unit,
    onAssign: (GarbageDriver, String?) -> Unit
) {
    var selectedDriver by remember {
        mutableStateOf(availableDrivers.find { it.id == currentlyAssignedDriverId } ?: availableDrivers.firstOrNull { it.isAvailable })
    }
    var assignmentNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isAssigning) onDismiss() },
        title = {
            Text(if (currentlyAssignedDriverId != null) "Reassign Garbage Vehicle" else "Assign Collection Vehicle")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select an operational driver and vehicle for this complaint:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (availableDrivers.isEmpty()) {
                    Text(
                        text = "No drivers currently available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        items(availableDrivers) { driver ->
                            val isSelected = selectedDriver?.id == driver.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedDriver = driver }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedDriver = driver }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "${driver.name} (${driver.vehicleNumber})",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                    Text(
                                        text = "ID: ${driver.id} • ${if (driver.isAvailable) "Available" else "On Route"} • ${driver.currentLocationDescription}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (driver.isAvailable) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = assignmentNote,
                    onValueChange = { assignmentNote = it },
                    label = { Text("Operational note (optional)") },
                    placeholder = { Text("e.g., Assigned for Ward 12 morning collection") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedDriver?.let { onAssign(it, assignmentNote.takeIf { n -> n.isNotBlank() }) }
                },
                enabled = selectedDriver != null && !isAssigning
            ) {
                if (isAssigning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Confirm Assignment")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isAssigning
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun OfficerUpdateStatusDialog(
    currentStatus: ComplaintStatus,
    allowedStatuses: List<ComplaintStatus>,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onConfirmUpdate: (ComplaintStatus, String?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(allowedStatuses.firstOrNull()) }
    var statusNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        title = { Text("Update Complaint Status") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Current: ${currentStatus.displayName}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Select permitted next operational status:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (allowedStatuses.isEmpty()) {
                    Text(
                        text = "This complaint is in a terminal status and cannot be transitioned further.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Column {
                        allowedStatuses.forEach { status ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedStatus = status }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedStatus == status,
                                    onClick = { selectedStatus = status }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = status.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                    Text(
                                        text = getComplaintStatusDescription(status),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = statusNote,
                    onValueChange = { statusNote = it },
                    label = { Text("Status update note (optional)") },
                    placeholder = { Text("e.g., Collection initiated by team") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedStatus?.let { onConfirmUpdate(it, statusNote.takeIf { n -> n.isNotBlank() }) }
                },
                enabled = selectedStatus != null && !isUpdating
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Apply Status")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUpdating
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun OfficerAddNoteDialog(
    isAdding: Boolean,
    onDismiss: () -> Unit,
    onConfirmAdd: (String) -> Unit
) {
    var noteText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isAdding) onDismiss() },
        title = { Text("Add Internal Municipal Note") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "This note will only be visible to municipal officers and will not be displayed to the citizen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Enter internal observation, inspection result, or instructions...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmAdd(noteText) },
                enabled = noteText.isNotBlank() && !isAdding
            ) {
                if (isAdding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Note")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isAdding
            ) {
                Text("Cancel")
            }
        }
    )
}

// ------------------------------------------------------------------------------------------------
// INTENT HELPERS
// ------------------------------------------------------------------------------------------------

private fun dialPhone(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${phone.trim()}")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun openGeoMapIntent(context: Context, lat: Double, lng: Double, label: String) {
    try {
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "No map application available to display coordinates", Toast.LENGTH_SHORT).show()
    }
}

private fun openVideoIntent(context: Context, videoUriString: String) {
    try {
        val uri = Uri.parse(videoUriString)
        val isRemote = videoUriString.startsWith("http://", true) || videoUriString.startsWith("https://", true)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            if (isRemote) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUriString)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "No video player or browser available to open attachment", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun OfficerAiAnalysisSection(
    aiAnalysis: com.garbage.management.domain.model.AiAnalysis?,
    aiStatus: String?
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Waste Analysis",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "Assistive",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (aiAnalysis != null) {
                val category = aiAnalysis.classification?.category?.replace("_", " ")?.lowercase(java.util.Locale.ROOT)?.replaceFirstChar { it.uppercase(java.util.Locale.ROOT) } ?: "Under analysis"
                val severity = aiAnalysis.severity?.level ?: "Not assessed"
                val confidenceText = if (aiAnalysis.classification?.confidence != null) {
                    "${(aiAnalysis.classification.confidence * 100).toInt()}%"
                } else {
                    "Not available"
                }
                val modelStatusText = aiAnalysis.modelStatus.replace("_", " ").lowercase(java.util.Locale.ROOT).replaceFirstChar { it.uppercase(java.util.Locale.ROOT) }

                // 2-column grid of key metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Waste Type
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Waste Type",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = category,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Severity
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Severity",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = severity,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = when (severity) {
                                    "CRITICAL" -> Color(0xFFC62828)
                                    "HIGH" -> Color(0xFFE65100)
                                    "MEDIUM" -> Color(0xFFF57F17)
                                    else -> Color(0xFF2E7D32)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // AI Status
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "AI Status",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = modelStatusText,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Confidence
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Confidence",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = confidenceText,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else if (aiStatus == "AI_UNAVAILABLE") {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "AI image analysis service was unavailable during report submission.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Text(
                    text = "No image attached or AI analysis pending.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI-generated estimate — officer verification required. AI cannot alter official complaint status.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

