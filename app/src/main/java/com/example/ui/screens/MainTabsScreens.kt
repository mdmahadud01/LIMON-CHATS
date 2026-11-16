package com.example.ui.screens

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CallSession
import com.example.model.CallStatus
import com.example.model.CallType
import com.example.model.ChatListEntry
import com.example.model.DeliveryStatus
import com.example.model.PublicProfile
import com.example.model.UiState
import com.example.ui.theme.DestructiveRed
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassConfirmDialog
import com.example.ui.theme.IosGlassIconButton
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.MessageStatusTicks
import com.example.ui.theme.SkeletonChatRow
import com.example.ui.theme.UserAvatar
import com.example.ui.theme.iosTap
import com.example.viewmodel.ChatFilterTab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsTabScreen(
    chatsState: UiState<List<ChatListEntry>>,
    myProfile: PublicProfile?,
    blockedPeerUids: Set<String> = emptySet(),
    onRefresh: () -> Unit,
    onOpenChat: (chatId: String, peerUid: String) -> Unit,
    onToggleArchive: (chatId: String, current: Boolean) -> Unit,
    onTogglePin: (chatId: String, current: Boolean) -> Unit,
    onToggleMute: (chatId: String, current: Boolean) -> Unit,
    onToggleLock: (chatId: String, current: Boolean) -> Unit = { _, _ -> },
    onVerifySecretCode: (String) -> Boolean = { false },
    onOpenLockedChats: () -> Unit = {},
    onMarkChatRead: (chatId: String) -> Unit = {},
    onClearChatHistory: (chatId: String) -> Unit = {},
    onBlockUser: (PublicProfile) -> Unit = {},
    onDeleteChat: (chatId: String) -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    onOpenSearchContacts: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val haptic = LocalHapticFeedback.current
    var selectedFilter by remember { mutableStateOf(ChatFilterTab.ALL) }
    var searchText by remember { mutableStateOf("") }
    var refreshing by remember { mutableStateOf(false) }
    var optionBarEntry by remember { mutableStateOf<ChatListEntry?>(null) }
    var confirmClearChatId by remember { mutableStateOf<String?>(null) }
    var confirmDeleteChatId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Limon Chat",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = myProfile?.let { "@${it.username} • Online" } ?: "Syncing profile...",
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.accent
                )
            }
            IosGlassIconButton(
                onClick = onOpenSearchContacts,
                modifier = Modifier.testTag("new_chat_search_button")
            ) {
                Icon(Icons.Default.PersonSearch, contentDescription = "Find users")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("Filter conversations...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { searchText = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear filter")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = glass.accent,
                unfocusedBorderColor = glass.glassBorder,
                focusedContainerColor = glass.glassSurface,
                unfocusedContainerColor = glass.glassSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("chats_filter_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatFilterTab.entries.forEach { tab ->
                val selected = selectedFilter == tab
                FilterChip(
                    selected = selected,
                    onClick = { selectedFilter = tab },
                    label = { Text(tab.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = glass.accent.copy(alpha = 0.22f),
                        selectedLabelColor = glass.accent
                    ),
                    modifier = Modifier.testTag("chat_filter_${tab.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                onRefresh()
                refreshing = false
            },
            modifier = Modifier.weight(1f)
        ) {
            when (chatsState) {
                is UiState.Loading -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 104.dp)
                    ) {
                        items(5) { SkeletonChatRow() }
                    }
                }

                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(chatsState.message, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = onRefresh) { Text("Retry") }
                            }
                        }
                    }
                }

                is UiState.Success -> {
                    val secretCodeMatched = remember(searchText) {
                        searchText.isNotBlank() && onVerifySecretCode(searchText)
                    }
                    val lockedCount = remember(chatsState.data) {
                        chatsState.data.count { it.preference.locked }
                    }
                    val filtered = remember(chatsState.data, selectedFilter, searchText) {
                        chatsState.data.filter { entry ->
                            if (entry.preference.locked) return@filter false
                            val matchesTab = when (selectedFilter) {
                                ChatFilterTab.ALL -> !entry.preference.archived
                                ChatFilterTab.UNREAD -> !entry.preference.archived && entry.unreadCount > 0
                                ChatFilterTab.PINNED -> !entry.preference.archived && entry.preference.pinned
                                ChatFilterTab.ARCHIVED -> entry.preference.archived
                            }
                            val name = entry.peerProfile?.displayName ?: entry.peerUid
                            val uname = entry.peerProfile?.username ?: ""
                            val matchesQuery = searchText.isBlank() ||
                                name.contains(searchText, ignoreCase = true) ||
                                uname.contains(searchText, ignoreCase = true) ||
                                entry.chat.lastMessageText.contains(searchText, ignoreCase = true)
                            matchesTab && matchesQuery
                        }
                    }

                    if (filtered.isEmpty() && !secretCodeMatched) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = glass.accent,
                                        modifier = Modifier.size(42.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (selectedFilter == ChatFilterTab.ARCHIVED) "No archived chats" else "Start a conversation",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Find people by @username or User ID in Contacts to start messaging.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    TextButton(
                                        onClick = onOpenSearchContacts,
                                        modifier = Modifier.testTag("empty_find_people_button")
                                    ) {
                                        Text("Find People")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 104.dp)
                        ) {
                            if (secretCodeMatched) {
                                item(key = "locked_chats_reveal") {
                                    GlassCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("locked_chats_reveal_card"),
                                        elevated = true,
                                        onClick = {
                                            searchText = ""
                                            onOpenLockedChats()
                                        },
                                        contentPadding = PaddingValues(13.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(glass.accent.copy(alpha = 0.18f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Locked Chats",
                                                    tint = glass.accent
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(11.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Locked Chats",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "$lockedCount hidden chat(s) • Tap to unlock",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = glass.accent
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            items(filtered, key = { it.chat.id }) { entry ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart) {
                                            onToggleArchive(entry.chat.id, entry.preference.archived)
                                            true
                                        } else if (value == SwipeToDismissBoxValue.StartToEnd) {
                                            onTogglePin(entry.chat.id, entry.preference.pinned)
                                            false
                                        } else false
                                    }
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(glass.accent.copy(alpha = 0.25f))
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Archive,
                                                contentDescription = "Archive",
                                                tint = glass.accent
                                            )
                                        }
                                    }
                                ) {
                                    ChatRowCard(
                                        entry = entry,
                                        myUid = myProfile?.uid ?: "",
                                        isPeerBlocked = entry.peerUid in blockedPeerUids,
                                        onClick = { onOpenChat(entry.chat.id, entry.peerUid) },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            optionBarEntry = entry
                                        },
                                        onToggleMute = { onToggleMute(entry.chat.id, entry.preference.muted) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    optionBarEntry?.let { entry ->
        SmokyGlassConversationOptionBar(
            entry = entry,
            isPeerBlocked = entry.peerUid in blockedPeerUids,
            onDismiss = { optionBarEntry = null },
            onOpenChat = {
                optionBarEntry = null
                onOpenChat(entry.chat.id, entry.peerUid)
            },
            onTogglePin = {
                optionBarEntry = null
                onTogglePin(entry.chat.id, entry.preference.pinned)
            },
            onToggleMute = {
                optionBarEntry = null
                onToggleMute(entry.chat.id, entry.preference.muted)
            },
            onMarkRead = {
                optionBarEntry = null
                onMarkChatRead(entry.chat.id)
            },
            onToggleArchive = {
                optionBarEntry = null
                onToggleArchive(entry.chat.id, entry.preference.archived)
            },
            onToggleLock = {
                optionBarEntry = null
                onToggleLock(entry.chat.id, entry.preference.locked)
            },
            onViewProfile = {
                optionBarEntry = null
                if (entry.peerUid.isNotBlank()) onOpenProfile(entry.peerUid)
            },
            onClearHistory = {
                optionBarEntry = null
                confirmClearChatId = entry.chat.id
            },
            onBlockUser = entry.peerProfile?.let { profile ->
                {
                    optionBarEntry = null
                    onBlockUser(profile)
                }
            },
            onDeleteChat = {
                optionBarEntry = null
                confirmDeleteChatId = entry.chat.id
            }
        )
    }

    confirmClearChatId?.let { cid ->
        GlassConfirmDialog(
            onDismissRequest = { confirmClearChatId = null },
            title = "Clear Chat History?",
            body = {
                Text(
                    "This clears all visible messages for your account in this conversation.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onClearChatHistory(cid)
                    confirmClearChatId = null
                }) {
                    Text("Clear", color = DestructiveRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearChatId = null }) { Text("Cancel") }
            }
        )
    }

    confirmDeleteChatId?.let { cid ->
        GlassConfirmDialog(
            onDismissRequest = { confirmDeleteChatId = null },
            title = "Delete Conversation?",
            body = {
                Text(
                    "This permanently removes the conversation thread.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteChat(cid)
                    confirmDeleteChatId = null
                }) {
                    Text("Delete", color = DestructiveRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteChatId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SmokyGlassConversationOptionBar(
    entry: ChatListEntry,
    isPeerBlocked: Boolean = false,
    onDismiss: () -> Unit,
    onOpenChat: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleMute: () -> Unit,
    onMarkRead: () -> Unit,
    onToggleArchive: () -> Unit,
    onToggleLock: () -> Unit,
    onViewProfile: () -> Unit,
    onClearHistory: () -> Unit,
    onBlockUser: (() -> Unit)?,
    onDeleteChat: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val peer = entry.peerProfile
    val displayName = peer?.displayName?.ifBlank { "@${peer.username}" } ?: "Limon User"
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
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = showContent,
                enter = scaleIn(
                    initialScale = 0.88f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                ) + fadeIn(),
                exit = scaleOut(targetScale = 0.92f) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .widthIn(max = 380.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = glass.smokyOverlay,
                        border = BorderStroke(
                            1.dp,
                            Brush.verticalGradient(listOf(glass.glassHighlight, glass.glassBorder))
                        ),
                        shadowElevation = 18.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                displayName = displayName,
                                photoUrl = if (isPeerBlocked) null else peer?.photoUrl,
                                isOnline = !isPeerBlocked && peer?.online == true,
                                size = 44.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isPeerBlocked) "Blocked" else entry.chat.lastMessageText.ifBlank { "@${peer?.username ?: "limon"}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
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
                            .testTag("conversation_smoky_option_bar")
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            SmokyOptionBarItem(
                                icon = Icons.Default.ChatBubbleOutline,
                                label = "Open Conversation",
                                onClick = onOpenChat
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                            SmokyOptionBarItem(
                                icon = Icons.Default.PushPin,
                                label = if (entry.preference.pinned) "Unpin Conversation" else "Pin to Top",
                                onClick = onTogglePin
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                            SmokyOptionBarItem(
                                icon = if (entry.preference.muted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                label = if (entry.preference.muted) "Unmute Alerts" else "Mute Alerts",
                                onClick = onToggleMute
                            )
                            if (entry.unreadCount > 0) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                                SmokyOptionBarItem(
                                    icon = Icons.Default.DoneAll,
                                    label = "Mark as Read",
                                    onClick = onMarkRead
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                            SmokyOptionBarItem(
                                icon = if (entry.preference.archived) Icons.Default.Unarchive else Icons.Default.Archive,
                                label = if (entry.preference.archived) "Unarchive Chat" else "Archive Chat",
                                onClick = onToggleArchive
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                            SmokyOptionBarItem(
                                icon = if (entry.preference.locked) Icons.Default.LockOpen else Icons.Default.Lock,
                                label = if (entry.preference.locked) "Unlock Chat" else "Lock Chat",
                                onClick = onToggleLock
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                            SmokyOptionBarItem(
                                icon = Icons.Default.Person,
                                label = "View Contact Profile",
                                onClick = onViewProfile
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                            SmokyOptionBarItem(
                                icon = Icons.Default.CleaningServices,
                                label = "Clear Chat History",
                                tint = DestructiveRed,
                                onClick = onClearHistory
                            )
                            if (onBlockUser != null && !isPeerBlocked) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                                SmokyOptionBarItem(
                                    icon = Icons.Default.Block,
                                    label = "Block @${peer?.username ?: "user"}",
                                    tint = DestructiveRed,
                                    onClick = onBlockUser
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = glass.glassBorder.copy(alpha = 0.45f))
                            SmokyOptionBarItem(
                                icon = Icons.Default.DeleteForever,
                                label = "Delete Conversation",
                                tint = DestructiveRed,
                                onClick = onDeleteChat
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmokyOptionBarItem(
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
private fun ChatRowCard(
    entry: ChatListEntry,
    myUid: String,
    isPeerBlocked: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleMute: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val peer = entry.peerProfile
    val displayName = peer?.displayName?.ifBlank { "@${peer.username}" } ?: "Limon User"
    val timeText = remember(entry.chat.lastMessageAt, is24Hour) {
        entry.chat.lastMessageAt?.toDate()?.let { formatShortTime(it, is24Hour) } ?: ""
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_row_${entry.chat.id}"),
        onClick = onClick,
        onLongClick = onLongClick,
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                displayName = displayName,
                photoUrl = if (isPeerBlocked) null else peer?.photoUrl,
                isOnline = !isPeerBlocked && peer?.online == true,
                size = 46.dp
            )
            Spacer(modifier = Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (entry.preference.pinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = glass.accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (entry.preference.muted) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "Muted",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(13.dp)
                                .clickable { onToggleMute() }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (entry.chat.lastMessageSenderId == myUid && entry.chat.lastMessageText.isNotBlank()) {
                        MessageStatusTicks(status = DeliveryStatus.SENT)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (entry.unreadCount > 0) glass.accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isPeerBlocked) "Blocked" else entry.chat.lastMessageText.ifBlank { "Tap to start chatting" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isPeerBlocked) DestructiveRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (entry.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(glass.accent)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = entry.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactsTabScreen(
    searchQuery: String,
    searchResults: UiState<List<PublicProfile>>,
    recentSearches: List<String>,
    blockedPeerUids: Set<String> = emptySet(),
    onQueryChange: (String) -> Unit,
    onSubmitSearch: (String) -> Unit,
    onClearHistory: () -> Unit,
    onOpenProfile: (PublicProfile) -> Unit,
    onStartChat: (PublicProfile) -> Unit,
    onStartCall: (PublicProfile, CallType) -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Contacts & Discovery",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Search by @username, display name, or exact User ID",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = { Text("@username, name, or User ID") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = glass.accent,
                unfocusedBorderColor = glass.glassBorder,
                focusedContainerColor = glass.glassSurface,
                unfocusedContainerColor = glass.glassSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("user_search_input")
        )

        AnimatedVisibility(visible = recentSearches.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Searches",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onClearHistory) { Text("Clear") }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentSearches.forEach { term ->
                        AssistChip(
                            onClick = { onSubmitSearch(term) },
                            label = { Text(term) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (searchResults) {
            is UiState.Loading -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(4) { SkeletonChatRow() }
                }
            }

            is UiState.Error -> {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(searchResults.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is UiState.Success -> {
                if (searchResults.data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(22.dp)) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonSearch,
                                    contentDescription = null,
                                    tint = glass.accent,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No matching Limon Chat users", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Try another @username or share your own QR profile for friends to find you.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 104.dp)
                    ) {
                        items(searchResults.data, key = { it.uid }) { user ->
                            val isBlocked = user.uid in blockedPeerUids
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("contact_card_${user.uid}"),
                                onClick = {
                                    onSubmitSearch(searchQuery)
                                    onOpenProfile(user)
                                },
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(
                                        displayName = user.displayName,
                                        photoUrl = if (isBlocked) null else user.photoUrl,
                                        isOnline = !isBlocked && user.online,
                                        size = 44.dp
                                    )
                                    Spacer(modifier = Modifier.width(11.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isBlocked) "@${user.username} • Blocked" else "@${user.username}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isBlocked) DestructiveRed else glass.accent
                                        )
                                        if (user.bio.isNotBlank() && !isBlocked) {
                                            Text(
                                                text = user.bio,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onStartChat(user) },
                                        modifier = Modifier.testTag("message_user_${user.uid}")
                                    ) {
                                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Message ${user.displayName}", tint = glass.accent)
                                    }
                                    IconButton(
                                        onClick = { onStartCall(user, CallType.VOICE) },
                                        modifier = Modifier.testTag("call_user_${user.uid}")
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "Call ${user.displayName}", tint = glass.accentSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class CallFilter(val label: String) {
    ALL("All"),
    MISSED("Missed"),
    INCOMING("Incoming"),
    OUTGOING("Outgoing")
}

@Composable
fun CallsTabScreen(
    callsState: UiState<List<CallSession>>,
    myUid: String,
    blockedPeerUids: Set<String> = emptySet(),
    onCallBack: (peerUid: String, callType: CallType) -> Unit,
    onDeleteCallLog: (String) -> Unit = {}
) {
    val glass = LocalLiquidGlassColors.current
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    var filter by remember { mutableStateOf(CallFilter.ALL) }
    var confirmDeleteCallId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Calls",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CallFilter.entries.forEach { tab ->
                FilterChip(
                    selected = filter == tab,
                    onClick = { filter = tab },
                    label = { Text(tab.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = glass.accent.copy(alpha = 0.22f),
                        selectedLabelColor = glass.accent
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        when (callsState) {
            is UiState.Loading -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(4) { SkeletonChatRow() }
                }
            }

            is UiState.Error -> {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(callsState.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is UiState.Success -> {
                val filtered = remember(callsState.data, filter, myUid) {
                    callsState.data.filter { call ->
                        val isOutgoing = call.callerId == myUid
                        val isMissed = call.status == CallStatus.MISSED.value || call.status == CallStatus.DECLINED.value
                        when (filter) {
                            CallFilter.ALL -> true
                            CallFilter.MISSED -> isMissed
                            CallFilter.INCOMING -> !isOutgoing
                            CallFilter.OUTGOING -> isOutgoing
                        }
                    }
                }
                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = glass.accent, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No call history yet", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Start an encrypted-transport WebRTC voice or video call from any chat or contact.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 104.dp)
                    ) {
                        items(filtered, key = { it.id }) { call ->
                            val isOutgoing = call.callerId == myUid
                            val peerUid = if (isOutgoing) call.receiverId else call.callerId
                            val peerName = if (isOutgoing) call.receiverName else call.callerName
                            val isBlocked = peerUid in blockedPeerUids
                            val peerPhoto = if (isBlocked) null else if (isOutgoing) call.receiverPhotoUrl else call.callerPhotoUrl
                            val isMissed = call.status == CallStatus.MISSED.value || call.status == CallStatus.DECLINED.value
                            val type = CallType.from(call.callType)
                            val dateStr = call.createdAt?.toDate()?.let { formatShortTime(it, is24Hour) } ?: ""

                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                onLongClick = { confirmDeleteCallId = call.id },
                                contentPadding = PaddingValues(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(displayName = peerName, photoUrl = peerPhoto, size = 44.dp)
                                    Spacer(modifier = Modifier.width(11.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = peerName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isMissed) DestructiveRed else MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = when {
                                                    isMissed -> Icons.AutoMirrored.Filled.CallMissed
                                                    isOutgoing -> Icons.AutoMirrored.Filled.CallMade
                                                    else -> Icons.AutoMirrored.Filled.CallReceived
                                                },
                                                contentDescription = null,
                                                tint = if (isMissed) DestructiveRed else glass.accent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            val durLabel = if (call.durationSeconds > 0) " • ${call.durationSeconds}s" else ""
                                            Text(
                                                text = "${call.status.replaceFirstChar { it.uppercase() }}$durLabel • $dateStr",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { confirmDeleteCallId = call.id },
                                        modifier = Modifier.testTag("delete_call_log_${call.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remove from call list",
                                            tint = DestructiveRed
                                        )
                                    }
                                    IconButton(onClick = { onCallBack(peerUid, type) }) {
                                        Icon(
                                            imageVector = if (type == CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                                            contentDescription = "Call back",
                                            tint = glass.accent
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

    confirmDeleteCallId?.let { callId ->
        GlassConfirmDialog(
            onDismissRequest = { confirmDeleteCallId = null },
            title = "Remove Call Log?",
            body = {
                Text(
                    "Remove this call entry from your call list and inbox?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCallLog(callId)
                    confirmDeleteCallId = null
                }) {
                    Text("Remove", color = DestructiveRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteCallId = null }) { Text("Cancel") }
            }
        )
    }
}

fun formatShortTime(date: Date, is24Hour: Boolean): String {
    val pattern = if (is24Hour) "HH:mm" else "h:mm a"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
}

fun formatShortTime(date: Date, context: Context): String =
    formatShortTime(date, DateFormat.is24HourFormat(context))

fun formatShortTime(date: Date): String = formatShortTime(date, is24Hour = true)
