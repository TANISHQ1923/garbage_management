package com.garbage.management.presentation.screens.driver

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.Scaffold
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
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.garbage.management.domain.model.AuthState
import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.GarbageComplaint
import com.garbage.management.presentation.components.ComplaintStatusBadge
import com.garbage.management.presentation.components.EmergencyPriorityBadge
import com.garbage.management.presentation.components.UriImageViewer
import com.garbage.management.presentation.components.formatTimestamp
import com.garbage.management.presentation.components.getGarbageTypeIcon
import com.garbage.management.presentation.viewmodel.AuthViewModel
import com.garbage.management.presentation.viewmodel.DriverViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverTaskDetailsScreen(
    complaintId: String,
    authViewModel: AuthViewModel,
    driverViewModel: DriverViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val currentUser = (authState as? AuthState.Authenticated)?.user
    val driverId = currentUser?.driverId ?: currentUser?.id ?: "DRV-882"
    val driverName = currentUser?.name ?: "Ramesh Kumar"

    val uiState by driverViewModel.uiState.collectAsStateWithLifecycle()
    val complaint = uiState.currentComplaintDetail

    var showStartConfirmation by remember { mutableStateOf(false) }
    var showCompleteConfirmation by remember { mutableStateOf(false) }

    // Temp camera photo storage
    var tempCameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturingForType by remember { mutableStateOf<String?>(null) } // "before" or "after"

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraImageUri != null) {
            if (capturingForType == "before") {
                driverViewModel.setBeforeCleaningImage(tempCameraImageUri.toString())
            } else if (capturingForType == "after") {
                driverViewModel.setAfterCleaningImage(tempCameraImageUri.toString())
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val photoFile = File(context.cacheDir, "driver_photo_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                tempCameraImageUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to initialize camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to capture collection evidence.", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            if (capturingForType == "before") {
                driverViewModel.setBeforeCleaningImage(uri.toString())
            } else if (capturingForType == "after") {
                driverViewModel.setAfterCleaningImage(uri.toString())
            }
        }
    }

    fun launchPhotoCapture(type: String) {
        capturingForType = type
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun launchGalleryPick(type: String) {
        capturingForType = type
        galleryLauncher.launch("image/*")
    }

    LaunchedEffect(complaintId) {
        driverViewModel.loadComplaintDetail(complaintId)
    }

    // Feedback toasts
    LaunchedEffect(uiState.userFeedbackMessage) {
        uiState.userFeedbackMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            driverViewModel.clearFeedback()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            driverViewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Task Details",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoadingDetail || complaint == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. HEADER OVERVIEW CARD
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                                        text = "Complaint ID",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = complaint.id,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                                ComplaintStatusBadge(status = complaint.status)
                            }

                            if (complaint.isEmergency) {
                                Spacer(modifier = Modifier.height(10.dp))
                                EmergencyPriorityBadge(text = "PRIORITY / EMERGENCY TASK")
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            // Garbage type
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = getGarbageTypeIcon(complaint.garbageType),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Waste Type",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = complaint.garbageType.displayName,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (!complaint.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = complaint.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 2. LOCATION & MAPS NAVIGATION CARD
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Garbage Location",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = complaint.locationDescription,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (complaint.latitude != null && complaint.longitude != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Coordinates: ${String.format("%.5f", complaint.latitude)}, ${String.format("%.5f", complaint.longitude)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        openNavigationMap(context, complaint.latitude, complaint.longitude)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Directions,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Navigate to Location",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }

                    // 3. VEHICLE & ASSIGNMENT INFO
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Assigned Vehicle: ${complaint.assignedVehicleNumber ?: "DL-01-GB-4040"}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            complaint.assignedAt?.let { timestamp ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Dispatched at: ${formatTimestamp(timestamp)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Officer note if available
                            val relevantNote = complaint.officerNotes.lastOrNull()
                            if (relevantNote != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.Note,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "Officer Note (${relevantNote.officerName})",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = relevantNote.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. ACTION SECTIONS ACCORDING TO COMPLAINT STATUS
                    when (complaint.status) {
                        ComplaintStatus.ASSIGNED -> {
                            // Section: Start Collection
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        text = "Ready to Start Collection",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Confirm arrival at the site and tap Start Collection to begin clearing waste.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showStartConfirmation = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2E7D32) // Forest Green
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Start Collection",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }

                        ComplaintStatus.PICKUP_IN_PROGRESS -> {
                            // Section: Evidence Capture and Completion
                            Text(
                                text = "Collection Evidence & Completion",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Before Cleaning Photo Card
                            PhotoEvidenceCard(
                                title = "Before Cleaning Photo",
                                subtitle = "Optional initial evidence showing waste volume before cleanup.",
                                imageUri = uiState.draftBeforeImageUri,
                                isMandatory = false,
                                onTakePhoto = { launchPhotoCapture("before") },
                                onChooseGallery = { launchGalleryPick("before") },
                                onRemovePhoto = { driverViewModel.setBeforeCleaningImage(null) }
                            )

                            // After Cleaning Photo Card (MANDATORY)
                            PhotoEvidenceCard(
                                title = "After Cleaning Photo",
                                subtitle = "MANDATORY verification photo showing clear and sanitized site.",
                                imageUri = uiState.draftAfterImageUri,
                                isMandatory = true,
                                onTakePhoto = { launchPhotoCapture("after") },
                                onChooseGallery = { launchGalleryPick("after") },
                                onRemovePhoto = { driverViewModel.setAfterCleaningImage(null) }
                            )

                            // Mark Completed Action Button
                            val hasAfterPhoto = !uiState.draftAfterImageUri.isNullOrBlank()
                            Button(
                                onClick = { showCompleteConfirmation = true },
                                enabled = hasAfterPhoto,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1565C0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Mark Collection Completed",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            if (!hasAfterPhoto) {
                                Text(
                                    text = "⚠️ An After-Cleaning photo is required to complete this collection.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        ComplaintStatus.CLEANED,
                        ComplaintStatus.VERIFIED -> {
                            // Completed Banner
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Collection Completed",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF1B5E20)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "This waste collection was completed and documented. Awaiting final inspection by municipal officer.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF2E7D32)
                                    )

                                    if (!complaint.afterCleaningImageUri.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Documented Cleanup Evidence:",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF1B5E20)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        UriImageViewer(
                                            uriString = complaint.afterCleaningImageUri,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                    }
                                }
                            }
                        }

                        else -> Unit
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // DIALOG: Start Collection Confirmation
            if (showStartConfirmation && complaint != null) {
                AlertDialog(
                    onDismissRequest = { showStartConfirmation = false },
                    title = {
                        Text(
                            text = "Start Collection",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    text = {
                        Text("Start garbage collection for this assignment (${complaint.id})?")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showStartConfirmation = false
                                driverViewModel.startCollection(
                                    context = context,
                                    complaintId = complaint.id,
                                    driverId = driverId,
                                    driverName = driverName
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("Start Collection")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showStartConfirmation = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // DIALOG: Complete Collection Confirmation
            if (showCompleteConfirmation && complaint != null) {
                AlertDialog(
                    onDismissRequest = { showCompleteConfirmation = false },
                    title = {
                        Text(
                            text = "Complete Collection",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Mark this garbage collection as completed?",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "• Complaint ID: ${complaint.id}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• Before Photo: ${if (!uiState.draftBeforeImageUri.isNullOrBlank()) "Attached ✓" else "Not provided"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "• After Photo: ${if (!uiState.draftAfterImageUri.isNullOrBlank()) "Attached ✓" else "Missing"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (!uiState.draftAfterImageUri.isNullOrBlank()) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showCompleteConfirmation = false
                                driverViewModel.completeCollection(
                                    context = context,
                                    complaintId = complaint.id,
                                    driverId = driverId,
                                    driverName = driverName
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                        ) {
                            Text("Complete Collection")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCompleteConfirmation = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PhotoEvidenceCard(
    title: String,
    subtitle: String,
    imageUri: String?,
    isMandatory: Boolean,
    onTakePhoto: () -> Unit,
    onChooseGallery: () -> Unit,
    onRemovePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isMandatory) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = "REQUIRED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFC62828),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (!imageUri.isNullOrBlank()) {
                    IconButton(
                        onClick = onRemovePhoto,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Photo",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!imageUri.isNullOrBlank()) {
                // PHOTO PREVIEW
                UriImageViewer(
                    uriString = imageUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onTakePhoto,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retake")
                    }

                    OutlinedButton(
                        onClick = onChooseGallery,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gallery")
                    }
                }
            } else {
                // CAPTURE / PICK BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onTakePhoto,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take Photo")
                    }

                    OutlinedButton(
                        onClick = onChooseGallery,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Choose Gallery")
                    }
                }
            }
        }
    }
}

/**
 * Safely launches map navigation intent with fallback to browser or error message.
 */
private fun openNavigationMap(context: Context, latitude: Double, longitude: Double) {
    try {
        val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(Garbage+Collection+Site)")
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        try {
            val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
            context.startActivity(browserIntent)
        } catch (e2: Exception) {
            Toast.makeText(
                context,
                "No navigation or map application installed to open coordinates.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
