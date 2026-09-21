package com.garbage.management.presentation.components

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.ContextCompat
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Festival
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.garbage.management.domain.model.ComplaintStatus
import com.garbage.management.domain.model.GarbageType
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Renders an image from either a remote HTTPS URL (Cloudinary) or local content/file URI
 * using Coil with automatic disk/memory caching and fallback placeholder.
 */
@Composable
fun UriImageViewer(
    uriString: String?,
    modifier: Modifier = Modifier
) {
    if (!uriString.isNullOrBlank()) {
        val context = LocalContext.current
        coil.compose.SubcomposeAsyncImage(
            model = coil.request.ImageRequest.Builder(context)
                .data(uriString)
                .crossfade(true)
                .build(),
            contentDescription = "Garbage image",
            contentScale = ContentScale.Crop,
            modifier = modifier,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            },
            error = {
                // Fallback for local content URIs if Coil encounters security/provider restriction
                val fallbackBitmap = remember(uriString) {
                    try {
                        val uri = Uri.parse(uriString)
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        }
                    } catch (_: Exception) {
                        null
                    }
                }

                if (fallbackBitmap != null) {
                    Image(
                        bitmap = fallbackBitmap,
                        contentDescription = "Garbage image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Unable to Load Image",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No Image Preview",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Badge showing the current status of a complaint with distinct theme colors.
 */
@Composable
fun ComplaintStatusBadge(
    status: ComplaintStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        ComplaintStatus.SUBMITTED -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        ComplaintStatus.ASSIGNED -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        ComplaintStatus.PICKUP_IN_PROGRESS -> Color(0xFFF3E5F5) to Color(0xFF6A1B9A)
        ComplaintStatus.CLEANED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        ComplaintStatus.VERIFIED -> Color(0xFFE0F2F1) to Color(0xFF00695C)
        ComplaintStatus.REJECTED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = status.displayName,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Clean priority badge for emergency requests without alarming or exaggerated colors.
 */
@Composable
fun EmergencyPriorityBadge(
    modifier: Modifier = Modifier,
    text: String = "Priority Request"
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFFFEBEE),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Emergency Priority",
                tint = Color(0xFFC62828),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFB71C1C)
            )
        }
    }
}

/**
 * Returns contextual municipal processing description for each complaint status.
 */
fun getComplaintStatusDescription(status: ComplaintStatus): String {
    return when (status) {
        ComplaintStatus.SUBMITTED -> "Your report has been submitted and is waiting for municipal processing."
        ComplaintStatus.ASSIGNED -> "A garbage collection vehicle has been assigned."
        ComplaintStatus.PICKUP_IN_PROGRESS -> "Collection is currently in progress."
        ComplaintStatus.CLEANED -> "The reported garbage has been marked as cleaned."
        ComplaintStatus.VERIFIED -> "The cleanup has been verified."
        ComplaintStatus.REJECTED -> "This report was rejected by the municipal system."
    }
}

/**
 * Returns icon corresponding to NotificationType.
 */
fun getNotificationTypeIcon(type: com.garbage.management.domain.model.NotificationType): ImageVector {
    return when (type) {
        com.garbage.management.domain.model.NotificationType.COMPLAINT_SUBMITTED -> Icons.Default.DeleteSweep
        com.garbage.management.domain.model.NotificationType.COMPLAINT_STATUS_UPDATED -> Icons.Default.Check
        com.garbage.management.domain.model.NotificationType.EMERGENCY_REQUEST_RECEIVED -> Icons.Default.Warning
        com.garbage.management.domain.model.NotificationType.PICKUP_ASSIGNED -> Icons.Default.Recycling
        com.garbage.management.domain.model.NotificationType.PICKUP_STARTED -> Icons.Default.Recycling
        com.garbage.management.domain.model.NotificationType.CLEANUP_COMPLETED -> Icons.Default.Check
        com.garbage.management.domain.model.NotificationType.CLEANUP_VERIFIED -> Icons.Default.Check
        com.garbage.management.domain.model.NotificationType.DRIVER_ASSIGNMENT -> Icons.Default.DeleteSweep
        com.garbage.management.domain.model.NotificationType.SYSTEM_NOTIFICATION -> Icons.Default.Event
    }
}

/**
 * Returns matching Material icon for each GarbageType.
 */
fun getGarbageTypeIcon(type: GarbageType): ImageVector {
    return when (type) {
        GarbageType.OVERFLOWING_BIN -> Icons.Default.Recycling
        GarbageType.FESTIVAL_WASTE -> Icons.Default.Festival
        GarbageType.EVENT_WASTE -> Icons.Default.Event
        GarbageType.ROADSIDE_GARBAGE -> Icons.Default.DeleteSweep
        GarbageType.FLOOD_WASTE -> Icons.Default.WaterDamage
        GarbageType.OTHER_WASTE -> Icons.Default.Park
    }
}

/**
 * Formats epoch millisecond timestamps for user display.
 */
fun formatTimestamp(epochMillis: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(epochMillis))
}

/**
 * Status step progression timeline.
 */
@Composable
fun StatusTimelineView(
    currentStatus: ComplaintStatus,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        ComplaintStatus.SUBMITTED,
        ComplaintStatus.ASSIGNED,
        ComplaintStatus.PICKUP_IN_PROGRESS,
        ComplaintStatus.CLEANED,
        ComplaintStatus.VERIFIED
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Progress Timeline",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            steps.forEachIndexed { index, step ->
                val isCompleted = step.stepOrder < currentStatus.stepOrder
                val isCurrent = step == currentStatus
                val isPending = step.stepOrder > currentStatus.stepOrder

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(28.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when {
                                isCompleted -> MaterialTheme.colorScheme.primary
                                isCurrent -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.outlineVariant
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                MaterialTheme.colorScheme.onPrimary,
                                                CircleShape
                                            )
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MoreHoriz,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        if (index < steps.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(28.dp)
                                    .background(
                                        if (isCompleted) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.padding(top = 2.dp)) {
                        Text(
                            text = step.displayName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = when {
                                isCurrent -> MaterialTheme.colorScheme.primary
                                isCompleted -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )

                        if (isCurrent) {
                            Text(
                                text = "Current Status",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Utility helper to safely fetch device location using LocationManager.
 */
object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? = suspendCancellableCoroutine { continuation ->
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        // 1. Try last known location first
        var bestLocation: Location? = null
        if (isGpsEnabled) {
            bestLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }
        if (bestLocation == null && isNetworkEnabled) {
            bestLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }

        if (bestLocation != null) {
            continuation.resume(bestLocation)
            return@suspendCancellableCoroutine
        }

        // 2. Modern Android API 30+ getCurrentLocation or single update fallback
        val provider = if (isGpsEnabled) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val cancellationSignal = android.os.CancellationSignal()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }
            val executor = ContextCompat.getMainExecutor(context)
            locationManager.getCurrentLocation(
                provider,
                cancellationSignal,
                executor
            ) { location ->
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }
        } else {
            val listener = LocationListener { location ->
                @Suppress("DEPRECATION")
                locationManager.removeUpdates(this as LocationListener)
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }

            try {
                @Suppress("DEPRECATION")
                locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                continuation.invokeOnCancellation {
                    @Suppress("DEPRECATION")
                    locationManager.removeUpdates(listener)
                }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }
}
