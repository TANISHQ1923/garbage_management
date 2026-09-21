package com.garbage.management.presentation.screens.citizen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.garbage.management.domain.model.AuthState
import com.garbage.management.domain.model.GarbageType
import com.garbage.management.presentation.components.AppTopBar
import com.garbage.management.presentation.components.LocationHelper
import com.garbage.management.presentation.components.PrimaryAppButton
import com.garbage.management.presentation.components.UriImageViewer
import com.garbage.management.presentation.components.getGarbageTypeIcon
import com.garbage.management.presentation.viewmodel.AuthViewModel
import com.garbage.management.presentation.viewmodel.ComplaintViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportGarbageScreen(
    authViewModel: AuthViewModel,
    complaintViewModel: ComplaintViewModel,
    initialEmergency: Boolean = false,
    onReportSubmitted: (complaintId: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val currentUser = (authState as? AuthState.Authenticated)?.user

    val state by complaintViewModel.reportUiState.collectAsStateWithLifecycle()

    var isCapturingLocation by remember { mutableStateOf(false) }
    var tempCameraImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(initialEmergency) {
        complaintViewModel.resetReportForm(initialEmergency = initialEmergency)
    }

    // Camera capture launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraImageUri != null) {
            complaintViewModel.onImageSelected(tempCameraImageUri.toString())
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val photoFile = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            tempCameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos.", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery image picker launcher
    val galleryImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            complaintViewModel.onImageSelected(uri.toString())
        }
    }

    // Video picker launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment ?: "video_${System.currentTimeMillis()}.mp4"
            complaintViewModel.onVideoSelected(uri.toString(), fileName)
        }
    }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isCapturingLocation = true
            coroutineScope.launch {
                val location = LocationHelper.getCurrentLocation(context)
                isCapturingLocation = false
                if (location != null) {
                    complaintViewModel.onLocationCaptured(location.latitude, location.longitude)
                } else {
                    Toast.makeText(context, "Unable to get current GPS location. Please check location settings or enter manually.", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(context, "Location permission denied. Please enter location manually.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (state.isEmergency) "Emergency Garbage Report" else "Report Garbage Issue",
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
            // PROMINENT EMERGENCY NOTICE
            if (state.isEmergency) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFEBEE),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Emergency Pickup Request",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFB71C1C)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Emergency requests are intended for urgent garbage situations and may receive higher priority during municipal processing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF880E4F)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // SECTION 1: GARBAGE TYPE
            Text(
                text = "1. Select Garbage Type *",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Choose the category that best describes the issue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val chunkedTypes = GarbageType.entries.chunked(2)
                for (rowTypes in chunkedTypes) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (type in rowTypes) {
                            GarbageTypeSelectCard(
                                type = type,
                                isSelected = state.garbageType == type,
                                onClick = { complaintViewModel.onGarbageTypeSelect(type) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 2: DESCRIPTION
            Text(
                text = "2. Issue Description",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Provide additional details (approximate size, landmarks)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = state.description,
                onValueChange = {
                    if (it.length <= 500) complaintViewModel.onDescriptionChange(it)
                },
                placeholder = { Text("e.g., Overflowing onto road, bad odor near bus stop, need prompt clearance...") },
                supportingText = {
                    Text(
                        text = "${state.description.length} / 500 characters",
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 3: IMAGE ATTACHMENT
            Text(
                text = "3. Attach Garbage Photo",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Photo evidence helps municipal officers and drivers locate the issue quickly",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (state.imageUri != null) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            UriImageViewer(
                                uriString = state.imageUri,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            IconButton(
                                onClick = complaintViewModel::onImageRemoved,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Photo Attached",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedButton(
                                onClick = { galleryImageLauncher.launch("image/*") },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Replace")
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                val photoFile = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                                tempCameraImageUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Take Photo")
                    }

                    OutlinedButton(
                        onClick = { galleryImageLauncher.launch("image/*") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("From Gallery")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 4: VIDEO ATTACHMENT
            Text(
                text = "4. Attach Video (Optional)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (state.videoUri != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Video Attached",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = state.videoFileName ?: "Video file selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        IconButton(onClick = complaintViewModel::onVideoRemoved) {
                            Icon(Icons.Default.Close, contentDescription = "Remove video")
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { videoPickerLauncher.launch("video/*") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose or Record Video")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 5: LOCATION
            Text(
                text = "5. Garbage Location *",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Capture GPS coordinates or describe the exact location",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // GPS Location Action Button
            OutlinedButton(
                onClick = {
                    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                    if (hasFine || hasCoarse) {
                        isCapturingLocation = true
                        coroutineScope.launch {
                            val loc = LocationHelper.getCurrentLocation(context)
                            isCapturingLocation = false
                            if (loc != null) {
                                complaintViewModel.onLocationCaptured(loc.latitude, loc.longitude)
                            } else {
                                Toast.makeText(context, "Could not acquire GPS fix. Please ensure location is enabled.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                enabled = !isCapturingLocation,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isCapturingLocation) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Acquiring GPS Location...")
                } else {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (state.isGpsCaptured) "Refresh Current Location" else "Use Current Location")
                }
            }

            if (state.isGpsCaptured && state.latitude != null && state.longitude != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(Locale.US, "GPS Captured: Lat %.5f, Long %.5f", state.latitude, state.longitude),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF1B5E20)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Manual Location Input
            OutlinedTextField(
                value = state.locationDescription,
                onValueChange = complaintViewModel::onManualLocationChange,
                label = { Text("Location Landmark / Street Address") },
                placeholder = { Text("e.g., Behind Central Bus Terminal, Opposite Gate 4") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                isError = state.locationError != null,
                supportingText = state.locationError?.let { { Text(it) } },
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 6: EMERGENCY PICKUP
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isEmergency) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
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
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (state.isEmergency) Color(0xFFC62828) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Request Emergency Pickup",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (state.isEmergency) Color(0xFFB71C1C) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Switch(
                            checked = state.isEmergency,
                            onCheckedChange = complaintViewModel::onEmergencyChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFC62828),
                                checkedTrackColor = Color(0xFFFFCDD2)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Emergency pickup requests are prioritized for urgent garbage situations, blocked access, or immediate health hazards.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.isEmergency) Color(0xFF880E4F) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.generalError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.generalError ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // UPLOAD PROGRESS BANNER
            if (state.isSubmitting && !state.uploadProgressMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = state.uploadProgressMessage ?: "Processing...",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // SECTION 7: SUBMIT BUTTON
            PrimaryAppButton(
                text = if (state.isSubmitting && !state.uploadProgressMessage.isNullOrBlank()) {
                    state.uploadProgressMessage ?: "Submitting..."
                } else if (state.isEmergency) {
                    "Submit Emergency Report"
                } else {
                    "Submit Garbage Report"
                },
                onClick = {
                    if (state.isSubmitting) return@PrimaryAppButton
                    if (currentUser != null) {
                        val hasGps = state.latitude != null && state.longitude != null
                        val hasManual = state.locationDescription.isNotBlank()

                        if (state.isEmergency) {
                            if (!hasGps && !hasManual) {
                                complaintViewModel.submitReport(
                                    context = context,
                                    citizenUser = currentUser,
                                    onSuccess = {}
                                )
                            } else {
                                complaintViewModel.showEmergencyConfirmDialog()
                            }
                        } else {
                            complaintViewModel.submitReport(
                                context = context,
                                citizenUser = currentUser,
                                onSuccess = { createdComplaint ->
                                    onReportSubmitted(createdComplaint.id)
                                }
                            )
                        }
                    } else {
                        Toast.makeText(context, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
                    }
                },
                isLoading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            )

            // EMERGENCY CONFIRMATION DIALOG
            if (state.showEmergencyConfirmDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = complaintViewModel::dismissEmergencyConfirmDialog,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Submit Emergency Pickup Request?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Please confirm the urgent details of this request:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "• Garbage Type: ${state.garbageType.displayName}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "• Location: ${if (state.locationDescription.isNotBlank()) state.locationDescription else String.format(Locale.US, "GPS: %.4f, %.4f", state.latitude ?: 0.0, state.longitude ?: 0.0)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "• Emergency Pickup: YES",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB71C1C)
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Emergency requests are intended for urgent garbage situations and may receive higher priority during municipal processing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                if (currentUser != null) {
                                    complaintViewModel.submitReport(
                                        context = context,
                                        citizenUser = currentUser,
                                        onSuccess = { createdComplaint ->
                                            onReportSubmitted(createdComplaint.id)
                                        }
                                    )
                                }
                            }
                        ) {
                            Text("Confirm Request", color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.TextButton(
                            onClick = complaintViewModel::dismissEmergencyConfirmDialog
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun GarbageTypeSelectCard(
    type: GarbageType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getGarbageTypeIcon(type),
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = type.displayName,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
