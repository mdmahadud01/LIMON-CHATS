package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.media.LimonMediaUploader
import com.example.media.LocationHelper
import com.example.model.AppSettings
import com.example.model.CallSession
import com.example.model.CallType
import com.example.model.ChatMessage
import com.example.model.ChatWallpaperPreset
import com.example.model.MessageType
import com.example.model.PrivacyScope
import com.example.model.PublicProfile
import com.example.model.UiState
import com.example.notifications.NotificationHelper
import com.example.ui.theme.ChatWallpaperBackground
import com.example.ui.theme.DestructiveRed
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassConfirmDialog
import com.example.ui.theme.IosGlassIconButton
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.TypingDotsIndicator
import com.example.ui.theme.UserAvatar
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.iosTap
import com.example.viewmodel.ConversationViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

private sealed interface ChatTimelineItem {
    val sortTimeMs: Long
    val key: String
    val isMine: Boolean

    data class Message(
        val message: ChatMessage,
        override val sortTimeMs: Long,
        override val isMine: Boolean
    ) : ChatTimelineItem {
        override val key: String = "msg_${message.id}"
    }

    data class CallLog(
        val call: CallSession,
        override val sortTimeMs: Long,
        override val isMine: Boolean
    ) : ChatTimelineItem {
        override val key: String = "call_${call.id}"
    }
}

private data class PendingMediaDraft(
    val type: MessageType,
    val fileName: String,
    val uri: Uri? = null,
    val localFile: File? = null
)

@Composable
fun PrivateChatScreen(
    viewModel: ConversationViewModel,
    settings: AppSettings,
    discoveredContacts: List<PublicProfile>,
    isNetworkOnline: Boolean = true,
    isChatLocked: Boolean = false,
    capturedCameraPath: String? = null,
    onConsumeCapturedCameraPath: () -> Unit = {},
    onToggleLock: () -> Unit = {},
    onSetWallpaperPreset: (ChatWallpaperPreset) -> Unit = {},
    onSetCustomWallpaperUri: (String?) -> Unit = {},
    onSetWallpaperBlur: (Boolean) -> Unit = {},
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenCamera: () -> Unit,
    onOpenImage: (String) -> Unit,
    onOpenVideo: (String) -> Unit,
    onOpenViewOnceImage: ((url: String, chatId: String?, messageId: String?, secureMode: Boolean) -> Unit)? = null,
    onOpenViewOnceVideo: ((url: String, chatId: String?, messageId: String?, secureMode: Boolean) -> Unit)? = null,
    onStartCall: (CallType) -> Unit,
    onBlockPeer: (PublicProfile) -> Unit,
    onUnblockPeer: (String) -> Unit = {},
    onDeleteChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val glass = LocalLiquidGlassColors.current
    val density = LocalDensity.current
    val is24Hour = DateFormat.is24HourFormat(context)

    val chatThread by viewModel.chatThread.collectAsStateWithLifecycle()
    val peerProfile by viewModel.peerProfile.collectAsStateWithLifecycle()
    val chatPref by viewModel.chatPreference.collectAsStateWithLifecycle()
    val isPeerTyping by viewModel.isPeerTyping.collectAsStateWithLifecycle()
    val isBlocked by viewModel.isBlockedByMe.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val callLogs by viewModel.callLogs.collectAsStateWithLifecycle()
    val replyingTo by viewModel.replyingTo.collectAsStateWithLifecycle()
    val editingMessage by viewModel.editingMessage.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedMessageIds.collectAsStateWithLifecycle()
    val pendingUpload by viewModel.pendingUpload.collectAsStateWithLifecycle()
    val feedback by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val scrollToBottomSignal by viewModel.scrollToBottomSignal.collectAsStateWithLifecycle()
    val isRecording by viewModel.voiceRecorder.isRecording.collectAsStateWithLifecycle()
    val isVoicePaused by viewModel.isVoicePaused.collectAsStateWithLifecycle()
    val recordingElapsed by viewModel.voiceRecorder.elapsedMs.collectAsStateWithLifecycle()
    val liveAmplitudes by viewModel.liveVoiceAmplitudes.collectAsStateWithLifecycle()
    val voicePlayback by viewModel.voicePlayer.state.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var inputText by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchMatchCursor by remember { mutableIntStateOf(0) }
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showWallpaperSheet by remember { mutableStateOf(false) }
    var wallpaperShowToPeer by remember(chatThread?.sharedWallpaperPreset) {
        mutableStateOf(!chatThread?.sharedWallpaperPreset.isNullOrBlank())
    }
    var actionTargetMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var forwardTargetMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showContactSharePicker by remember { mutableStateOf(false) }
    var showDeleteBlockedDialog by remember { mutableStateOf(false) }
    var pendingMediaDraft by remember { mutableStateOf<PendingMediaDraft?>(null) }

    var topBarHeightDp by remember { mutableStateOf(104.dp) }
    var bottomBarHeightDp by remember { mutableStateOf(80.dp) }

    val listState = rememberLazyListState()

    // Lifecycle foreground tracking for accurate read vs delivered receipts & background alerts
    DisposableEffect(lifecycleOwner, viewModel.chatId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.setChatForeground(true)
                    NotificationHelper.cancelNotification(context, viewModel.chatId.hashCode())
                    NotificationHelper.clearConversationHistory(viewModel.chatId)
                }
                Lifecycle.Event.ON_STOP -> viewModel.setChatForeground(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            viewModel.setChatForeground(false)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(highlightedMessageId) {
        if (highlightedMessageId != null) {
            delay(1600L)
            highlightedMessageId = null
        }
    }

    LaunchedEffect(editingMessage) {
        editingMessage?.let { inputText = it.text }
    }

    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeFeedback()
        }
    }

    LaunchedEffect(capturedCameraPath) {
        val path = capturedCameraPath
        if (!path.isNullOrBlank()) {
            onConsumeCapturedCameraPath()
            val file = File(path)
            if (file.exists()) {
                pendingMediaDraft = PendingMediaDraft(
                    type = MessageType.IMAGE,
                    fileName = file.name,
                    localFile = file
                )
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val name = resolveFileName(context, it, "photo.jpg")
            pendingMediaDraft = PendingMediaDraft(MessageType.IMAGE, name, uri = it)
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val name = resolveFileName(context, it, "video.mp4")
            pendingMediaDraft = PendingMediaDraft(MessageType.VIDEO, name, uri = it)
        }
    }

    val hasSharedWallpaper = !chatThread?.sharedWallpaperPreset.isNullOrBlank()
    val effectiveWallpaperPreset = if (hasSharedWallpaper) {
        ChatWallpaperPreset.from(chatThread!!.sharedWallpaperPreset)
    } else {
        settings.chatWallpaper
    }
    val effectiveWallpaperUri = if (hasSharedWallpaper) {
        chatThread?.sharedWallpaperUrl
    } else {
        settings.customWallpaperUri
    }
    val effectiveWallpaperBlur = if (hasSharedWallpaper) {
        chatThread?.sharedWallpaperBlur ?: settings.wallpaperBlur
    } else {
        settings.wallpaperBlur
    }

    val wallpaperPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { pickedUri ->
            scope.launch {
                val localUri = copyWallpaperToAppFiles(context, pickedUri)
                if (localUri != null) {
                    onSetCustomWallpaperUri(localUri)
                }
                if (wallpaperShowToPeer) {
                    val db = runCatching {
                        FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
                    }.getOrNull()
                    if (db != null) {
                        LimonMediaUploader.uploadSharedWallpaper(context, db, viewModel.chatId, pickedUri)
                            .onSuccess { sharedUrl ->
                                viewModel.setSharedWallpaper(
                                    preset = ChatWallpaperPreset.CUSTOM,
                                    customUrl = sharedUrl,
                                    blur = effectiveWallpaperBlur
                                )
                            }
                            .onFailure {
                                snackbarHostState.showSnackbar("Failed to share wallpaper photo")
                            }
                    }
                } else if (localUri == null) {
                    snackbarHostState.showSnackbar("Could not load selected wallpaper photo")
                }
            }
        }
    }

    val docPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val name = resolveFileName(context, it, "document.pdf")
            viewModel.uploadAndSendMedia(MessageType.DOCUMENT, name, uri = it)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startVoiceRecording()
        else scope.launch { snackbarHostState.showSnackbar("Microphone permission required for voice messages") }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                LocationHelper.getCurrentLocation(context)
                    .onSuccess { (lat, lng) ->
                        val placeName = LocationHelper.resolvePlaceName(context, lat, lng)
                        viewModel.sendLocation(lat, lng, placeName)
                    }
                    .onFailure { snackbarHostState.showSnackbar(it.localizedMessage ?: "Location unavailable") }
            }
        } else {
            scope.launch { snackbarHostState.showSnackbar("Location permission denied") }
        }
    }

    val displayedMessages = remember(messagesState) {
        (messagesState as? UiState.Success)?.data.orEmpty()
    }
    val timelineItems = remember(displayedMessages, callLogs, viewModel.currentUserId) {
        val msgItems = displayedMessages.map { msg ->
            val ts = msg.createdAt
            val ms = (ts?.seconds ?: (Long.MAX_VALUE / 1000L)) * 1000L + (ts?.nanoseconds ?: 0) / 1_000_000L
            ChatTimelineItem.Message(
                message = msg,
                sortTimeMs = ms,
                isMine = msg.senderId == viewModel.currentUserId
            )
        }
        val callItems = callLogs.map { call ->
            val ts = call.createdAt
            val ms = (ts?.seconds ?: (Long.MAX_VALUE / 1000L)) * 1000L + (ts?.nanoseconds ?: 0) / 1_000_000L
            ChatTimelineItem.CallLog(
                call = call,
                sortTimeMs = ms,
                isMine = call.callerId == viewModel.currentUserId
            )
        }
        (msgItems + callItems).sortedBy { it.sortTimeMs }
    }
    val searchMatchingIndices = remember(displayedMessages, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else displayedMessages.mapIndexedNotNull { idx, msg ->
            if (msg.text.contains(searchQuery, ignoreCase = true) ||
                msg.mediaName?.contains(searchQuery, ignoreCase = true) == true ||
                msg.locationName?.contains(searchQuery, ignoreCase = true) == true
            ) idx else null
        }
    }

    val showScrollToBottom by remember(timelineItems.size) {
        derivedStateOf { timelineItems.isNotEmpty() && listState.canScrollForward }
    }

    fun jumpToMessageId(targetId: String) {
        val idx = timelineItems.indexOfFirst { it is ChatTimelineItem.Message && it.message.id == targetId }
        if (idx >= 0) {
            highlightedMessageId = targetId
            scope.launch {
                val offset = if (displayedMessages.size >= 40) idx + 1 else idx
                listState.animateScrollToItem(offset)
            }
        }
    }

    LaunchedEffect(searchQuery, searchMatchingIndices.size) {
        if (searchMatchingIndices.isNotEmpty()) {
            searchMatchCursor = searchMatchingIndices.lastIndex
            val targetId = displayedMessages[searchMatchingIndices[searchMatchCursor]].id
            jumpToMessageId(targetId)
        }
    }

    LaunchedEffect(scrollToBottomSignal) {
        if (scrollToBottomSignal > 0 && timelineItems.isNotEmpty()) {
            listState.animateScrollToItem(timelineItems.lastIndex)
        }
    }

    val headerGlassColor = glass.smokyOverlay.copy(alpha = if (glass.isDark) 0.78f else 0.78f)

    ChatWallpaperBackground(
        preset = effectiveWallpaperPreset,
        customWallpaperUri = effectiveWallpaperUri,
        blurEnabled = effectiveWallpaperBlur,
        animateOrbs = settings.animationsEnabled
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = messagesState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Loading messages...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    is UiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(state.message, color = DestructiveRed)
                        }
                    }

                    is UiState.Success -> {
                        val displayed = state.data
                        var previousCount by remember { mutableIntStateOf(0) }
                        LaunchedEffect(timelineItems.size) {
                            if (timelineItems.isNotEmpty() && !showSearchBar) {
                                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                val wasAtBottom = previousCount == 0 || lastVisible >= previousCount - 3
                                val latestIsMine = timelineItems.lastOrNull()?.isMine == true
                                if (wasAtBottom || latestIsMine) {
                                    listState.animateScrollToItem(timelineItems.lastIndex)
                                }
                            }
                            previousCount = timelineItems.size
                        }

                        if (timelineItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .navigationBarsPadding()
                                    .imePadding()
                                    .padding(top = topBarHeightDp, bottom = bottomBarHeightDp),
                                contentAlignment = Alignment.Center
                            ) {
                                GlassCard(contentPadding = PaddingValues(18.dp)) {
                                    Text(
                                        text = if (isBlocked) "Blocked conversation" else "Say hello to ${peerProfile?.displayName ?: "your contact"}!",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .navigationBarsPadding()
                                    .imePadding(),
                                contentPadding = PaddingValues(
                                    top = topBarHeightDp + 6.dp,
                                    bottom = bottomBarHeightDp + 8.dp
                                )
                            ) {
                                if (displayed.size >= 40) {
                                    item(key = "load_older") {
                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            TextButton(onClick = { viewModel.loadOlderMessages() }) {
                                                Text("Load older messages")
                                            }
                                        }
                                    }
                                }
                                itemsIndexed(timelineItems, key = { _, item -> item.key }) { index, item ->
                                    val nextIsDifferentSender = index == timelineItems.lastIndex ||
                                        timelineItems[index + 1].isMine != item.isMine
                                    val effectivePeerPhoto = if (isBlocked) null else peerProfile?.photoUrl
                                    when (item) {
                                        is ChatTimelineItem.CallLog -> {
                                            CallLogBubbleItem(
                                                call = item.call,
                                                currentUserId = viewModel.currentUserId,
                                                showIncomingAvatar = !item.isMine && nextIsDifferentSender,
                                                peerDisplayName = peerProfile?.displayName ?: "Contact",
                                                peerPhotoUrl = effectivePeerPhoto,
                                                animationsEnabled = settings.animationsEnabled,
                                                bubbleStyle = settings.bubbleStyle,
                                                onCallBack = onStartCall,
                                                onDeleteForMe = { viewModel.deleteCallLog(item.call.id) }
                                            )
                                        }
                                        is ChatTimelineItem.Message -> {
                                            val msg = item.message
                                            val isMine = item.isMine
                                            MessageBubbleItem(
                                                message = msg,
                                                isMine = isMine,
                                                isSelected = msg.id in selectedIds,
                                                isHighlighted = msg.id == highlightedMessageId,
                                                showIncomingAvatar = !isMine && nextIsDifferentSender,
                                                peerDisplayName = peerProfile?.displayName ?: "Contact",
                                                peerPhotoUrl = effectivePeerPhoto,
                                                currentUserId = viewModel.currentUserId,
                                                animationsEnabled = settings.animationsEnabled,
                                                bubbleStyle = settings.bubbleStyle,
                                                voicePlayback = voicePlayback,
                                                onClick = {
                                                    if (selectedIds.isNotEmpty()) viewModel.toggleMessageSelection(msg.id)
                                                    else actionTargetMessage = msg
                                                },
                                                onLongClick = {
                                                    if (selectedIds.isNotEmpty()) viewModel.toggleMessageSelection(msg.id)
                                                    else actionTargetMessage = msg
                                                },
                                                onSwipeReply = { viewModel.setReplyTarget(msg) },
                                                onReplyQuoteClick = { repliedId -> jumpToMessageId(repliedId) },
                                                onOpenImage = onOpenImage,
                                                onOpenVideo = onOpenVideo,
                                                onOpenViewOnceMedia = { mediaMsg ->
                                                    val msgType = MessageType.from(mediaMsg.type)
                                                    val url = mediaMsg.mediaUrl
                                                    if (mediaMsg.viewOnceViewed) {
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar(
                                                                if (msgType == MessageType.VIDEO) "Video Expair" else "Photo Expair"
                                                            )
                                                        }
                                                    } else if (!url.isNullOrBlank()) {
                                                        val secureForViewer = mediaMsg.senderId != viewModel.currentUserId
                                                        if (msgType == MessageType.VIDEO) {
                                                            if (onOpenViewOnceVideo != null) {
                                                                onOpenViewOnceVideo(url, viewModel.chatId, mediaMsg.id, secureForViewer)
                                                            } else {
                                                                onOpenVideo(url)
                                                            }
                                                        } else {
                                                            if (onOpenViewOnceImage != null) {
                                                                onOpenViewOnceImage(url, viewModel.chatId, mediaMsg.id, secureForViewer)
                                                            } else {
                                                                onOpenImage(url)
                                                            }
                                                        }
                                                    }
                                                },
                                                onOpenDocument = { docMsg ->
                                                    val url = docMsg.mediaUrl ?: return@MessageBubbleItem
                                                    scope.launch {
                                                        val local = viewModel.resolveMediaFile(context, url, docMsg.mediaName)
                                                        if (local != null) {
                                                            val contentUri = FileProvider.getUriForFile(
                                                                context,
                                                                "${context.packageName}.fileprovider",
                                                                local
                                                            )
                                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                                setDataAndType(contentUri, "*/*")
                                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            }
                                                            val ok = runCatching { context.startActivity(intent) }.isSuccess
                                                            if (!ok) {
                                                                snackbarHostState.showSnackbar("No app found to open ${docMsg.mediaName ?: "file"}")
                                                            }
                                                        } else {
                                                            snackbarHostState.showSnackbar("Could not open document")
                                                        }
                                                    }
                                                },
                                                onToggleVoicePlay = { viewModel.toggleVoicePlayback(msg) },
                                                onSeekVoice = { frac -> viewModel.voicePlayer.seekTo(frac) },
                                                onCycleVoiceSpeed = { viewModel.voicePlayer.cycleSpeed() },
                                                onOpenContact = onOpenProfile,
                                                onReactionClick = { emoji -> viewModel.toggleReaction(msg, emoji) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating Smoky Liquid Glass Top Header
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .onSizeChanged { size ->
                            topBarHeightDp = with(density) { size.height.toDp() }
                        }
                ) {
                    if (selectedIds.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .shadow(
                                    elevation = 14.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    ambientColor = glass.accent.copy(alpha = 0.16f),
                                    spotColor = Color(0x2E0F172A)
                                ),
                            shape = RoundedCornerShape(24.dp),
                            color = headerGlassColor,
                            border = BorderStroke(1.dp, glass.glassHighlight.copy(alpha = 0.78f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.clearSelection() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                                }
                                Text(
                                    text = "${selectedIds.size} selected",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.deleteSelectedMessagesForMe() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete selected", tint = DestructiveRed)
                                }
                            }
                        }
                    } else {
                        val headerBorder = Brush.verticalGradient(
                            listOf(glass.glassHighlight.copy(alpha = 0.9f), glass.glassBorder.copy(alpha = 0.5f))
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = headerGlassColor,
                                border = BorderStroke(1.dp, headerBorder),
                                modifier = Modifier
                                    .size(44.dp)
                                    .shadow(12.dp, CircleShape, spotColor = Color(0x2E0F172A))
                            ) {
                                IosGlassIconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("chat_back_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(26.dp),
                                color = headerGlassColor,
                                border = BorderStroke(1.dp, headerBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .shadow(12.dp, RoundedCornerShape(26.dp), spotColor = Color(0x2E0F172A))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(26.dp))
                                        .iosTap(pressedScale = 0.97f, pressedAlpha = 0.82f) {
                                            onOpenProfile(viewModel.peerUid)
                                        }
                                        .padding(start = 5.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val showOnline = !isBlocked &&
                                        peerProfile?.privacyShowOnline != PrivacyScope.NOBODY.value &&
                                        peerProfile?.online == true
                                    val headerPhoto = if (isBlocked) null else peerProfile?.photoUrl
                                    AnimatedContent(
                                        targetState = headerPhoto,
                                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                                        label = "header_avatar_transition"
                                    ) { photoUrl ->
                                        UserAvatar(
                                            displayName = peerProfile?.displayName ?: "Contact",
                                            photoUrl = photoUrl,
                                            isOnline = showOnline,
                                            size = 36.dp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(9.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = peerProfile?.displayName ?: "Limon User",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isPeerTyping && !isBlocked) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                TypingDotsIndicator()
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = "typing...",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = glass.accent
                                                )
                                            }
                                        } else {
                                            val statusLine = when {
                                                isBlocked -> "Blocked"
                                                showOnline -> "online"
                                                peerProfile?.privacyShowLastSeen == PrivacyScope.NOBODY.value -> "last seen recently"
                                                peerProfile?.lastSeen != null -> "last seen ${formatShortTime(peerProfile!!.lastSeen!!.toDate(), is24Hour)}"
                                                else -> "@${peerProfile?.username ?: "user"}"
                                            }
                                            Text(
                                                text = statusLine,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = when {
                                                    isBlocked -> DestructiveRed
                                                    showOnline -> OnlineGreen
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = headerGlassColor,
                                border = BorderStroke(1.dp, headerBorder),
                                modifier = Modifier
                                    .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Color(0x2E0F172A))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    IosGlassIconButton(
                                        onClick = { onStartCall(CallType.VOICE) },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .testTag("chat_voice_call_button")
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "Voice Call", modifier = Modifier.size(19.dp))
                                    }
                                    Box {
                                        IosGlassIconButton(
                                            onClick = { showMenu = true },
                                            modifier = Modifier
                                                .size(40.dp)
                                                .testTag("chat_more_menu_button")
                                        ) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Chat options", modifier = Modifier.size(19.dp))
                                        }
                                        if (showMenu) {
                                            Popup(
                                                alignment = Alignment.TopEnd,
                                                offset = IntOffset(0, 120),
                                                onDismissRequest = { showMenu = false },
                                                properties = PopupProperties(focusable = true)
                                            ) {
                                                SmokyGlassThreeDotMenuBar(
                                                    isMuted = chatPref.muted,
                                                    isArchived = chatPref.archived,
                                                    isLocked = isChatLocked || chatPref.locked,
                                                    isBlocked = isBlocked,
                                                    peerUsername = peerProfile?.username,
                                                    onVideoCall = { showMenu = false; onStartCall(CallType.VIDEO) },
                                                    onSearch = { showMenu = false; showSearchBar = !showSearchBar },
                                                    onWallpaper = { showMenu = false; showWallpaperSheet = true },
                                                    onToggleMute = { showMenu = false; viewModel.toggleMute() },
                                                    onToggleArchive = { showMenu = false; viewModel.toggleArchive() },
                                                    onToggleLock = { showMenu = false; onToggleLock() },
                                                    onClearHistory = { showMenu = false; viewModel.clearChatHistory() },
                                                    onBlockPeer = peerProfile?.let { p ->
                                                        { showMenu = false; onBlockPeer(p) }
                                                    },
                                                    onUnblockPeer = { showMenu = false; onUnblockPeer(viewModel.peerUid) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = !isNetworkOnline) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(WarningAmber.copy(alpha = 0.22f))
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Waiting for network — messages queued offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    AnimatedVisibility(visible = showSearchBar) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            elevated = true,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search in conversation...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                if (searchMatchingIndices.isNotEmpty()) {
                                    Text(
                                        text = "${searchMatchCursor + 1}/${searchMatchingIndices.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            searchMatchCursor = if (searchMatchCursor > 0) searchMatchCursor - 1 else searchMatchingIndices.lastIndex
                                            jumpToMessageId(displayedMessages[searchMatchingIndices[searchMatchCursor]].id)
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous match")
                                    }
                                    IconButton(
                                        onClick = {
                                            searchMatchCursor = (searchMatchCursor + 1) % searchMatchingIndices.size
                                            jumpToMessageId(displayedMessages[searchMatchingIndices[searchMatchCursor]].id)
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next match")
                                    }
                                }
                                IconButton(
                                    onClick = { searchQuery = ""; showSearchBar = false },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close search")
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = !chatThread?.pinnedMessageText.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(glass.glassSurfaceElevated)
                                .clickable {
                                    chatThread?.pinnedMessageId?.let { jumpToMessageId(it) }
                                }
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PushPin, contentDescription = null, tint = glass.accent, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = chatThread?.pinnedMessageText ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.pinMessage(null) }) { Text("Unpin") }
                        }
                    }
                }

                // Floating Glass Scroll-to-Bottom Arrow on bottom-right when scrolled up
                AnimatedVisibility(
                    visible = showScrollToBottom,
                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(end = 14.dp, bottom = bottomBarHeightDp + 10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = glass.smokyOverlay,
                        border = BorderStroke(
                            1.dp,
                            Brush.verticalGradient(listOf(glass.glassHighlight, glass.glassBorder))
                        ),
                        shadowElevation = 14.dp,
                        modifier = Modifier
                            .size(40.dp)
                            .iosTap(pressedScale = 0.88f) {
                                if (timelineItems.isNotEmpty()) {
                                    scope.launch {
                                        listState.animateScrollToItem(timelineItems.lastIndex)
                                    }
                                }
                            }
                            .testTag("scroll_to_bottom_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Scroll to latest messages",
                                tint = glass.accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Floating Translucent Bottom Composer or Blocked Action Bar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .onSizeChanged { size ->
                            bottomBarHeightDp = with(density) { size.height.toDp() }
                        }
                ) {
                    pendingUpload?.let { upload ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentPadding = PaddingValues(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (upload.error != null) "Upload failed: ${upload.error}" else "Uploading ${upload.fileName}...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (upload.error != null) DestructiveRed else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (upload.error != null) {
                                        IconButton(onClick = { viewModel.retryFailedUpload() }) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Retry upload")
                                        }
                                    }
                                    IconButton(onClick = { viewModel.cancelCurrentUpload() }) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancel upload")
                                    }
                                }
                                if (upload.error == null) {
                                    LinearProgressIndicator(
                                        progress = { upload.progress },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    if (isBlocked) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = glass.smokyOverlay,
                            border = BorderStroke(
                                1.dp,
                                Brush.verticalGradient(listOf(glass.glassHighlight, glass.glassBorder))
                            ),
                            shadowElevation = 14.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("blocked_action_bar")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { showDeleteBlockedDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DestructiveRed),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("blocked_delete_chat_button")
                                ) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Delete")
                                }
                                Button(
                                    onClick = { onUnblockPeer(viewModel.peerUid) },
                                    colors = ButtonDefaults.buttonColors(containerColor = glass.accent, contentColor = Color.White),
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("blocked_unblock_button")
                                ) {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Unblock", color = Color.White)
                                }
                            }
                        }
                    } else {
                        GlassTypingComposerBar(
                            text = inputText,
                            onTextChange = {
                                inputText = it
                                viewModel.onInputTextChanged(it)
                            },
                            replyingTo = replyingTo,
                            editingMessage = editingMessage,
                            isRecordingVoice = isRecording,
                            isVoicePaused = isVoicePaused,
                            recordingElapsedMs = recordingElapsed,
                            liveVoiceAmplitudes = liveAmplitudes,
                            showEmojiPicker = showEmojiPicker,
                            enterToSend = settings.enterToSend,
                            animationsEnabled = settings.animationsEnabled,
                            onToggleEmojiPicker = { showEmojiPicker = !showEmojiPicker },
                            onEmojiSelected = { emoji -> inputText += emoji },
                            onCancelReplyOrEdit = {
                                viewModel.setReplyTarget(null)
                                viewModel.setEditTarget(null)
                                inputText = ""
                            },
                            onSend = {
                                val out = inputText
                                inputText = ""
                                showEmojiPicker = false
                                viewModel.sendTextOrApplyEdit(out)
                            },
                            onOpenAttachments = { showAttachmentSheet = true },
                            onOpenCamera = onOpenCamera,
                            onStartRecording = {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            onPauseRecording = { viewModel.pauseVoiceRecording() },
                            onResumeRecording = { viewModel.resumeVoiceRecording() },
                            onStopAndSendRecording = { viewModel.finishAndSendVoiceRecording() },
                            onCancelRecording = { viewModel.cancelVoiceRecording() }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteBlockedDialog) {
        GlassConfirmDialog(
            onDismissRequest = { showDeleteBlockedDialog = false },
            title = "Delete Conversation?",
            body = {
                Text(
                    "This permanently removes this chat thread.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteBlockedDialog = false
                    onDeleteChat(viewModel.chatId)
                    onBack()
                }) {
                    Text("Delete", color = DestructiveRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteBlockedDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAttachmentSheet) {
        AttachmentPickerSheet(
            onDismiss = { showAttachmentSheet = false },
            onPickImage = {
                showAttachmentSheet = false
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onPickVideo = {
                showAttachmentSheet = false
                videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            },
            onOpenCamera = {
                showAttachmentSheet = false
                onOpenCamera()
            },
            onPickDocument = {
                showAttachmentSheet = false
                docPicker.launch(arrayOf("*/*"))
            },
            onShareLocation = {
                showAttachmentSheet = false
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            },
            onShareContact = {
                showAttachmentSheet = false
                showContactSharePicker = true
            }
        )
    }

    if (showWallpaperSheet) {
        ChatWallpaperSheet(
            currentPreset = effectiveWallpaperPreset,
            blurEnabled = effectiveWallpaperBlur,
            showToPeer = wallpaperShowToPeer,
            onToggleShowToPeer = { checked ->
                wallpaperShowToPeer = checked
                if (checked) {
                    viewModel.setSharedWallpaper(
                        preset = effectiveWallpaperPreset,
                        customUrl = if (effectiveWallpaperPreset == ChatWallpaperPreset.CUSTOM) effectiveWallpaperUri else null,
                        blur = effectiveWallpaperBlur
                    )
                } else {
                    viewModel.setSharedWallpaper(null, null, null)
                }
            },
            onSelectPreset = { preset ->
                onSetWallpaperPreset(preset)
                if (wallpaperShowToPeer) {
                    viewModel.setSharedWallpaper(preset, null, effectiveWallpaperBlur)
                }
            },
            onPickCustomPhoto = {
                wallpaperPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onToggleBlur = { blur ->
                onSetWallpaperBlur(blur)
                if (wallpaperShowToPeer) {
                    viewModel.setSharedWallpaper(
                        preset = effectiveWallpaperPreset,
                        customUrl = if (effectiveWallpaperPreset == ChatWallpaperPreset.CUSTOM) effectiveWallpaperUri else null,
                        blur = blur
                    )
                }
            },
            onDismiss = { showWallpaperSheet = false }
        )
    }

    actionTargetMessage?.let { msg ->
        MessageActionsSheet(
            message = msg,
            isMine = msg.senderId == viewModel.currentUserId,
            onDismiss = { actionTargetMessage = null },
            onReact = { emoji -> viewModel.toggleReaction(msg, emoji) },
            onReply = { viewModel.setReplyTarget(msg) },
            onForward = { forwardTargetMessage = msg },
            onCopy = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Message", msg.text))
            },
            onPin = { viewModel.pinMessage(msg) },
            onEdit = { viewModel.setEditTarget(msg) },
            onSelect = { viewModel.toggleMessageSelection(msg.id) },
            onSaveMedia = {
                scope.launch {
                    val resolved = msg.mediaUrl?.let { viewModel.resolveMediaFile(context, it, msg.mediaName) }
                    val saved = saveRemoteMediaToDevice(context, msg, resolved)
                    snackbarHostState.showSnackbar(
                        if (saved) "Saved to device Downloads / Gallery" else "Could not save media"
                    )
                }
            },
            onShareMedia = {
                scope.launch {
                    val resolved = msg.mediaUrl?.let { viewModel.resolveMediaFile(context, it, msg.mediaName) }
                    val ok = shareRemoteMedia(context, msg, resolved)
                    if (!ok) snackbarHostState.showSnackbar("Unable to share media")
                }
            },
            onDeleteForMe = { viewModel.deleteMessageForMe(msg.id) },
            onDeleteForEveryone = { viewModel.deleteMessageForEveryone(msg.id) }
        )
    }

    forwardTargetMessage?.let { msg ->
        ContactPickerSheet(
            title = "Forward Message To",
            contacts = discoveredContacts,
            onDismiss = { forwardTargetMessage = null },
            onSelect = { contact -> viewModel.forwardMessageTo(contact.uid, msg) }
        )
    }

    if (showContactSharePicker) {
        ContactPickerSheet(
            title = "Share Contact Card",
            contacts = discoveredContacts,
            onDismiss = { showContactSharePicker = false },
            onSelect = { contact -> viewModel.sendContactCard(contact) }
        )
    }

    pendingMediaDraft?.let { draft ->
        MediaPreviewComposerOverlay(
            draft = draft,
            onDismiss = { pendingMediaDraft = null },
            onSend = { description, isViewOnce ->
                pendingMediaDraft = null
                viewModel.uploadAndSendMedia(
                    type = draft.type,
                    fileName = draft.fileName,
                    uri = draft.uri,
                    localFile = draft.localFile,
                    caption = description,
                    isViewOnce = isViewOnce
                )
            }
        )
    }
}

@Composable
private fun MediaPreviewComposerOverlay(
    draft: PendingMediaDraft,
    onDismiss: () -> Unit,
    onSend: (description: String, isViewOnce: Boolean) -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    var description by remember(draft) { mutableStateOf("") }
    var isViewOnce by remember(draft) { mutableStateOf(false) }
    var isSelected by remember(draft) { mutableStateOf(true) }
    var isFullPreview by remember(draft) { mutableStateOf(false) }
    val previewScale by animateFloatAsState(
        targetValue = if (isFullPreview) 1f else if (isSelected) 0.94f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "media_preview_scale"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        BackHandler {
            if (isFullPreview) isFullPreview = false else onDismiss()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Center interactive preview (1st tap toggles select/unselect, 2nd tap opens full zoom)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 68.dp, bottom = if (isFullPreview) 18.dp else 94.dp, start = 12.dp, end = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                val model: Any? = draft.uri ?: draft.localFile
                Surface(
                    shape = RoundedCornerShape(if (isFullPreview) 12.dp else 22.dp),
                    border = if (isSelected && !isFullPreview) {
                        BorderStroke(2.dp, Color(0xFF0A84FF))
                    } else null,
                    color = Color(0xFF111827),
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(previewScale)
                        .clip(RoundedCornerShape(if (isFullPreview) 12.dp else 22.dp))
                        .clickable {
                            if (!isSelected) {
                                isSelected = true
                            } else {
                                isFullPreview = !isFullPreview
                            }
                        }
                        .testTag("media_preview_surface")
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = model,
                            contentDescription = draft.fileName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (draft.type == MessageType.VIDEO) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.56f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Text("Video Ready", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        if (!isFullPreview) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color(0xFF0A84FF) else Color.Black.copy(alpha = 0.45f)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (isSelected) "Selected • Tap to Expand" else "Tap to Select",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top bar with Close and (1) View Once toggle
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                }

                Text(
                    text = if (draft.type == MessageType.VIDEO) "Send Video" else "Send Photo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Surface(
                    shape = CircleShape,
                    color = if (isViewOnce) Color(0xFF0A84FF) else Color.White.copy(alpha = 0.16f),
                    border = BorderStroke(
                        1.dp,
                        if (isViewOnce) Color.White else Color.White.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier
                        .clip(CircleShape)
                        .iosTap(pressedScale = 0.9f) { isViewOnce = !isViewOnce }
                        .testTag("view_once_toggle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isViewOnce) Color.White else Color.Transparent)
                                .then(
                                    if (!isViewOnce) Modifier.background(Color.White.copy(alpha = 0.18f)) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "1",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isViewOnce) Color(0xFF0A84FF) else Color.White
                            )
                        }
                        Text(
                            text = if (isViewOnce) "Once On" else "Once",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Bottom Description Input + Send Button
            if (!isFullPreview) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Description...", color = Color.White.copy(alpha = 0.65f)) },
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.12f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.10f),
                            focusedBorderColor = glass.accent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("media_description_input")
                    )

                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) glass.accent else Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .iosTap(enabled = isSelected, pressedScale = 0.9f) {
                                onSend(description.trim(), isViewOnce)
                            }
                            .testTag("media_preview_send_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Media",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun copyWallpaperToAppFiles(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
        val target = File(context.filesDir, "chat_wallpaper_custom.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { out -> input.copyTo(out) }
        } ?: return@withContext null
        Uri.fromFile(target).toString()
    }.getOrNull()
}

@Composable
private fun SmokyGlassThreeDotMenuBar(
    isMuted: Boolean,
    isArchived: Boolean,
    isLocked: Boolean,
    isBlocked: Boolean,
    peerUsername: String?,
    onVideoCall: () -> Unit,
    onSearch: () -> Unit,
    onWallpaper: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleArchive: () -> Unit,
    onToggleLock: () -> Unit,
    onClearHistory: () -> Unit,
    onBlockPeer: (() -> Unit)?,
    onUnblockPeer: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.88f,
            transformOrigin = TransformOrigin(1f, 0f),
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(),
        exit = scaleOut(targetScale = 0.92f, transformOrigin = TransformOrigin(1f, 0f)) + fadeOut()
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = glass.smokyOverlay,
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(listOf(glass.glassHighlight, glass.glassBorder))
            ),
            shadowElevation = 0.dp,
            modifier = Modifier
                .padding(end = 10.dp)
                .widthIn(min = 216.dp, max = 256.dp)
                .testTag("chat_smoky_menu_popup")
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                SmokyMenuRow(Icons.Default.Videocam, "Video Call", onClick = onVideoCall, testTag = "menu_video_call_item")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                SmokyMenuRow(Icons.Default.Search, "Search Messages", onClick = onSearch)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                SmokyMenuRow(Icons.Default.Wallpaper, "Chat Wallpaper", onClick = onWallpaper)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                SmokyMenuRow(
                    if (isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                    if (isMuted) "Unmute Chat" else "Mute Chat",
                    onClick = onToggleMute
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                SmokyMenuRow(
                    if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                    if (isLocked) "Unlock Chat" else "Lock Chat",
                    onClick = onToggleLock,
                    testTag = "menu_lock_chat_item"
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                SmokyMenuRow(Icons.Default.Archive, if (isArchived) "Unarchive Chat" else "Archive Chat", onClick = onToggleArchive)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                SmokyMenuRow(Icons.Default.CleaningServices, "Clear Chat History", tint = DestructiveRed, onClick = onClearHistory)
                if (isBlocked) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                    SmokyMenuRow(Icons.Default.LockOpen, "Unblock @${peerUsername ?: "user"}", tint = glass.accent, onClick = onUnblockPeer)
                } else if (onBlockPeer != null) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                    SmokyMenuRow(Icons.Default.Block, "Block @${peerUsername ?: "user"}", tint = DestructiveRed, onClick = onBlockPeer)
                }
            }
        }
    }
}

@Composable
private fun SmokyMenuRow(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    testTag: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .iosTap(pressedScale = 0.97f, pressedAlpha = 0.72f, onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = tint)
    }
}

private suspend fun saveRemoteMediaToDevice(context: Context, msg: ChatMessage, resolvedFile: File?): Boolean = withContext(Dispatchers.IO) {
    val url = msg.mediaUrl ?: return@withContext false
    val msgType = MessageType.from(msg.type)
    val fileName = msg.mediaName?.takeIf { it.isNotBlank() } ?: when (msgType) {
        MessageType.IMAGE -> "Limon_${System.currentTimeMillis()}.jpg"
        MessageType.VIDEO -> "Limon_${System.currentTimeMillis()}.mp4"
        else -> "Limon_${System.currentTimeMillis()}"
    }
    val mimeType = when (msgType) {
        MessageType.IMAGE -> "image/jpeg"
        MessageType.VIDEO -> "video/mp4"
        else -> "application/octet-stream"
    }
    return@withContext runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = when (msgType) {
                MessageType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                MessageType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
            }
            val relPath = when (msgType) {
                MessageType.IMAGE -> Environment.DIRECTORY_PICTURES + "/LimonChat"
                MessageType.VIDEO -> Environment.DIRECTORY_MOVIES + "/LimonChat"
                else -> Environment.DIRECTORY_DOWNLOADS + "/LimonChat"
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val itemUri = resolver.insert(collection, values) ?: return@runCatching false
            resolver.openOutputStream(itemUri)?.use { out ->
                if (resolvedFile != null && resolvedFile.exists()) {
                    resolvedFile.inputStream().use { input -> input.copyTo(out) }
                } else {
                    URL(url).openStream().use { input -> input.copyTo(out) }
                }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
            true
        } else {
            val outFile = File(context.cacheDir, fileName)
            if (resolvedFile != null && resolvedFile.exists()) {
                resolvedFile.copyTo(outFile, overwrite = true)
            } else {
                URL(url).openStream().use { input ->
                    outFile.outputStream().use { out -> input.copyTo(out) }
                }
            }
            true
        }
    }.getOrDefault(false)
}

private suspend fun shareRemoteMedia(context: Context, msg: ChatMessage, resolvedFile: File?): Boolean {
    val url = msg.mediaUrl ?: return false
    val msgType = MessageType.from(msg.type)
    val fileName = msg.mediaName?.takeIf { it.isNotBlank() } ?: when (msgType) {
        MessageType.IMAGE -> "share_image.jpg"
        MessageType.VIDEO -> "share_video.mp4"
        else -> "share_document"
    }
    val mimeType = when (msgType) {
        MessageType.IMAGE -> "image/*"
        MessageType.VIDEO -> "video/*"
        else -> "*/*"
    }
    val localFile = resolvedFile?.takeIf { it.exists() } ?: withContext(Dispatchers.IO) {
        runCatching {
            val target = File(context.cacheDir, fileName)
            URL(url).openStream().use { input ->
                target.outputStream().use { out -> input.copyTo(out) }
            }
            target
        }.getOrNull()
    } ?: return false

    return runCatching {
        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", localFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
        true
    }.getOrDefault(false)
}

private fun resolveFileName(context: Context, uri: Uri, fallback: String): String {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) {
            return cursor.getString(idx) ?: fallback
        }
    }
    return fallback
}
