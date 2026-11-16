package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.media.LimonMediaResolver
import com.example.media.VoicePlaybackState
import com.example.model.BubbleStyleOption
import com.example.model.CallSession
import com.example.model.CallStatus
import com.example.model.CallType
import com.example.model.ChatMessage
import com.example.model.ChatWallpaperPreset
import com.example.model.DeliveryStatus
import com.example.model.MessageType
import com.example.model.PublicProfile
import com.example.ui.theme.DestructiveRed
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassConfirmDialog
import com.example.ui.theme.IosGlassIconButton
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.MessageStatusTicks
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.UserAvatar
import com.example.ui.theme.VoiceWaveformView
import com.example.ui.theme.chatWallpaperStops
import com.example.ui.theme.iosCombinedTap
import com.example.ui.theme.iosTap
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.BreakIterator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleItem(
    message: ChatMessage,
    isMine: Boolean,
    isSelected: Boolean,
    isHighlighted: Boolean = false,
    showIncomingAvatar: Boolean = false,
    peerDisplayName: String = "",
    peerPhotoUrl: String? = null,
    currentUserId: String = "",
    animationsEnabled: Boolean = true,
    bubbleStyle: BubbleStyleOption,
    voicePlayback: VoicePlaybackState,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onSwipeReply: () -> Unit,
    onReplyQuoteClick: ((String) -> Unit)? = null,
    onOpenImage: (String) -> Unit,
    onOpenVideo: (String) -> Unit,
    onOpenViewOnceMedia: ((ChatMessage) -> Unit)? = null,
    onOpenDocument: ((ChatMessage) -> Unit)? = null,
    onToggleVoicePlay: () -> Unit,
    onSeekVoice: ((Float) -> Unit)? = null,
    onCycleVoiceSpeed: () -> Unit,
    onOpenContact: (String) -> Unit,
    onReactionClick: (String) -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val is24Hour = DateFormat.is24HourFormat(context)

    val dragOffset = remember(message.id) { Animatable(0f) }
    var hapticTriggered by remember(message.id) { mutableStateOf(false) }

    var appeared by remember(message.id) { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(message.id) {
        appeared = true
    }
    val entryAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = if (animationsEnabled) tween(200) else tween(0),
        label = "msg_entry_alpha"
    )
    val entryScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.94f,
        animationSpec = if (animationsEnabled) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        } else tween(0),
        label = "msg_entry_scale"
    )

    val highlightOverlay by animateColorAsState(
        targetValue = if (isHighlighted) glass.accent.copy(alpha = 0.24f) else Color.Transparent,
        animationSpec = tween(durationMillis = 320),
        label = "msg_highlight"
    )

    val corner = when (bubbleStyle) {
        BubbleStyleOption.LIQUID_PILL -> 20.dp
        BubbleStyleOption.CLASSIC_IOS -> 16.dp
        BubbleStyleOption.COMPACT_GLASS -> 12.dp
    }
    val bubbleShape = if (isMine) {
        RoundedCornerShape(topStart = corner, topEnd = corner, bottomStart = corner, bottomEnd = 5.dp)
    } else {
        RoundedCornerShape(topStart = corner, topEnd = corner, bottomStart = 5.dp, bottomEnd = corner)
    }

    val bubbleBrush = when {
        isSelected -> Brush.linearGradient(listOf(glass.accent.copy(alpha = 0.55f), glass.accent.copy(alpha = 0.42f)))
        isMine -> Brush.linearGradient(listOf(glass.sentBubbleStart, glass.sentBubbleEnd))
        else -> Brush.linearGradient(listOf(glass.receivedBubble, glass.receivedBubble))
    }
    val textColor = if (isMine) glass.sentBubbleText else glass.receivedBubbleText
    val msgType = MessageType.from(message.type)
    val timeStr = remember(message.createdAt, is24Hour) {
        message.createdAt?.toDate()?.let { formatShortTime(it, is24Hour) } ?: ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(highlightOverlay)
            .graphicsLayer {
                alpha = entryAlpha
                scaleX = entryScale
                scaleY = entryScale
            }
    ) {
        if (dragOffset.value > 14f) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Reply,
                contentDescription = null,
                tint = glass.accent.copy(alpha = (dragOffset.value / 70f).coerceIn(0f, 1f)),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(20.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 2.dp)
                .offset { IntOffset(dragOffset.value.roundToInt(), 0) }
                .pointerInput(message.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffset.value > 70f) onSwipeReply()
                            hapticTriggered = false
                            scope.launch {
                                dragOffset.animateTo(
                                    0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            hapticTriggered = false
                            scope.launch { dragOffset.animateTo(0f) }
                        },
                        onHorizontalDrag = { _, delta ->
                            val next = (dragOffset.value + delta).coerceIn(0f, 112f)
                            if (next > 70f && !hapticTriggered) {
                                hapticTriggered = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else if (next < 55f) {
                                hapticTriggered = false
                            }
                            scope.launch { dragOffset.snapTo(next) }
                        }
                    )
                },
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isMine) {
                if (showIncomingAvatar) {
                    UserAvatar(
                        displayName = peerDisplayName.ifBlank { "Contact" },
                        photoUrl = peerPhotoUrl,
                        size = 26.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    Spacer(modifier = Modifier.width(32.dp))
                }
            }

            val isSingleEmoji = remember(msgType, message.text, message.deletedForEveryone, message.replyToText, message.forwardedFrom) {
                msgType == MessageType.TEXT &&
                    !message.deletedForEveryone &&
                    message.replyToText.isNullOrBlank() &&
                    message.forwardedFrom.isNullOrBlank() &&
                    isSingleEmojiMessage(message.text)
            }

            Column(
                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
                modifier = Modifier.widthIn(max = 290.dp)
            ) {
                if (isSingleEmoji) {
                    Column(
                        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) glass.accent.copy(alpha = 0.25f) else Color.Transparent)
                            .iosCombinedTap(
                                pressedScale = 0.94f,
                                pressedAlpha = 0.90f,
                                onClick = onClick,
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    (onLongClick ?: onClick).invoke()
                                }
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("message_bubble_${message.id}")
                    ) {
                        AnimatedSingleEmojiContent(
                            emoji = message.text.trim(),
                            animationsEnabled = animationsEnabled
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (glass.isDark) Color(0x990F172A) else Color(0xB8FFFFFF)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isMine) {
                                    MessageStatusTicks(status = DeliveryStatus.from(message.deliveryStatus))
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = bubbleShape,
                        color = Color.Transparent,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isMine) Color.White.copy(alpha = 0.24f) else glass.glassBorder
                        ),
                        shadowElevation = if (isMine) 3.dp else 2.dp,
                        modifier = Modifier
                            .clip(bubbleShape)
                            .background(bubbleBrush)
                            .iosCombinedTap(
                                pressedScale = 0.975f,
                                pressedAlpha = 0.90f,
                                onClick = onClick,
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    (onLongClick ?: onClick).invoke()
                                }
                            )
                            .testTag("message_bubble_${message.id}")
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)) {
                            if (!message.forwardedFrom.isNullOrBlank()) {
                                Text(
                                    text = "Forwarded from ${message.forwardedFrom}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontStyle = FontStyle.Italic,
                                    color = textColor.copy(alpha = 0.82f)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                            }

                            if (!message.replyToText.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isMine) Color.Black.copy(alpha = 0.20f)
                                            else glass.accent.copy(alpha = 0.10f)
                                        )
                                        .clickable(enabled = !message.replyToId.isNullOrBlank() && onReplyQuoteClick != null) {
                                            message.replyToId?.let { id -> onReplyQuoteClick?.invoke(id) }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (isMine) Color.White else glass.accent)
                                    )
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Column {
                                        Text(
                                            text = message.replyToSenderName ?: "Reply",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isMine) Color.White else glass.accent
                                        )
                                        Text(
                                            text = message.replyToText,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            color = textColor.copy(alpha = 0.86f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                            }

                            if (message.deletedForEveryone) {
                                Text(
                                    text = "This message was deleted",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = FontStyle.Italic,
                                    color = textColor.copy(alpha = 0.65f)
                                )
                            } else {
                                when (msgType) {
                                    MessageType.IMAGE -> {
                                        if (message.isViewOnce) {
                                            ViewOnceMediaBubbleCard(
                                                url = message.mediaUrl,
                                                isVideo = false,
                                                isMine = isMine,
                                                isExpired = message.viewOnceViewed,
                                                textColor = textColor,
                                                accentColor = glass.accent,
                                                messageId = message.id,
                                                onClick = { onOpenViewOnceMedia?.invoke(message) }
                                            )
                                            Spacer(modifier = Modifier.height(5.dp))
                                        } else {
                                            message.mediaUrl?.let { url ->
                                                AsyncImage(
                                                    model = url,
                                                    contentDescription = "Shared photo",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(188.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable { onOpenImage(url) }
                                                )
                                                Spacer(modifier = Modifier.height(5.dp))
                                            }
                                        }
                                    }

                                    MessageType.VIDEO -> {
                                        if (message.isViewOnce) {
                                            ViewOnceMediaBubbleCard(
                                                url = message.mediaUrl,
                                                isVideo = true,
                                                isMine = isMine,
                                                isExpired = message.viewOnceViewed,
                                                textColor = textColor,
                                                accentColor = glass.accent,
                                                messageId = message.id,
                                                onClick = { onOpenViewOnceMedia?.invoke(message) }
                                            )
                                            Spacer(modifier = Modifier.height(5.dp))
                                        } else {
                                            message.mediaUrl?.let { url ->
                                                VideoFrameThumbnail(
                                                    url = url,
                                                    accentColor = glass.accent,
                                                    onClick = { onOpenVideo(url) }
                                                )
                                                Spacer(modifier = Modifier.height(5.dp))
                                            }
                                        }
                                    }

                                MessageType.VOICE -> {
                                    val isThisPlaying = voicePlayback.playingMessageId == message.id && voicePlayback.isPlaying
                                    val isThisActive = voicePlayback.playingMessageId == message.id
                                    val progress = if (isThisActive) voicePlayback.progress else 0f
                                    val displayMs = if (isThisActive && voicePlayback.currentMs > 0L) {
                                        voicePlayback.currentMs
                                    } else {
                                        (message.mediaDurationMs ?: 1000L)
                                    }
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.widthIn(min = 204.dp)
                                        ) {
                                            IosGlassIconButton(
                                                onClick = onToggleVoicePlay,
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isMine) Color.White.copy(alpha = 0.24f)
                                                        else glass.accent.copy(alpha = 0.16f)
                                                    )
                                            ) {
                                                Icon(
                                                    imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = if (isThisPlaying) "Pause voice message" else "Play voice message",
                                                    tint = if (isMine) Color.White else glass.accent
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                VoiceWaveformView(
                                                    amplitudes = message.waveform?.map { it.toFloat() } ?: emptyList(),
                                                    progress = progress,
                                                    barColor = if (isMine) Color.White else glass.accent,
                                                    trackColor = textColor.copy(alpha = 0.28f),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    onSeek = if (isThisActive) onSeekVoice else null
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = formatDurationMs(displayMs),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = textColor.copy(alpha = 0.82f)
                                                    )
                                                    Text(
                                                        text = "${voicePlayback.speed}x",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = textColor,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable(onClick = onCycleVoiceSpeed)
                                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (isThisActive && !voicePlayback.error.isNullOrBlank()) {
                                            Text(
                                                text = voicePlayback.error,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DestructiveRed
                                            )
                                        }
                                    }
                                }

                                MessageType.DOCUMENT -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = if (isMine) 0.18f else 0.08f))
                                            .clickable {
                                                if (onOpenDocument != null) {
                                                    onOpenDocument(message)
                                                } else {
                                                    message.mediaUrl?.let { url ->
                                                        runCatching {
                                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                        }
                                                    }
                                                }
                                            }
                                            .padding(9.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isMine) Color.White.copy(alpha = 0.2f)
                                                    else glass.accent.copy(alpha = 0.16f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Description,
                                                contentDescription = null,
                                                tint = if (isMine) Color.White else glass.accent
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(9.dp))
                                        Column {
                                            Text(
                                                text = message.mediaName ?: "Document",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val kb = ((message.mediaSizeBytes ?: 0L) / 1024L).coerceAtLeast(1L)
                                            Text(
                                                text = "$kb KB • Tap to open",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = textColor.copy(alpha = 0.78f)
                                            )
                                        }
                                    }
                                }

                                MessageType.LOCATION -> {
                                    LocationMessageCard(
                                        latitude = message.latitude ?: 0.0,
                                        longitude = message.longitude ?: 0.0,
                                        locationName = message.locationName ?: message.text.takeIf { !it.startsWith("Location (") },
                                        isMine = isMine,
                                        textColor = textColor,
                                        accentColor = glass.accent,
                                        onOpenMaps = {
                                            val lat = message.latitude ?: 0.0
                                            val lng = message.longitude ?: 0.0
                                            val label = Uri.encode(message.locationName ?: "Shared Location")
                                            val geo = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
                                            val launched = runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, geo))
                                            }.isSuccess
                                            if (!launched) {
                                                runCatching {
                                                    val webUri = Uri.parse("https://maps.google.com/?q=$lat,$lng")
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                                }
                                            }
                                        }
                                    )
                                }

                                MessageType.CONTACT -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = if (isMine) 0.18f else 0.08f))
                                            .clickable {
                                                message.contactUid?.let(onOpenContact)
                                            }
                                            .padding(9.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isMine) Color.White.copy(alpha = 0.22f)
                                                    else glass.accent.copy(alpha = 0.16f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = "Contact",
                                                tint = if (isMine) Color.White else glass.accent
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(9.dp))
                                        Column {
                                            Text(
                                                text = message.contactName ?: "Contact",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textColor
                                            )
                                            Text(
                                                text = "@${message.contactUsername ?: ""}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = textColor.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }

                                MessageType.TEXT -> Unit
                            }

                                if (message.text.isNotBlank() &&
                                    msgType != MessageType.LOCATION &&
                                    msgType != MessageType.CONTACT &&
                                    !(message.isViewOnce && message.viewOnceViewed)
                                ) {
                                    Text(
                                        text = message.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = textColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.align(Alignment.End),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (message.editedAt != null && !message.deletedForEveryone) {
                                    Text(
                                        text = "edited",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.72f)
                                    )
                                }
                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor.copy(alpha = 0.78f)
                                )
                                if (isMine && !message.deletedForEveryone) {
                                    MessageStatusTicks(
                                        status = DeliveryStatus.from(message.deliveryStatus),
                                        tintOverride = Color.White.copy(alpha = 0.88f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (message.reactions.isNotEmpty() && !message.deletedForEveryone) {
                    val counts = remember(message.reactions) {
                        message.reactions.values.groupingBy { it }.eachCount()
                    }
                    val myReaction = message.reactions[currentUserId]
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        counts.forEach { (emoji, count) ->
                            val isReactedByMe = myReaction == emoji
                            val bounceScale by animateFloatAsState(
                                targetValue = if (isReactedByMe) 1.06f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "reaction_bounce"
                            )
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isReactedByMe) glass.accent.copy(alpha = 0.24f) else glass.glassSurfaceElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (isReactedByMe) glass.accent else glass.glassBorder
                                ),
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = bounceScale
                                        scaleY = bounceScale
                                    }
                                    .clickable { onReactionClick(emoji) }
                            ) {
                                Text(
                                    text = if (count > 1) "$emoji $count" else emoji,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isReactedByMe) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun isSingleEmojiMessage(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isEmpty() || trimmed.length > 16) return false
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(trimmed)
    val firstStart = iterator.first()
    val firstEnd = iterator.next()
    if (firstStart == BreakIterator.DONE || firstEnd == BreakIterator.DONE) return false
    if (iterator.next() != BreakIterator.DONE) return false

    var hasEmojiSymbol = false
    var i = 0
    while (i < trimmed.length) {
        val cp = trimmed.codePointAt(i)
        if (cp in 0x1F000..0x1FAFF ||
            cp in 0x2600..0x27BF ||
            cp == 0x2764 ||
            cp == 0x200D ||
            cp == 0xFE0F ||
            Character.getType(cp) == Character.OTHER_SYMBOL.toInt()
        ) {
            hasEmojiSymbol = true
        } else if (Character.isLetterOrDigit(cp)) {
            return false
        }
        i += Character.charCount(cp)
    }
    return hasEmojiSymbol
}

@Composable
private fun AnimatedSingleEmojiContent(
    emoji: String,
    animationsEnabled: Boolean
) {
    if (!animationsEnabled) {
        Text(
            text = emoji,
            fontSize = 52.sp,
            modifier = Modifier.padding(4.dp)
        )
        return
    }
    val infinite = rememberInfiniteTransition(label = "single_emoji_$emoji")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "emoji_phase"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 680),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji_pulse"
    )
    val tilt by infinite.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emoji_tilt"
    )

    val isCrying = emoji.contains("😭") || emoji.contains("😢") || emoji.contains("😿")
    val isBrokenHeart = emoji.contains("💔")
    val isHeart = !isBrokenHeart && (emoji.contains("❤️") || emoji.contains("❤") || emoji.contains("💖") || emoji.contains("💗") || emoji.contains("😍") || emoji.contains("🥰"))
    val isLaugh = emoji.contains("😂") || emoji.contains("🤣")
    val isFire = emoji.contains("🔥")
    val isParty = emoji.contains("🎉") || emoji.contains("🥳")
    val isAngry = emoji.contains("😡") || emoji.contains("🤬")

    Box(
        modifier = Modifier
            .size(86.dp)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when {
                isCrying -> {
                    val drop1Y = h * (0.45f + 0.45f * phase)
                    val drop2Y = h * (0.45f + 0.45f * ((phase + 0.5f) % 1f))
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 1f - phase * 0.7f),
                        radius = 4.2.dp.toPx(),
                        center = Offset(w * 0.30f, drop1Y)
                    )
                    drawCircle(
                        color = Color(0xFF0EA5E9).copy(alpha = 1f - ((phase + 0.5f) % 1f) * 0.7f),
                        radius = 4.2.dp.toPx(),
                        center = Offset(w * 0.70f, drop2Y)
                    )
                }
                isBrokenHeart -> {
                    val crackAlpha = (1f - phase).coerceIn(0.25f, 0.9f)
                    drawCircle(
                        color = Color(0xFFEF4444).copy(alpha = 0.22f * crackAlpha),
                        radius = w * (0.34f + 0.12f * phase),
                        center = Offset(w * 0.5f, h * 0.5f)
                    )
                    drawLine(
                        color = Color(0xFFFFD166).copy(alpha = crackAlpha),
                        start = Offset(w * 0.5f, h * 0.28f),
                        end = Offset(w * 0.46f, h * 0.52f),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFFFFD166).copy(alpha = crackAlpha),
                        start = Offset(w * 0.46f, h * 0.52f),
                        end = Offset(w * 0.54f, h * 0.74f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                isHeart -> {
                    for (idx in 0..2) {
                        val p = (phase + idx * 0.33f) % 1f
                        val xOff = w * (0.28f + idx * 0.22f)
                        val yOff = h * (0.72f - p * 0.62f)
                        drawCircle(
                            color = Color(0xFFF43F5E).copy(alpha = (1f - p) * 0.75f),
                            radius = 3.5.dp.toPx(),
                            center = Offset(xOff, yOff)
                        )
                    }
                }
                isFire -> {
                    for (idx in 0..3) {
                        val p = (phase + idx * 0.25f) % 1f
                        val xOff = w * (0.32f + 0.36f * ((idx % 2) * 0.6f + 0.2f))
                        val yOff = h * (0.68f - p * 0.58f)
                        drawCircle(
                            color = Color(0xFFF59E0B).copy(alpha = 1f - p),
                            radius = 3.dp.toPx(),
                            center = Offset(xOff, yOff)
                        )
                    }
                }
                isParty -> {
                    val colors = listOf(Color(0xFF10B981), Color(0xFF0EA5E9), Color(0xFFF43F5E), Color(0xFFF59E0B))
                    colors.forEachIndexed { idx, c ->
                        val angle = (phase * 360f + idx * 90f) * (Math.PI / 180.0)
                        val dist = w * (0.26f + 0.18f * phase)
                        drawCircle(
                            color = c.copy(alpha = 1f - phase * 0.65f),
                            radius = 3.2.dp.toPx(),
                            center = Offset(
                                (w / 2f + cos(angle) * dist).toFloat(),
                                (h / 2f + sin(angle) * dist).toFloat()
                            )
                        )
                    }
                }
                isAngry -> {
                    drawCircle(
                        color = Color(0xFFEF4444).copy(alpha = 0.16f + 0.12f * (pulse - 0.94f)),
                        radius = w * 0.44f,
                        center = Offset(w / 2f, h / 2f)
                    )
                }
                else -> {
                    val sparkAngle = phase * 6.283f
                    drawCircle(
                        color = Color(0xFF34D399).copy(alpha = 0.55f),
                        radius = 2.8.dp.toPx(),
                        center = Offset(
                            (w / 2f + cos(sparkAngle) * w * 0.40f),
                            (h / 2f + sin(sparkAngle) * h * 0.40f)
                        )
                    )
                }
            }
        }

        val scaleFactor = when {
            isHeart || isParty -> pulse
            isBrokenHeart -> 0.96f + (1f - phase) * 0.10f
            else -> 0.98f + (pulse - 0.94f) * 0.45f
        }
        val rotFactor = when {
            isLaugh || isCrying -> tilt
            isAngry -> tilt * 1.4f
            isBrokenHeart -> if (phase > 0.7f) tilt * 0.7f else 0f
            else -> tilt * 0.45f
        }
        val bounceY = when {
            isCrying -> kotlin.math.abs(sin(phase * Math.PI * 2)).toFloat() * -4f
            isFire -> (pulse - 1f) * -18f
            else -> 0f
        }

        Text(
            text = emoji,
            fontSize = 52.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = scaleFactor
                scaleY = scaleFactor
                rotationZ = rotFactor
                translationY = bounceY
            }
        )
    }
}

@Composable
private fun ViewOnceMediaBubbleCard(
    url: String?,
    isVideo: Boolean,
    isMine: Boolean,
    isExpired: Boolean,
    textColor: Color,
    accentColor: Color,
    messageId: String,
    onClick: () -> Unit
) {
    if (isExpired) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = if (isMine) 0.18f else 0.08f))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("view_once_expired_$messageId"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VisibilityOff,
                contentDescription = "Expired media",
                tint = textColor.copy(alpha = 0.78f),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (isVideo) "Video Expair" else "Photo Expair",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.85f)
            )
        }
        return
    }

    val blurMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(radius = 28.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(182.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F172A))
            .clickable(onClick = onClick)
            .testTag("view_once_blurred_$messageId"),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank() && !isVideo) {
            AsyncImage(
                model = url,
                contentDescription = "View once photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(blurMod)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.45f),
                            accentColor.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.55f)
                        )
                    )
                )
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.22f),
                border = BorderStroke(1.5.dp, Color.White),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "1",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isMine) {
                    if (isVideo) "View-once video • Tap to preview" else "View-once photo • Tap to preview"
                } else {
                    if (isVideo) "Tap to view video once" else "Tap to view photo once"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun VideoFrameThumbnail(
    url: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val thumbBitmap by produceState<Bitmap?>(initialValue = null, url) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val db = runCatching {
                    FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
                }.getOrNull()
                val localFile = LimonMediaResolver.resolveToLocalFile(context, db, url, "video.mp4")
                val retriever = MediaMetadataRetriever()
                try {
                    if (localFile != null && localFile.exists()) {
                        retriever.setDataSource(localFile.absolutePath)
                    } else if (url.startsWith("http://") || url.startsWith("https://")) {
                        retriever.setDataSource(url, emptyMap())
                    } else {
                        return@runCatching null
                    }
                    retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } finally {
                    runCatching { retriever.release() }
                }
            }.getOrNull()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val bmp = thumbBitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Video thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.24f))
            )
        }
        Surface(
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play video",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun LocationMessageCard(
    latitude: Double,
    longitude: Double,
    locationName: String?,
    isMine: Boolean,
    textColor: Color,
    accentColor: Color,
    onOpenMaps: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(min = 208.dp, max = 266.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = if (isMine) 0.16f else 0.06f))
            .clickable(onClick = onOpenMaps)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(98.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFD8F3DC), Color(0xFFE0F2FE), Color(0xFFCCFBF1))
                    )
                )
                drawRoundRect(
                    color = Color(0xFF95D5B2).copy(alpha = 0.55f),
                    topLeft = Offset(size.width * 0.08f, size.height * 0.12f),
                    size = Size(size.width * 0.28f, size.height * 0.36f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawRoundRect(
                    color = Color(0xFF93C5FD).copy(alpha = 0.45f),
                    topLeft = Offset(size.width * 0.68f, size.height * 0.52f),
                    size = Size(size.width * 0.25f, size.height * 0.38f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                val roadColor = Color.White.copy(alpha = 0.85f)
                drawLine(roadColor, Offset(0f, size.height * 0.32f), Offset(size.width, size.height * 0.44f), strokeWidth = 5f)
                drawLine(roadColor, Offset(0f, size.height * 0.72f), Offset(size.width, size.height * 0.60f), strokeWidth = 3.5f)
                drawLine(roadColor, Offset(size.width * 0.38f, 0f), Offset(size.width * 0.46f, size.height), strokeWidth = 4.5f)
                drawLine(roadColor, Offset(size.width * 0.72f, 0f), Offset(size.width * 0.64f, size.height), strokeWidth = 3f)
                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                drawCircle(color = accentColor.copy(alpha = 0.22f), radius = 24.dp.toPx(), center = center)
                drawCircle(color = accentColor.copy(alpha = 0.38f), radius = 12.dp.toPx(), center = center)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location pin",
                    tint = DestructiveRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(
                text = locationName?.takeIf { it.isNotBlank() } ?: "Shared Location",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.78f)
                )
                Text(
                    text = "Open in Maps",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isMine) Color.White else accentColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isMine) Color.White.copy(alpha = 0.2f) else accentColor.copy(alpha = 0.14f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}

data class CallLogDisplay(
    val title: String,
    val subtitle: String,
    val isOutgoing: Boolean,
    val isMissedOrDeclined: Boolean,
    val callType: CallType
)

fun formatCallDurationCompact(durationSeconds: Long): String {
    if (durationSeconds <= 0L) return ""
    return when {
        durationSeconds < 60L -> "${durationSeconds}s"
        durationSeconds < 120L -> "1 minute"
        else -> "${durationSeconds / 60L} minutes"
    }
}

fun formatCallClockTime(date: Date, is24Hour: Boolean = false): String {
    val pattern = if (is24Hour) "HH:mm" else "h:mm a"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
}

fun formatCallClockTime(date: Date, context: Context): String =
    formatCallClockTime(date, DateFormat.is24HourFormat(context))

fun formatCallLogDisplay(
    call: CallSession,
    currentUserId: String,
    is24Hour: Boolean = false
): CallLogDisplay {
    val isOutgoing = call.callerId == currentUserId
    val status = CallStatus.from(call.status)
    val callType = CallType.from(call.callType)
    val isMissedOrDeclined = status == CallStatus.MISSED || status == CallStatus.DECLINED

    val title = when {
        status == CallStatus.MISSED -> if (isOutgoing) {
            if (callType == CallType.VIDEO) "Unanswered Video Call" else "Cancelled Call"
        } else {
            if (callType == CallType.VIDEO) "Missed Video Call" else "Missed Call"
        }
        status == CallStatus.DECLINED ->
            if (callType == CallType.VIDEO) "Declined Video Call" else "Declined Call"
        isOutgoing ->
            if (callType == CallType.VIDEO) "Outgoing Video Call" else "Outgoing Call"
        else ->
            if (callType == CallType.VIDEO) "Incoming Video Call" else "Incoming Call"
    }

    val timeStr = call.createdAt?.toDate()?.let { formatCallClockTime(it, is24Hour) } ?: ""
    val detailParen = when {
        call.durationSeconds > 0L -> "(${formatCallDurationCompact(call.durationSeconds)})"
        status == CallStatus.MISSED -> "(Missed)"
        status == CallStatus.DECLINED -> "(Declined)"
        status == CallStatus.RINGING -> "(Ringing)"
        else -> "(Ended)"
    }
    val subtitle = listOf(timeStr, detailParen).filter { it.isNotBlank() }.joinToString(" ")

    return CallLogDisplay(
        title = title,
        subtitle = subtitle,
        isOutgoing = isOutgoing,
        isMissedOrDeclined = isMissedOrDeclined,
        callType = callType
    )
}

@Composable
fun CallLogBubbleItem(
    call: CallSession,
    currentUserId: String,
    showIncomingAvatar: Boolean = false,
    peerDisplayName: String = "",
    peerPhotoUrl: String? = null,
    animationsEnabled: Boolean = true,
    bubbleStyle: BubbleStyleOption = BubbleStyleOption.CLASSIC_IOS,
    onCallBack: (CallType) -> Unit,
    onDeleteForMe: (() -> Unit)? = null
) {
    val glass = LocalLiquidGlassColors.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val info = remember(call, currentUserId, is24Hour) { formatCallLogDisplay(call, currentUserId, is24Hour) }
    val isMine = info.isOutgoing
    var showMenuDialog by remember { mutableStateOf(false) }

    var appeared by remember(call.id) { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(call.id) { appeared = true }
    val entryAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = if (animationsEnabled) tween(200) else tween(0),
        label = "call_log_alpha"
    )
    val entryScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.94f,
        animationSpec = if (animationsEnabled) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        } else tween(0),
        label = "call_log_scale"
    )

    val corner = when (bubbleStyle) {
        BubbleStyleOption.LIQUID_PILL -> 20.dp
        BubbleStyleOption.CLASSIC_IOS -> 16.dp
        BubbleStyleOption.COMPACT_GLASS -> 12.dp
    }
    val bubbleShape = if (isMine) {
        RoundedCornerShape(topStart = corner, topEnd = corner, bottomStart = corner, bottomEnd = 5.dp)
    } else {
        RoundedCornerShape(topStart = corner, topEnd = corner, bottomStart = 5.dp, bottomEnd = corner)
    }
    val bubbleBrush = if (isMine) {
        Brush.linearGradient(listOf(glass.sentBubbleStart, glass.sentBubbleEnd))
    } else {
        Brush.linearGradient(listOf(glass.receivedBubble, glass.receivedBubble))
    }

    val titleColor = if (isMine) glass.sentBubbleText else glass.receivedBubbleText
    val subColor = if (isMine) glass.sentBubbleText.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant
    val arrowTint = when {
        info.isMissedOrDeclined -> if (isMine) Color(0xFFFFD4D4) else DestructiveRed
        isMine -> Color(0xFFD1FAE5)
        else -> OnlineGreen
    }
    val callBackTint = if (isMine) Color.White else if (glass.isDark) Color(0xFF3898FF) else Color(0xFF007AFF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .graphicsLayer {
                alpha = entryAlpha
                scaleX = entryScale
                scaleY = entryScale
            },
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            if (showIncomingAvatar) {
                UserAvatar(
                    displayName = peerDisplayName.ifBlank { "Contact" },
                    photoUrl = peerPhotoUrl,
                    size = 26.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else {
                Spacer(modifier = Modifier.width(32.dp))
            }
        }

        Surface(
            shape = bubbleShape,
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.dp,
                color = if (isMine) Color.White.copy(alpha = 0.24f) else glass.glassBorder
            ),
            shadowElevation = if (isMine) 3.dp else 2.dp,
            modifier = Modifier
                .widthIn(min = 204.dp, max = 276.dp)
                .clip(bubbleShape)
                .background(bubbleBrush)
                .iosCombinedTap(
                    pressedScale = 0.97f,
                    pressedAlpha = 0.88f,
                    onClick = { showMenuDialog = true },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenuDialog = true
                    }
                )
                .testTag("inbox_call_log_${call.id}")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = info.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when {
                                info.isMissedOrDeclined -> Icons.AutoMirrored.Filled.CallMissed
                                isMine -> Icons.AutoMirrored.Filled.CallMade
                                else -> Icons.AutoMirrored.Filled.CallReceived
                            },
                            contentDescription = null,
                            tint = arrowTint,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = info.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = subColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                IosGlassIconButton(
                    onClick = { onCallBack(info.callType) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (info.callType == CallType.VIDEO) Icons.Outlined.Videocam else Icons.Outlined.Call,
                        contentDescription = "Call back",
                        tint = callBackTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showMenuDialog) {
        GlassConfirmDialog(
            onDismissRequest = { showMenuDialog = false },
            title = info.title,
            body = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = info.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ActionSheetRow(
                        icon = if (info.callType == CallType.VIDEO) Icons.Outlined.Videocam else Icons.Outlined.Call,
                        label = "Call Back",
                        onClick = {
                            showMenuDialog = false
                            onCallBack(info.callType)
                        }
                    )
                    if (onDeleteForMe != null) {
                        HorizontalDivider(color = glass.glassBorder.copy(alpha = 0.45f))
                        ActionSheetRow(
                            icon = Icons.Default.Delete,
                            label = "Delete for Me",
                            tint = DestructiveRed,
                            onClick = {
                                showMenuDialog = false
                                onDeleteForMe()
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMenuDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun GlassTypingComposerBar(
    text: String,
    onTextChange: (String) -> Unit,
    replyingTo: ChatMessage?,
    editingMessage: ChatMessage?,
    isRecordingVoice: Boolean,
    isVoicePaused: Boolean = false,
    recordingElapsedMs: Long,
    liveVoiceAmplitudes: List<Double> = emptyList(),
    showEmojiPicker: Boolean,
    enterToSend: Boolean = false,
    animationsEnabled: Boolean = true,
    onToggleEmojiPicker: () -> Unit,
    onEmojiSelected: (String) -> Unit,
    onCancelReplyOrEdit: () -> Unit,
    onSend: () -> Unit,
    onOpenAttachments: () -> Unit,
    onOpenCamera: () -> Unit,
    onStartRecording: () -> Unit,
    onPauseRecording: () -> Unit = {},
    onResumeRecording: () -> Unit = {},
    onStopAndSendRecording: () -> Unit,
    onCancelRecording: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val haptic = LocalHapticFeedback.current
    val showSendButton = text.isNotBlank() || editingMessage != null

    val smokyPillFill = if (glass.isDark) Color(0xC616202F) else Color(0xCEFAFBFD)
    val smokyPillBorder = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (glass.isDark) 0.24f else 0.94f),
            Color.White.copy(alpha = if (glass.isDark) 0.08f else 0.50f)
        )
    )
    val iosBlue = if (glass.isDark) Color(0xFF3898FF) else Color(0xFF007AFF)
    val iconSlate = if (glass.isDark) Color.White else Color(0xFF333942)
    val subtleIconSlate = if (glass.isDark) Color(0xFFCBD5E1) else Color(0xFF6B7280)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        AnimatedVisibility(visible = replyingTo != null || editingMessage != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = smokyPillFill,
                border = BorderStroke(1.dp, smokyPillBorder),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (editingMessage != null) Icons.Default.Edit else Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = glass.accent,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (editingMessage != null) "Editing message" else "Replying",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = glass.accent
                        )
                        Text(
                            text = (editingMessage?.text ?: replyingTo?.text ?: "").ifBlank { "Media message" },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IosGlassIconButton(onClick = onCancelReplyOrEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(17.dp))
                    }
                }
            }
        }

        AnimatedVisibility(visible = showEmojiPicker) {
            ScreenshotOneEmojiDrawer(
                onEmojiPicked = onEmojiSelected,
                onBackspace = {
                    if (text.isNotEmpty()) {
                        val it = BreakIterator.getCharacterInstance()
                        it.setText(text)
                        val end = it.last()
                        val prev = it.previous()
                        if (prev != BreakIterator.DONE && prev >= 0) {
                            onTextChange(text.substring(0, prev))
                        } else {
                            onTextChange("")
                        }
                    }
                }
            )
        }

        if (isRecordingVoice) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = smokyPillFill,
                border = BorderStroke(1.dp, DestructiveRed.copy(alpha = 0.38f)),
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DestructiveRed.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IosGlassIconButton(onClick = onCancelRecording, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Cancel voice recording", tint = DestructiveRed)
                    }
                    Text(
                        text = formatDurationMs(recordingElapsedMs),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = DestructiveRed
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    VoiceWaveformView(
                        amplitudes = liveVoiceAmplitudes.map { it.toFloat() },
                        progress = 1f,
                        barColor = if (isVoicePaused) DestructiveRed.copy(alpha = 0.45f) else DestructiveRed,
                        trackColor = DestructiveRed.copy(alpha = 0.2f),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IosGlassIconButton(
                        onClick = if (isVoicePaused) onResumeRecording else onPauseRecording,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(glass.glassSurface)
                    ) {
                        Icon(
                            imageVector = if (isVoicePaused) Icons.Default.Mic else Icons.Default.Pause,
                            contentDescription = if (isVoicePaused) "Resume recording" else "Pause recording",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IosGlassIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onStopAndSendRecording()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(glass.accent)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send voice message", tint = Color.White)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = smokyPillFill,
                    border = BorderStroke(1.dp, smokyPillBorder),
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(10.dp, CircleShape, spotColor = Color(0x260F172A))
                        .iosTap(pressedScale = 0.90f, pressedAlpha = 0.78f, onClick = onOpenAttachments)
                        .testTag("chat_attach_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach media",
                            tint = iconSlate,
                            modifier = Modifier
                                .size(21.dp)
                                .rotate(32f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = smokyPillFill,
                    border = BorderStroke(1.dp, smokyPillBorder),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = Color(0x240F172A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (text.isEmpty()) {
                                Text(
                                    text = "Message",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = subtleIconSlate
                                )
                            }
                            BasicTextField(
                                value = text,
                                onValueChange = onTextChange,
                                maxLines = 5,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(iosBlue),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = if (enterToSend) ImeAction.Send else ImeAction.Default
                                ),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (enterToSend && text.isNotBlank()) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSend()
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("chat_message_input")
                            )
                        }

                        IosGlassIconButton(
                            onClick = onToggleEmojiPicker,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("chat_emoji_toggle")
                        ) {
                            Icon(
                                imageVector = if (showEmojiPicker) Icons.Filled.EmojiEmotions else Icons.Outlined.EmojiEmotions,
                                contentDescription = "Emojis",
                                tint = if (showEmojiPicker) iosBlue else subtleIconSlate,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }

                AnimatedContent(
                    targetState = showSendButton,
                    transitionSpec = {
                        if (animationsEnabled) {
                            (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()) togetherWith
                                (scaleOut() + fadeOut())
                        } else {
                            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        }
                    },
                    label = "composer_action_swap"
                ) { isSend ->
                    if (isSend) {
                        Surface(
                            shape = CircleShape,
                            color = glass.accent,
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(10.dp, CircleShape, spotColor = glass.accent.copy(alpha = 0.35f))
                                .iosTap(pressedScale = 0.88f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSend()
                                }
                                .testTag("chat_send_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send message",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = smokyPillFill,
                            border = BorderStroke(1.dp, smokyPillBorder),
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(10.dp, CircleShape, spotColor = Color(0x260F172A))
                                .iosTap(pressedScale = 0.90f, pressedAlpha = 0.78f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onStartRecording()
                                }
                                .testTag("chat_mic_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Mic,
                                    contentDescription = "Record voice message",
                                    tint = iconSlate,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class EmojiEntry(val emoji: String, val keywords: String, val category: Int)

@Composable
private fun ScreenshotOneEmojiDrawer(
    onEmojiPicked: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    var selectedCategory by remember { mutableIntStateOf(0) }
    var selectedMode by remember { mutableStateOf("Emoji") } // Emoji | GIFs | Stickers
    var searchQuery by remember { mutableStateOf("") }

    val categoryIcons = remember {
        listOf(
            Icons.Default.AccessTime to "Recent",
            Icons.Outlined.EmojiEmotions to "Smileys",
            Icons.Default.Pets to "Animals",
            Icons.Default.Fastfood to "Food",
            Icons.Default.SportsSoccer to "Activities",
            Icons.Default.DirectionsCar to "Travel",
            Icons.Default.Lightbulb to "Objects"
        )
    }

    val emojiCatalog = remember {
        listOf(
            EmojiEntry("😂", "laugh joy haha funny tears", 0),
            EmojiEntry("❤️", "love heart red", 0),
            EmojiEntry("😘", "kiss love heart", 0),
            EmojiEntry("😍", "love heart eyes", 0),
            EmojiEntry("👍", "thumbs up ok good", 0),
            EmojiEntry("😭", "cry sob sad tears", 0),
            EmojiEntry("💔", "broken heart sad hurt", 0),
            EmojiEntry("🔥", "fire hot lit", 0),
            EmojiEntry("😁", "grin happy smile", 1),
            EmojiEntry("😊", "blush smile happy", 1),
            EmojiEntry("🙂", "smile ok", 1),
            EmojiEntry("😌", "relieved calm", 1),
            EmojiEntry("😔", "sad pensive", 1),
            EmojiEntry("😄", "happy grin", 1),
            EmojiEntry("😒", "unamused side eye", 1),
            EmojiEntry("😳", "flushed shocked", 1),
            EmojiEntry("😜", "wink tongue crazy", 1),
            EmojiEntry("😉", "wink", 1),
            EmojiEntry("😃", "smiley happy", 1),
            EmojiEntry("😢", "cry tear sad", 1),
            EmojiEntry("😱", "scream shock wow", 1),
            EmojiEntry("😡", "angry mad", 1),
            EmojiEntry("🥰", "love hearts smile", 1),
            EmojiEntry("🤣", "rofl laugh", 1),
            EmojiEntry("😎", "cool sunglasses", 1),
            EmojiEntry("🥳", "party celebrate", 1),
            EmojiEntry("🤯", "mind blown shocked", 1),
            EmojiEntry("😴", "sleep tired", 1),
            EmojiEntry("🙈", "monkey see no evil", 2),
            EmojiEntry("🙊", "monkey speak no evil", 2),
            EmojiEntry("🐶", "dog puppy", 2),
            EmojiEntry("🐱", "cat kitty", 2),
            EmojiEntry("🦁", "lion", 2),
            EmojiEntry("🦋", "butterfly", 2),
            EmojiEntry("🌹", "rose flower", 2),
            EmojiEntry("🌸", "flower blossom", 2),
            EmojiEntry("🍋", "lemon citrus limon", 3),
            EmojiEntry("🍎", "apple fruit", 3),
            EmojiEntry("🍕", "pizza food", 3),
            EmojiEntry("🍔", "burger food", 3),
            EmojiEntry("☕", "coffee tea", 3),
            EmojiEntry("🎂", "cake birthday", 3),
            EmojiEntry("🍓", "strawberry", 3),
            EmojiEntry("🍦", "ice cream", 3),
            EmojiEntry("🎉", "party confetti", 4),
            EmojiEntry("⚽", "football soccer", 4),
            EmojiEntry("🏀", "basketball", 4),
            EmojiEntry("🎮", "game gaming", 4),
            EmojiEntry("🎵", "music note", 4),
            EmojiEntry("🏆", "trophy win", 4),
            EmojiEntry("🚗", "car drive", 5),
            EmojiEntry("✈️", "plane flight travel", 5),
            EmojiEntry("🚀", "rocket launch", 5),
            EmojiEntry("🌙", "moon night", 5),
            EmojiEntry("☀️", "sun bright", 5),
            EmojiEntry("☔", "rain umbrella", 5),
            EmojiEntry("💡", "idea bulb", 6),
            EmojiEntry("💎", "diamond gem", 6),
            EmojiEntry("📌", "pin mark", 6),
            EmojiEntry("👀", "eyes look", 6),
            EmojiEntry("💯", "100 percent perfect", 6),
            EmojiEntry("✅", "check done ok", 6),
            EmojiEntry("❗", "exclamation alert", 6),
            EmojiEntry("💥", "boom explosion", 6),
            EmojiEntry("🙏", "pray thanks please", 6),
            EmojiEntry("👏", "clap applause", 6),
            EmojiEntry("🤝", "handshake deal", 6),
            EmojiEntry("🫶", "heart hands", 6)
        )
    }

    val stickerPacks = remember {
        listOf("🥳 Party", "😭 Sobbing", "💔 Broken", "🔥 On Fire", "😍 Adore", "🚀 Rocket", "🍋 Limon", "💯 100%", "🙈 Shy", "🤝 Deal", "✨ Magic", "🎉 Congrats")
    }
    val gifQuickReactions = remember {
        listOf("😂", "😭", "❤️", "💔", "🔥", "🎉", "😡", "🥳", "😍", "🙏", "😱", "🤣")
    }

    val visibleEmojis = remember(selectedCategory, searchQuery) {
        val q = searchQuery.trim()
        when {
            q.isNotEmpty() -> emojiCatalog.filter {
                it.emoji.contains(q) || it.keywords.contains(q, ignoreCase = true)
            }
            selectedCategory == 0 -> emojiCatalog.take(40)
            else -> emojiCatalog.filter { it.category == selectedCategory }.ifEmpty { emojiCatalog }
        }
    }

    val drawerBg = if (glass.isDark) Color(0xDE16202F) else Color(0xEAF5F7FA)
    val drawerBorder = Color.White.copy(alpha = if (glass.isDark) 0.22f else 0.90f)
    val pillSwitchBg = if (glass.isDark) Color(0xE81E2A3D) else Color(0xF5FFFFFF)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = drawerBg,
        border = BorderStroke(1.dp, drawerBorder),
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .testTag("emoji_drawer_container")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp, start = 10.dp, end = 10.dp, bottom = 52.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categoryIcons.forEachIndexed { index, (icon, desc) ->
                        val active = selectedCategory == index
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) glass.accent.copy(alpha = 0.16f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    selectedMode = "Emoji"
                                    selectedCategory = index
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = desc,
                                tint = if (active) glass.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (glass.isDark) Color(0x33FFFFFF) else Color(0xFFE6EAF0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(glass.accent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("emoji_search_input")
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(17.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (selectedMode) {
                        "GIFs" -> {
                            Text(
                                text = "Animated Emoji Reactions",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            gifQuickReactions.chunked(4).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    row.forEach { emoji ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = glass.glassSurface,
                                            modifier = Modifier
                                                .padding(2.dp)
                                                .iosTap(pressedScale = 0.88f) { onEmojiPicked(emoji) }
                                        ) {
                                            Text(
                                                text = emoji,
                                                fontSize = 32.sp,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        "Stickers" -> {
                            stickerPacks.chunked(3).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    row.forEach { sticker ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = glass.glassSurface,
                                            modifier = Modifier
                                                .weight(1f)
                                                .iosTap(pressedScale = 0.90f) { onEmojiPicked(sticker) }
                                        ) {
                                            Text(
                                                text = sticker,
                                                style = MaterialTheme.typography.labelMedium,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            visibleEmojis.chunked(8).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (i in 0 until 8) {
                                        val entry = row.getOrNull(i)
                                        if (entry != null) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(CircleShape)
                                                    .iosTap(pressedScale = 0.82f) { onEmojiPicked(entry.emoji) }
                                                    .padding(vertical = 3.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = entry.emoji, fontSize = 24.sp)
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom floating switcher pill (Emoji | GIFs | Stickers) + Backspace circle
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.size(36.dp))
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = pillSwitchBg,
                    border = BorderStroke(1.dp, drawerBorder),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Emoji", "GIFs", "Stickers").forEach { tab ->
                            val active = selectedMode == tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (active) {
                                            if (glass.isDark) Color(0x38FFFFFF) else Color(0xFFE7EBEF)
                                        } else Color.Transparent
                                    )
                                    .clickable { selectedMode = tab }
                                    .padding(horizontal = 13.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = pillSwitchBg,
                    border = BorderStroke(1.dp, drawerBorder),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .size(36.dp)
                        .iosTap(pressedScale = 0.88f, onClick = onBackspace)
                        .testTag("emoji_backspace_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Backspace",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return "${mins}:${secs.toString().padStart(2, '0')}"
}

@Composable
private fun SmokyGlassBottomSheetContainer(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(glass.smokyScrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = showContent,
                enter = scaleIn(
                    initialScale = 0.92f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                ) + fadeIn(),
                exit = scaleOut(targetScale = 0.94f) + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = glass.smokyOverlay,
                    border = BorderStroke(
                        1.dp,
                        Brush.verticalGradient(listOf(glass.glassHighlight, glass.glassBorder))
                    ),
                    shadowElevation = 24.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun AttachmentPickerSheet(
    onDismiss: () -> Unit,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onOpenCamera: () -> Unit,
    onPickDocument: () -> Unit,
    onShareLocation: () -> Unit,
    onShareContact: () -> Unit
) {
    SmokyGlassBottomSheetContainer(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .testTag("attachment_glass_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Share to Conversation", style = MaterialTheme.typography.titleLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AttachmentTile(Icons.Default.Image, "Photo", onPickImage)
                AttachmentTile(Icons.Default.Videocam, "Video", onPickVideo)
                AttachmentTile(Icons.Default.CameraAlt, "Camera", onOpenCamera)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AttachmentTile(Icons.Default.Description, "Document", onPickDocument)
                AttachmentTile(Icons.Default.LocationOn, "Location", onShareLocation)
                AttachmentTile(Icons.Default.ContactPage, "Contact", onShareContact)
            }
        }
    }
}

@Composable
private fun AttachmentTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .iosTap(onClick = onClick)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(glass.accent.copy(alpha = 0.20f))
                .border(1.dp, glass.glassHighlight.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = glass.accent)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun MessageActionsSheet(
    message: ChatMessage,
    isMine: Boolean,
    onDismiss: () -> Unit,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onForward: () -> Unit,
    onCopy: () -> Unit,
    onPin: () -> Unit,
    onEdit: () -> Unit,
    onSelect: () -> Unit,
    onSaveMedia: (() -> Unit)? = null,
    onShareMedia: (() -> Unit)? = null,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val quickEmojis = listOf("👍", "❤️", "😂", "🔥", "😮")
    val allEmojis = remember {
        listOf(
            "❤️", "👍", "👎", "😂", "🤣", "😊", "😍", "🥰",
            "😘", "😮", "😢", "😭", "😡", "🔥", "🎉", "👏",
            "🙏", "🤝", "✨", "💯", "🫶", "🥳", "🤯", "🤔",
            "😴", "👀", "🚀", "🍋", "💎", "⚡", "🌟", "🙌"
        )
    }
    val hasDownloadableMedia = !message.mediaUrl.isNullOrBlank() &&
        message.type in listOf(MessageType.IMAGE.value, MessageType.VIDEO.value, MessageType.DOCUMENT.value)
    var showOverlay by remember { mutableStateOf(false) }
    var showAllEmojis by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showOverlay = true }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(glass.smokyScrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = showOverlay,
                enter = scaleIn(
                    initialScale = 0.86f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
                exit = scaleOut(targetScale = 0.92f) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .widthIn(max = 368.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = glass.smokyOverlay,
                        border = BorderStroke(
                            1.dp,
                            Brush.verticalGradient(listOf(glass.glassHighlight, glass.glassBorder))
                        ),
                        shadowElevation = 20.dp,
                        modifier = Modifier.testTag("message_smoky_reaction_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            quickEmojis.forEach { emoji ->
                                Text(
                                    text = emoji,
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            onReact(emoji)
                                            onDismiss()
                                        }
                                        .padding(6.dp)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = glass.accent.copy(alpha = 0.20f),
                                border = BorderStroke(1.dp, glass.glassHighlight.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable { showAllEmojis = !showAllEmojis }
                                    .testTag("reaction_plus_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (showAllEmojis) Icons.Default.Close else Icons.Default.Add,
                                        contentDescription = "All reactions",
                                        tint = glass.accent,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = showAllEmojis) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = glass.smokyOverlay,
                            border = BorderStroke(
                                1.dp,
                                Brush.verticalGradient(listOf(glass.glassHighlight, glass.glassBorder))
                            ),
                            shadowElevation = 22.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .testTag("all_reactions_emoji_grid")
                        ) {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                allEmojis.chunked(8).forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        row.forEach { emoji ->
                                            Text(
                                                text = emoji,
                                                style = MaterialTheme.typography.titleLarge,
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        onReact(emoji)
                                                        onDismiss()
                                                    }
                                                    .padding(4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val previewLabel = message.text.ifBlank {
                        when (MessageType.from(message.type)) {
                            MessageType.IMAGE -> "Photo"
                            MessageType.VIDEO -> "Video"
                            MessageType.VOICE -> "Voice Message"
                            MessageType.DOCUMENT -> message.mediaName ?: "Document"
                            MessageType.LOCATION -> message.locationName ?: "Shared Location"
                            MessageType.CONTACT -> message.contactName ?: "Contact Card"
                            MessageType.TEXT -> "Message"
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = glass.smokyOverlay,
                        border = BorderStroke(1.dp, glass.glassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = previewLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = glass.smokyOverlay,
                        border = BorderStroke(
                            1.dp,
                            Brush.verticalGradient(listOf(glass.glassHighlight, glass.glassBorder))
                        ),
                        shadowElevation = 24.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("message_smoky_action_bar")
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 4.dp)
                        ) {
                            ActionSheetRow(Icons.AutoMirrored.Filled.Reply, "Reply", onClick = { onReply(); onDismiss() })
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                            ActionSheetRow(Icons.Default.Forward, "Forward", onClick = { onForward(); onDismiss() })
                            if (message.text.isNotBlank()) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                                ActionSheetRow(Icons.Default.ContentCopy, "Copy Text", onClick = { onCopy(); onDismiss() })
                            }
                            if (hasDownloadableMedia && onSaveMedia != null) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                                ActionSheetRow(Icons.Default.Download, "Save to Device", onClick = { onSaveMedia(); onDismiss() })
                            }
                            if (hasDownloadableMedia && onShareMedia != null) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                                ActionSheetRow(Icons.Default.Share, "Share Media", onClick = { onShareMedia(); onDismiss() })
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                            ActionSheetRow(Icons.Default.PushPin, "Pin Message", onClick = { onPin(); onDismiss() })
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                            ActionSheetRow(Icons.Default.Check, "Select Message", onClick = { onSelect(); onDismiss() })
                            if (isMine && !message.deletedForEveryone && message.type == MessageType.TEXT.value) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                                ActionSheetRow(Icons.Default.Edit, "Edit Message", onClick = { onEdit(); onDismiss() })
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                            ActionSheetRow(Icons.Default.Delete, "Delete for Me", tint = DestructiveRed, onClick = { onDeleteForMe(); onDismiss() })
                            if (isMine && !message.deletedForEveryone) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                                ActionSheetRow(Icons.Default.DeleteForever, "Delete for Everyone", tint = DestructiveRed, onClick = { onDeleteForEveryone(); onDismiss() })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionSheetRow(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .iosTap(pressedScale = 0.97f, pressedAlpha = 0.72f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}

@Composable
fun ChatWallpaperSheet(
    currentPreset: ChatWallpaperPreset,
    blurEnabled: Boolean,
    showToPeer: Boolean = false,
    onToggleShowToPeer: (Boolean) -> Unit = {},
    onSelectPreset: (ChatWallpaperPreset) -> Unit,
    onPickCustomPhoto: () -> Unit,
    onToggleBlur: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val presets = listOf(
        ChatWallpaperPreset.PASTEL_MINT,
        ChatWallpaperPreset.LAVENDER_SKY,
        ChatWallpaperPreset.WARM_PEACH,
        ChatWallpaperPreset.MIDNIGHT_AURORA
    )
    SmokyGlassBottomSheetContainer(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .testTag("wallpaper_glass_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Chat Wallpaper & Blur", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presets.forEach { preset ->
                    val selected = currentPreset == preset
                    val stops = chatWallpaperStops(preset, glass.isDark)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectPreset(preset) }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 62.dp, height = 88.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.verticalGradient(stops))
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) glass.accent else glass.glassBorder,
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(glass.accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(preset.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onPickCustomPhoto)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 62.dp, height = 88.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(glass.accent.copy(alpha = 0.14f))
                            .border(
                                width = if (currentPreset == ChatWallpaperPreset.CUSTOM) 2.5.dp else 1.dp,
                                color = if (currentPreset == ChatWallpaperPreset.CUSTOM) glass.accent else glass.glassBorder,
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Wallpaper, contentDescription = "Custom photo", tint = glass.accent)
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text("Custom", style = MaterialTheme.typography.labelSmall)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(glass.glassSurface)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Show User", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Display this wallpaper on your chat partner's screen too.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = showToPeer,
                    onCheckedChange = onToggleShowToPeer,
                    modifier = Modifier.testTag("wallpaper_show_user_switch")
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(glass.glassSurface)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Frosted Wallpaper Blur", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Softens background gradients & photos for crisp bubble legibility.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = blurEnabled, onCheckedChange = onToggleBlur)
            }
        }
    }
}

@Composable
fun ContactPickerSheet(
    title: String,
    contacts: List<PublicProfile>,
    onDismiss: () -> Unit,
    onSelect: (PublicProfile) -> Unit
) {
    SmokyGlassBottomSheetContainer(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (contacts.isEmpty()) {
                Text(
                    "No discovered contacts yet. Search users in Contacts tab first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                contacts.take(12).forEach { profile ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onSelect(profile)
                            onDismiss()
                        },
                        contentPadding = PaddingValues(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(
                                displayName = profile.displayName,
                                photoUrl = profile.photoUrl,
                                size = 38.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(profile.displayName, style = MaterialTheme.typography.titleMedium)
                                Text("@${profile.username}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
