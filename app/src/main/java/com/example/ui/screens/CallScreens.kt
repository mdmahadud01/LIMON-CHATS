package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.media.CallAudioRoute
import com.example.media.shouldUseProximitySensor
import com.example.model.CallStatus
import com.example.model.CallType
import com.example.ui.theme.DestructiveRed
import com.example.ui.theme.GlassCard
import com.example.ui.theme.LiquidGlassBackground
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.UserAvatar
import com.example.viewmodel.CallViewModel
import org.webrtc.SurfaceViewRenderer

@Composable
fun ActiveCallScreen(
    callId: String,
    viewModel: CallViewModel,
    isInPipMode: Boolean = false,
    onEnterPip: (() -> Unit)? = null,
    onCallFinished: () -> Unit
) {
    val context = LocalContext.current
    val glass = LocalLiquidGlassColors.current
    val session by viewModel.activeCall.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsStateWithLifecycle()
    val audioRoute by viewModel.audioRoute.collectAsStateWithLifecycle()
    val connectedAccessory by viewModel.connectedAccessory.collectAsStateWithLifecycle()
    val isVideoEnabled by viewModel.isVideoEnabled.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.durationSeconds.collectAsStateWithLifecycle()
    val rtcReadyTick by viewModel.rtcReadyTick.collectAsStateWithLifecycle()

    var hasAudioPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasAudioPerm = grants[Manifest.permission.RECORD_AUDIO] ?: hasAudioPerm
        hasCameraPerm = grants[Manifest.permission.CAMERA] ?: hasCameraPerm
        viewModel.onPermissionsUpdated(hasAudioPerm, hasCameraPerm)
    }

    LaunchedEffect(callId, hasAudioPerm, hasCameraPerm) {
        viewModel.joinOrAcceptCall(callId)
        viewModel.onPermissionsUpdated(hasAudioPerm, hasCameraPerm)
        if (!hasAudioPerm) {
            permLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
        }
    }

    LaunchedEffect(session?.id, session?.status) {
        if (session?.id == callId && session?.status in listOf(CallStatus.ENDED.value, CallStatus.DECLINED.value, CallStatus.MISSED.value)) {
            onCallFinished()
        }
    }

    val isIncomingRinging = session?.receiverId == viewModel.currentUserId &&
        session?.status == CallStatus.RINGING.value
    val isVideoCall = session?.callType == CallType.VIDEO.value
    val isVideoAccepted = isVideoCall && session?.status == CallStatus.ACCEPTED.value
    val peerName = if (session?.callerId == viewModel.currentUserId) {
        session?.receiverName ?: "Contact"
    } else {
        session?.callerName ?: "Caller"
    }
    val peerPhoto = if (session?.callerId == viewModel.currentUserId) {
        session?.receiverPhotoUrl
    } else {
        session?.callerPhotoUrl
    }

    // Proximity sensor screen-off ONLY during earpiece voice calls; stays ON when Speaker, Bluetooth, or Earphone is active
    val engageProximity = shouldUseProximitySensor(
        isVideoCall = isVideoCall,
        isSpeakerOn = isSpeakerOn,
        audioRoute = audioRoute
    )
    DisposableEffect(engageProximity, callId) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = if (engageProximity && powerManager != null &&
            powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)
        ) {
            runCatching {
                powerManager.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "LimonChat:CallProximity"
                ).apply { acquire(60 * 60 * 1000L) }
            }.getOrNull()
        } else {
            null
        }
        onDispose {
            runCatching {
                if (wakeLock != null && wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }
    }

    var localRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    var remoteRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    DisposableEffect(localRenderer, remoteRenderer, isVideoCall, rtcReadyTick) {
        if (isVideoCall && (localRenderer != null || remoteRenderer != null)) {
            viewModel.attachVideoRenderers(localRenderer, remoteRenderer)
        }
        onDispose { }
    }

    val pulseTransition = rememberInfiniteTransition(label = "call_pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "pulse_scale"
    )

    LiquidGlassBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isVideoAccepted && hasCameraPerm) {
                AndroidView(
                    factory = { ctx -> SurfaceViewRenderer(ctx).also { remoteRenderer = it } },
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (isVideoCall && hasCameraPerm && isVideoEnabled && !isInPipMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(width = 104.dp, height = 152.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    AndroidView(
                        factory = { ctx -> SurfaceViewRenderer(ctx).also { localRenderer = it } },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            if (isVideoAccepted && hasCameraPerm && !isInPipMode && onEnterPip != null) {
                IconButton(
                    onClick = onEnterPip,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.50f))
                        .testTag("call_pip_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureInPictureAlt,
                        contentDescription = "Picture in picture",
                        tint = Color.White
                    )
                }
            }

            if (!isInPipMode) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(20.dp))
                        if (!isVideoAccepted) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(126.dp)
                                        .scale(pulse)
                                        .clip(CircleShape)
                                        .background(glass.accent.copy(alpha = 0.22f))
                                )
                                UserAvatar(displayName = peerName, photoUrl = peerPhoto, size = 102.dp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        val overlayTextColor = if (isVideoAccepted) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        }
                        Text(
                            text = peerName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = overlayTextColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val durText = if (session?.status == CallStatus.ACCEPTED.value) {
                            val m = durationSeconds / 60
                            val s = durationSeconds % 60
                            "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')} • $connectionStatus"
                        } else if (isIncomingRinging) {
                            if (isVideoCall) "Incoming Video Call..." else "Incoming Voice Call..."
                        } else {
                            connectionStatus
                        }
                        Text(
                            text = durText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = overlayTextColor.copy(alpha = 0.85f)
                        )
                        val accessoryBadge = when {
                            audioRoute == CallAudioRoute.BLUETOOTH -> "Bluetooth Audio Connected"
                            audioRoute == CallAudioRoute.WIRED_HEADSET -> "Earphone Connected"
                            connectedAccessory == CallAudioRoute.BLUETOOTH -> "Bluetooth Available"
                            connectedAccessory == CallAudioRoute.WIRED_HEADSET -> "Earphone Available"
                            else -> null
                        }
                        if (accessoryBadge != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = glass.accent.copy(alpha = 0.18f),
                                border = BorderStroke(0.8.dp, glass.accent.copy(alpha = 0.55f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (accessoryBadge.contains("Bluetooth")) {
                                            Icons.Default.BluetoothAudio
                                        } else {
                                            Icons.Default.Headset
                                        },
                                        contentDescription = null,
                                        tint = glass.accent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = accessoryBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = overlayTextColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    if (!hasAudioPerm) {
                        GlassCard(contentPadding = PaddingValues(16.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Microphone permission is required for voice/video calls.")
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(onClick = {
                                    permLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
                                }) { Text("Grant Call Permissions") }
                            }
                        }
                    }

                    val dockContainer = if (isVideoAccepted) Color(0xCC0F172A) else glass.smokyOverlay
                    val dockLabelColor = if (isVideoAccepted || glass.isDark) Color.White else Color(0xFF0F172A)
                    val neutralButtonFill = if (isVideoAccepted) {
                        Color.White.copy(alpha = 0.22f)
                    } else if (glass.isDark) {
                        Color(0xFF1E293B)
                    } else {
                        Color(0xFFDDE5EE)
                    }
                    val activeToggleFill = if (isVideoAccepted) Color.White else glass.accent

                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = dockContainer,
                        border = BorderStroke(
                            1.dp,
                            Brush.verticalGradient(listOf(glass.glassHighlight, glass.glassBorder))
                        ),
                        shadowElevation = 18.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
                            if (isIncomingRinging) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    CallControlButton(
                                        icon = Icons.Default.CallEnd,
                                        label = "Decline",
                                        containerColor = DestructiveRed,
                                        labelColor = dockLabelColor,
                                        onClick = {
                                            viewModel.declineIncomingCall(callId)
                                            onCallFinished()
                                        },
                                        modifier = Modifier.testTag("decline_call_button")
                                    )
                                    CallControlButton(
                                        icon = Icons.Default.Call,
                                        label = "Accept",
                                        containerColor = OnlineGreen,
                                        labelColor = dockLabelColor,
                                        onClick = {
                                            if (!hasAudioPerm) {
                                                permLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA))
                                            } else {
                                                viewModel.joinOrAcceptCall(callId)
                                            }
                                        },
                                        modifier = Modifier.testTag("accept_call_button")
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CallControlButton(
                                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                        label = if (isMuted) "Unmute" else "Mute",
                                        containerColor = if (isMuted) activeToggleFill else neutralButtonFill,
                                        labelColor = dockLabelColor,
                                        onClick = { viewModel.toggleMute() }
                                    )
                                    val (routeIcon, routeLabel, routeActive) = when {
                                        audioRoute == CallAudioRoute.BLUETOOTH ->
                                            Triple(Icons.Default.BluetoothAudio, "Bluetooth", true)
                                        audioRoute == CallAudioRoute.WIRED_HEADSET ->
                                            Triple(Icons.Default.Headset, "Earphone", true)
                                        isSpeakerOn || audioRoute == CallAudioRoute.SPEAKER ->
                                            Triple(Icons.Default.VolumeUp, "Speaker", true)
                                        connectedAccessory == CallAudioRoute.BLUETOOTH ->
                                            Triple(Icons.Default.BluetoothAudio, "Bluetooth", false)
                                        connectedAccessory == CallAudioRoute.WIRED_HEADSET ->
                                            Triple(Icons.Default.Headset, "Earphone", false)
                                        else ->
                                            Triple(Icons.Default.VolumeUp, "Speaker", false)
                                    }
                                    CallControlButton(
                                        icon = routeIcon,
                                        label = routeLabel,
                                        containerColor = if (routeActive) glass.accent else neutralButtonFill,
                                        labelColor = dockLabelColor,
                                        onClick = { viewModel.toggleSpeaker() }
                                    )
                                    if (isVideoCall) {
                                        CallControlButton(
                                            icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                            label = "Video",
                                            containerColor = if (isVideoEnabled) neutralButtonFill else activeToggleFill,
                                            labelColor = dockLabelColor,
                                            onClick = { viewModel.toggleVideo() }
                                        )
                                        CallControlButton(
                                            icon = Icons.Default.Cameraswitch,
                                            label = "Flip",
                                            containerColor = neutralButtonFill,
                                            labelColor = dockLabelColor,
                                            onClick = { viewModel.switchCamera() }
                                        )
                                    }
                                    CallControlButton(
                                        icon = Icons.Default.CallEnd,
                                        label = "End",
                                        containerColor = DestructiveRed,
                                        labelColor = dockLabelColor,
                                        onClick = {
                                            viewModel.endActiveCall(callId)
                                            onCallFinished()
                                        },
                                        modifier = Modifier.testTag("end_call_button")
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
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    labelColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentTint = if (containerColor.alpha > 0.45f && containerColor.luminance() > 0.5f) {
        Color(0xFF0F172A)
    } else {
        Color.White
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(containerColor)
        ) {
            Icon(icon, contentDescription = label, tint = contentTint)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = labelColor
        )
    }
}
