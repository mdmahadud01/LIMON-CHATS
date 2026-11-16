package com.example.ui.screens

import android.text.format.DateFormat
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppSettings
import com.example.model.ChatListEntry
import com.example.model.UiState
import com.example.ui.theme.DestructiveRed
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassConfirmDialog
import com.example.ui.theme.IosGlassIconButton
import com.example.ui.theme.LiquidGlassBackground
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.UserAvatar

@Composable
fun LockedChatsScreen(
    chatsState: UiState<List<ChatListEntry>>,
    blockedPeerUids: Set<String> = emptySet(),
    onOpenChat: (chatId: String, peerUid: String) -> Unit,
    onUnlockChat: (chatId: String) -> Unit,
    onUnlockAllChats: () -> Unit,
    onOpenChatLockSettings: () -> Unit,
    onBack: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    var showOverflowMenu by remember { mutableStateOf(false) }

    val lockedChats = remember(chatsState) {
        (chatsState as? UiState.Success)?.data?.filter { it.preference.locked }.orEmpty()
    }

    LiquidGlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IosGlassIconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("locked_chats_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = glass.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Locked Chats",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IosGlassIconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier.testTag("locked_chats_menu_button")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Locked chats options")
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Chat Lock Settings") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenChatLockSettings()
                            }
                        )
                        if (lockedChats.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Unlock All Chats") },
                                leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onUnlockAllChats()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (lockedChats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = glass.accent,
                                modifier = Modifier.size(40.dp)
                            )
                            Text("No Locked Chats", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Long-press a conversation or open a chat's 3-dot menu and choose Lock Chat to hide it here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(lockedChats, key = { it.chat.id }) { entry ->
                        val peer = entry.peerProfile
                        val isBlocked = entry.peerUid in blockedPeerUids
                        val displayName = peer?.displayName?.ifBlank { "@${peer.username}" } ?: "Limon User"
                        val timeText = entry.chat.lastMessageAt?.toDate()?.let { formatShortTime(it, is24Hour) } ?: ""

                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("locked_chat_row_${entry.chat.id}"),
                            onClick = { onOpenChat(entry.chat.id, entry.peerUid) },
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    displayName = displayName,
                                    photoUrl = if (isBlocked) null else peer?.photoUrl,
                                    isOnline = !isBlocked && peer?.online == true,
                                    size = 44.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isBlocked) "Blocked" else entry.chat.lastMessageText.ifBlank { "Locked conversation" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (timeText.isNotEmpty()) {
                                        Text(
                                            text = timeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(onClick = { onUnlockChat(entry.chat.id) }) {
                                        Text("Unlock", color = glass.accent)
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

@Composable
fun ChatLockSettingsScreen(
    settings: AppSettings,
    onSaveSecretCode: (String) -> Unit,
    onRemoveSecretCode: () -> Unit,
    onBack: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    var secretInput by remember { mutableStateOf("") }
    var feedbackText by remember { mutableStateOf<String?>(null) }

    LiquidGlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IosGlassIconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Chat Lock Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true, contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(glass.accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = glass.accent)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Secret Code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (settings.chatLockSecretConfigured) "Active" else "Not configured",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (settings.chatLockSecretConfigured) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "Enter a secret word, phrase, or emojis (for example: 🍋🔒). Type this exact secret code into the Chats search bar to reveal your Locked Chats.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = secretInput,
                        onValueChange = { secretInput = it },
                        label = { Text(if (settings.chatLockSecretConfigured) "New Secret Code (text or emojis)" else "Create Secret Code (text or emojis)") },
                        placeholder = { Text("e.g., limon27 or 🍋🔐") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = glass.accent,
                            unfocusedBorderColor = glass.glassBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("secret_code_input")
                    )

                    feedbackText?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = glass.accent)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            enabled = secretInput.trim().isNotEmpty(),
                            onClick = {
                                onSaveSecretCode(secretInput.trim())
                                secretInput = ""
                                feedbackText = "Secret code encrypted and saved."
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = glass.accent, contentColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_secret_code_button")
                        ) {
                            Text(if (settings.chatLockSecretConfigured) "Update Code" else "Save Secret Code", color = Color.White)
                        }
                        if (settings.chatLockSecretConfigured) {
                            OutlinedButton(
                                onClick = {
                                    onRemoveSecretCode()
                                    feedbackText = "Secret code cleared."
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DestructiveRed)
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatLockOverviewScreen(
    settings: AppSettings,
    lockedChatsCount: Int,
    onOpenLockedChats: () -> Unit,
    onOpenChatLockSettings: () -> Unit,
    onClearChatLock: () -> Unit,
    onBack: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val isActive = settings.chatLockSecretConfigured || lockedChatsCount > 0
    var showConfirmClear by remember { mutableStateOf(false) }

    LiquidGlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IosGlassIconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Chat Lock",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true, contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = glass.accent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chat Lock Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isActive) OnlineGreen.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isActive) "Active" else "Non Setup",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isActive) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = if (isActive) {
                            "$lockedChatsCount locked chat(s) • Secret code ${if (settings.chatLockSecretConfigured) "configured" else "not set"}."
                        } else {
                            "Lock sensitive conversations with your device fingerprint/face/PIN and an optional secret search code."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onOpenLockedChats,
                            colors = ButtonDefaults.buttonColors(containerColor = glass.accent, contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open Locked Chats", color = Color.White)
                        }
                        OutlinedButton(
                            onClick = onOpenChatLockSettings,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Secret Code")
                        }
                    }
                }
            }

            if (isActive) {
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = DestructiveRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Chat Lock", style = MaterialTheme.typography.titleMedium, color = DestructiveRed)
                        }
                        Text(
                            text = "Unlocks all locked chats, removes your secret code, and clears all messages inside currently locked conversations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showConfirmClear = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed, contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clear_chat_lock_button")
                        ) {
                            Text("Clear Chat Lock & Wipe Locked Chats", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showConfirmClear) {
        GlassConfirmDialog(
            onDismissRequest = { showConfirmClear = false },
            title = "Clear Chat Lock?",
            body = {
                Text(
                    "This removes your secret code, unlocks all locked chats, and wipes all messages in those locked conversations for your account.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmClear = false
                        onClearChatLock()
                    }
                ) {
                    Text("Clear & Wipe", color = DestructiveRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) { Text("Cancel") }
            }
        )
    }
}
