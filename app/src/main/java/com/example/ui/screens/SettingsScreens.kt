package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AccentColorOption
import com.example.model.AppSettings
import com.example.model.AppearanceMode
import com.example.model.BlockedUser
import com.example.model.BubbleStyleOption
import com.example.model.ChatWallpaperPreset
import com.example.model.GlassStyleOption
import com.example.model.PrivacyScope
import com.example.model.PrivateUserInfo
import com.example.model.PublicProfile
import com.example.model.UserNotification
import com.example.ui.theme.CitrusEmerald
import com.example.ui.theme.CoralSunset
import com.example.ui.theme.DestructiveRed
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassConfirmDialog
import com.example.ui.theme.IndigoFrost
import com.example.ui.theme.IosGlassIconButton
import com.example.ui.theme.LiquidGlassBackground
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.OceanCyan
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.UserAvatar
import com.example.ui.theme.iosTap

@Composable
fun SettingsTabScreen(
    myProfile: PublicProfile?,
    privateInfo: PrivateUserInfo?,
    blockedCount: Int,
    isChatLockActive: Boolean = false,
    onOpenMyProfile: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenChatLock: () -> Unit = {},
    onOpenStorage: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenBlocked: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenAbout: () -> Unit,
    onSignOut: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp)
            .padding(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_profile_card"),
            elevated = true,
            onClick = onOpenMyProfile,
            contentPadding = PaddingValues(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    displayName = myProfile?.displayName ?: "Limon User",
                    photoUrl = myProfile?.photoUrl,
                    isOnline = true,
                    size = 56.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = myProfile?.displayName ?: "Setting up profile...",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "@${myProfile?.username ?: "limon"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = glass.accent
                    )
                    if (!privateInfo?.email.isNullOrBlank()) {
                        Text(
                            text = privateInfo!!.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                TextButton(
                    onClick = onOpenEditProfile,
                    modifier = Modifier.testTag("settings_edit_profile_button")
                ) {
                    Text("Edit")
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 2.dp)) {
            Column {
                SettingsRowItem(
                    icon = Icons.Default.Palette,
                    title = "Appearance & Liquid Glass",
                    subtitle = "Themes, accent colors, bubble style & typography",
                    onClick = onOpenAppearance,
                    testTag = "settings_row_appearance"
                )
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 2.dp)) {
            Column {
                SettingsRowItem(
                    icon = Icons.Default.Lock,
                    title = "Privacy Controls",
                    subtitle = "Last seen, online status, photo & read receipts",
                    onClick = onOpenPrivacy,
                    testTag = "settings_row_privacy"
                )
                GlassDivider()
                SettingsRowItem(
                    icon = Icons.Default.Lock,
                    title = "Chat Lock",
                    subtitle = "Hide chats with biometrics & secret code",
                    statusLabel = if (isChatLockActive) "Active" else "Non Setup",
                    statusActive = isChatLockActive,
                    onClick = onOpenChatLock,
                    testTag = "settings_row_chat_lock"
                )
                GlassDivider()
                SettingsRowItem(
                    icon = Icons.Default.Security,
                    title = "Security & Keystore PIN",
                    subtitle = "Hardware-backed Two-Step PIN & active sessions",
                    onClick = onOpenSecurity,
                    testTag = "settings_row_security"
                )
                GlassDivider()
                SettingsRowItem(
                    icon = Icons.Default.Block,
                    title = "Blocked Users",
                    subtitle = "Manage blocked accounts",
                    badgeCount = blockedCount,
                    onClick = onOpenBlocked,
                    testTag = "settings_row_blocked"
                )
                GlassDivider()
                SettingsRowItem(
                    icon = Icons.Default.Storage,
                    title = "Storage & Data",
                    subtitle = "Media auto-download, quality & local cache",
                    onClick = onOpenStorage,
                    testTag = "settings_row_storage"
                )
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 2.dp)) {
            Column {
                SettingsRowItem(
                    icon = Icons.Default.AccountCircle,
                    title = "Account & Identity",
                    subtitle = "Verified Google account & permanent deletion",
                    onClick = onOpenAccount,
                    testTag = "settings_row_account"
                )
                GlassDivider()
                SettingsRowItem(
                    icon = Icons.Default.Info,
                    title = "About Limon Chat",
                    subtitle = "Transport security architecture, terms & licenses",
                    onClick = onOpenAbout,
                    testTag = "settings_row_about"
                )
            }
        }

        OutlinedButton(
            onClick = onSignOut,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DestructiveRed),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("settings_sign_out_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out")
        }
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeCount: Int = 0,
    statusLabel: String? = null,
    statusActive: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    val glass = LocalLiquidGlassColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .iosTap(pressedScale = 0.975f, pressedAlpha = 0.75f, onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(glass.accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = glass.accent, modifier = Modifier.size(19.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!statusLabel.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (statusActive) OnlineGreen.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (statusActive) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (badgeCount > 0) {
            Badge(containerColor = glass.accent, contentColor = Color.White) {
                Text(badgeCount.toString())
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GlassDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 14.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    )
}

@Composable
private fun SettingsSubPageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
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
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IosGlassIconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("subpage_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }
}

@Composable
fun NotificationsFeedScreen(
    notifications: List<UserNotification>,
    onMarkRead: (String) -> Unit,
    onOpenChat: (chatId: String, peerUid: String) -> Unit,
    onBack: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val context = LocalContext.current
    SettingsSubPageScaffold(title = "Notifications", onBack = onBack) {
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GlassCard(contentPadding = PaddingValues(22.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Notifications, null, tint = glass.accent, modifier = Modifier.size(38.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("You're all caught up", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "New messages, missed calls, and security alerts will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(notifications, key = { it.id }) { item ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevated = !item.read,
                        onClick = {
                            onMarkRead(item.id)
                            val cid = item.chatId
                            if (!cid.isNullOrBlank() && item.senderId.isNotBlank()) {
                                onOpenChat(cid, item.senderId)
                            }
                        },
                        contentPadding = PaddingValues(13.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!item.read) {
                               Box(
                                   modifier = Modifier
                                       .size(9.dp)
                                       .clip(CircleShape)
                                       .background(glass.accent)
                               )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(item.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            item.createdAt?.toDate()?.let { date ->
                                Text(formatShortTime(date, context), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BlockedUsersScreen(
    blockedUsers: List<BlockedUser>,
    onUnblock: (String) -> Unit,
    onBack: () -> Unit
) {
    SettingsSubPageScaffold(title = "Blocked Users", onBack = onBack) {
        if (blockedUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GlassCard(contentPadding = PaddingValues(22.dp)) {
                    Text("No blocked accounts.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(blockedUsers, key = { it.blockedUid }) { blocked ->
                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(13.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(displayName = blocked.blockedDisplayName, photoUrl = null, size = 42.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(blocked.blockedDisplayName, style = MaterialTheme.typography.titleMedium)
                                Text("@${blocked.blockedUsername}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(onClick = { onUnblock(blocked.blockedUid) }) {
                                Text("Unblock")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacySettingsScreen(
    myProfile: PublicProfile?,
    onSave: (lastSeen: PrivacyScope, online: PrivacyScope, photo: PrivacyScope, readReceipts: Boolean) -> Unit,
    onBack: () -> Unit
) {
    var lastSeen by remember(myProfile) { mutableStateOf(PrivacyScope.from(myProfile?.privacyShowLastSeen)) }
    var online by remember(myProfile) { mutableStateOf(PrivacyScope.from(myProfile?.privacyShowOnline)) }
    var photo by remember(myProfile) { mutableStateOf(PrivacyScope.from(myProfile?.privacyShowPhoto)) }
    var readReceipts by remember(myProfile) { mutableStateOf(myProfile?.privacyReadReceipts ?: true) }
    val glass = LocalLiquidGlassColors.current

    SettingsSubPageScaffold(title = "Privacy Controls", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrivacyScopeCard("Last Seen Visibility", lastSeen) {
                lastSeen = it
                onSave(lastSeen, online, photo, readReceipts)
            }
            PrivacyScopeCard("Online Status Indicator", online) {
                online = it
                onSave(lastSeen, online, photo, readReceipts)
            }
            PrivacyScopeCard("Profile Photo Visibility", photo) {
                photo = it
                onSave(lastSeen, online, photo, readReceipts)
            }
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Read Receipts", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Send double green checkmarks when you read incoming messages.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = readReceipts,
                        onCheckedChange = {
                            readReceipts = it
                            onSave(lastSeen, online, photo, readReceipts)
                        },
                        modifier = Modifier.testTag("privacy_read_receipts_switch")
                    )
                }
            }
            Text(
                text = "Changes sync directly to your Firestore profile rules.",
                style = MaterialTheme.typography.bodySmall,
                color = glass.accent
            )
        }
    }
}

@Composable
private fun PrivacyScopeCard(
    title: String,
    selected: PrivacyScope,
    onSelect: (PrivacyScope) -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrivacyScope.entries.forEach { scope ->
                    FilterChip(
                        selected = selected == scope,
                        onClick = { onSelect(scope) },
                        label = { Text(scope.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = glass.accent.copy(alpha = 0.22f),
                            selectedLabelColor = glass.accent
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AppearanceSettingsScreen(
    settings: AppSettings,
    onSetAppearance: (AppearanceMode) -> Unit,
    onSetGlassStyle: (GlassStyleOption) -> Unit = {},
    onSetAccent: (AccentColorOption) -> Unit,
    onSetBubbleStyle: (BubbleStyleOption) -> Unit,
    onSetChatWallpaper: (ChatWallpaperPreset) -> Unit = {},
    onSetWallpaperBlur: (Boolean) -> Unit = {},
    onSetFontScale: (Float) -> Unit,
    onSetAnimations: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    var sliderScale by remember(settings.fontScale) { mutableFloatStateOf(settings.fontScale) }

    SettingsSubPageScaffold(title = "Appearance & Liquid Glass", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme Mode", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppearanceMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.appearanceMode == mode,
                                onClick = { onSetAppearance(mode) },
                                label = { Text(mode.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = glass.accent.copy(alpha = 0.22f),
                                    selectedLabelColor = glass.accent
                                )
                            )
                        }
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("UI Glass Style", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassStyleOption.entries.forEach { styleOption ->
                            FilterChip(
                                selected = settings.glassStyle == styleOption,
                                onClick = { onSetGlassStyle(styleOption) },
                                label = { Text(styleOption.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = glass.accent.copy(alpha = 0.22f),
                                    selectedLabelColor = glass.accent
                                ),
                                modifier = Modifier.testTag("glass_style_${styleOption.name.lowercase()}")
                            )
                        }
                    }
                    Text(
                        text = settings.glassStyle.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Accent Color", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AccentColorOption.entries.forEach { option ->
                            val swatch = when (option) {
                                AccentColorOption.CITRUS_EMERALD -> CitrusEmerald
                                AccentColorOption.OCEAN_CYAN -> OceanCyan
                                AccentColorOption.INDIGO_FROST -> IndigoFrost
                                AccentColorOption.CORAL_SUNSET -> CoralSunset
                            }
                            val selected = settings.accentColor == option
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSetAccent(option) }
                                    .padding(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(swatch)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) Icon(Icons.Default.Check, null, tint = Color.White)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(option.label.substringBefore(" "), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Message Bubble Style", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BubbleStyleOption.entries.forEach { style ->
                            FilterChip(
                                selected = settings.bubbleStyle == style,
                                onClick = { onSetBubbleStyle(style) },
                                label = { Text(style.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = glass.accent.copy(alpha = 0.22f),
                                    selectedLabelColor = glass.accent
                                )
                            )
                        }
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Text Size Scale", style = MaterialTheme.typography.titleMedium)
                        Text("${(sliderScale * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = glass.accent)
                    }
                    Slider(
                        value = sliderScale,
                        onValueChange = { sliderScale = it },
                        onValueChangeFinished = { onSetFontScale(sliderScale) },
                        valueRange = 0.85f..1.30f,
                        colors = SliderDefaults.colors(thumbColor = glass.accent, activeTrackColor = glass.accent)
                    )
                    Text(
                        text = "Preview: Liquid Glass messages adapt smoothly to your typography preference.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Default Chat Wallpaper", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChatWallpaperPreset.entries.filter { it != ChatWallpaperPreset.CUSTOM }.forEach { preset ->
                            FilterChip(
                                selected = settings.chatWallpaper == preset,
                                onClick = { onSetChatWallpaper(preset) },
                                label = { Text(preset.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = glass.accent.copy(alpha = 0.22f),
                                    selectedLabelColor = glass.accent
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Wallpaper Blur", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Adds frosted glass softening behind chat bubbles.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = settings.wallpaperBlur, onCheckedChange = onSetWallpaperBlur)
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fluid Spring & Orb Animations", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Disable for battery conservation or reduced motion.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = settings.animationsEnabled, onCheckedChange = onSetAnimations)
                }
            }
        }
    }
}

@Composable
fun NotificationSettingsScreen(
    settings: AppSettings,
    onToggleMessages: (Boolean) -> Unit,
    onToggleCalls: (Boolean) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onTogglePreview: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    SettingsSubPageScaffold(title = "Notification Preferences", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsSwitchRow("Message Notifications", "Push & inline reply notifications for new chats", settings.messageNotifications, onToggleMessages)
                    GlassDivider()
                    SettingsSwitchRow("Call Notifications", "Full-screen & heads-up alerts for incoming WebRTC calls", settings.callNotifications, onToggleCalls)
                    GlassDivider()
                    SettingsSwitchRow("Notification Sound", "Play tone on incoming messages", settings.notificationSound, onToggleSound)
                    GlassDivider()
                    SettingsSwitchRow("Vibration", "Haptic vibration feedback for notifications", settings.notificationVibration, onToggleVibration)
                    GlassDivider()
                    SettingsSwitchRow("Show Message Preview", "Display message text inside lockscreen notifications", settings.notificationPreview, onTogglePreview)
                }
            }
        }
    }
}

@Composable
fun StorageSettingsScreen(
    settings: AppSettings,
    cacheSizeBytes: Long,
    onToggleAutoDownload: (Boolean) -> Unit,
    onToggleHighQuality: (Boolean) -> Unit,
    onToggleEnterToSend: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onBack: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val formattedSize = remember(cacheSizeBytes) {
        when {
            cacheSizeBytes < 1024L -> "$cacheSizeBytes B"
            cacheSizeBytes < 1024L * 1024L -> "${cacheSizeBytes / 1024L} KB"
            else -> String.format("%.2f MB", cacheSizeBytes / (1024.0 * 1024.0))
        }
    }

    SettingsSubPageScaffold(title = "Storage & Data", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsSwitchRow("Auto-Download Media", "Automatically prefetch chat photos & voice notes", settings.autoDownloadMedia, onToggleAutoDownload)
                    GlassDivider()
                    SettingsSwitchRow("High Quality Uploads", "Upload full-resolution photos and videos", settings.highQualityMedia, onToggleHighQuality)
                    GlassDivider()
                    SettingsSwitchRow("Enter Key to Send", "Send text message when pressing keyboard return", settings.enterToSend, onToggleEnterToSend)
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Local Media & Voice Cache", style = MaterialTheme.typography.titleMedium)
                            Text(formattedSize, style = MaterialTheme.typography.bodyMedium, color = glass.accent)
                        }
                        Button(
                            onClick = onClearCache,
                            colors = ButtonDefaults.buttonColors(containerColor = glass.accent, contentColor = Color.White),
                            modifier = Modifier.testTag("clear_cache_button")
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Cache", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecuritySettingsScreen(
    settings: AppSettings,
    privateInfo: PrivateUserInfo?,
    onEnablePin: (String) -> Unit,
    onDisablePin: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onToggleLoginAlerts: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    var showPinDialog by remember { mutableStateOf(false) }
    var verifyResult by remember { mutableStateOf<String?>(null) }

    SettingsSubPageScaffold(title = "Security & Keystore", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true, contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, null, tint = glass.accent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Two-Step Verification PIN", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = if (settings.twoStepEnabled)
                            "Active — Your PIN digest is encrypted with AES-256-GCM inside Android Keystore."
                        else
                            "Protect sensitive account actions with a local hardware-backed Keystore PIN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    verifyResult?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = glass.accent)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showPinDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = glass.accent, contentColor = Color.White),
                            modifier = Modifier.testTag("configure_pin_button")
                        ) {
                            Text(if (settings.twoStepEnabled) "Verify / Update PIN" else "Enable Two-Step PIN", color = Color.White)
                        }
                        if (settings.twoStepEnabled) {
                            OutlinedButton(onClick = onDisablePin) {
                                Text("Disable", color = DestructiveRed)
                            }
                        }
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                SettingsSwitchRow(
                    title = "Login & Security Alerts",
                    subtitle = "Receive system notifications for new device sessions",
                    checked = settings.loginAlertsEnabled,
                    onCheckedChange = onToggleLoginAlerts
                )
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Devices, null, tint = glass.accent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Active Sessions", style = MaterialTheme.typography.titleMedium)
                    }
                    val sessions = privateInfo?.activeSessions.orEmpty()
                    if (sessions.isEmpty()) {
                        Text("Current Android device (Active)", style = MaterialTheme.typography.bodySmall)
                    } else {
                        sessions.forEach { session ->
                            Text("• $session", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        GlassConfirmDialog(
            onDismissRequest = { showPinDialog = false },
            title = if (settings.twoStepEnabled) "Verify or Set New PIN" else "Create 4–8 Digit PIN",
            body = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it.filter { c -> c.isDigit() }.take(8) },
                    label = { Text("Security PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = pinInput.length >= 4,
                    onClick = {
                        if (settings.twoStepEnabled && onVerifyPin(pinInput)) {
                            verifyResult = "PIN verified successfully via Android Keystore."
                        } else {
                            onEnablePin(pinInput)
                            verifyResult = "New Keystore PIN saved."
                        }
                        showPinDialog = false
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AccountSettingsScreen(
    uid: String,
    privateInfo: PrivateUserInfo?,
    onSignOut: () -> Unit,
    onDeleteAccount: (onError: (String) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val glass = LocalLiquidGlassColors.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    SettingsSubPageScaffold(title = "Account & Identity", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, null, tint = OnlineGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Google Identity Connected", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = privateInfo?.email?.ifBlank { "Verified Google Account" } ?: "Verified Google Account",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    GlassDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Limon User ID", uid))
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Account User ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(uid, style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ContentCopy, "Copy ID", tint = glass.accent)
                    }
                }
            }

            deleteError?.let {
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = DestructiveRed)
                }
            }

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Sign Out of This Device")
            }

            Button(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("delete_account_button")
            ) {
                Icon(Icons.Default.DeleteForever, null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Account & Profile", color = Color.White)
            }
        }
    }

    if (showDeleteDialog) {
        GlassConfirmDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = "Delete Limon Chat Account?",
            body = {
                Text(
                    "This removes your @username reservation, public profile, private metadata, and Firebase Auth account permanently.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteAccount { err -> deleteError = err }
                }) { Text("Delete Forever", color = DestructiveRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val glass = LocalLiquidGlassColors.current
    var dialogTitle by remember { mutableStateOf<String?>(null) }
    var dialogBody by remember { mutableStateOf("") }

    SettingsSubPageScaffold(title = "About Limon Chat", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true, contentPadding = PaddingValues(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("LIMON CHAT", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = glass.accent)
                    Text("Version 1.0 • Production Android Build", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Real-time Liquid Glass messenger & WebRTC HD voice/video calling powered by Firebase Firestore, Realtime Database, Cloud Messaging, and Credential Manager.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Transport & Data Security", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "• All Firestore, Realtime Database, and WebRTC signaling traffic is encrypted in transit via TLS/HTTPS & DTLS-SRTP.\n" +
                            "• Firestore security rules enforce strict member-only chat & call access and private email/token isolation.\n" +
                            "• Local Two-Step PINs are sealed with AES-256-GCM in hardware-backed Android Keystore.\n" +
                            "• Messages are stored under cloud security rules (not custom Signal double-ratchet E2EE).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
                Column {
                    AboutActionRow("Privacy Policy") {
                        dialogTitle = "Privacy Policy"
                        dialogBody = "Limon Chat keeps your Google email and phone number inside your owner-only private subcollection. Public search exposes only your display name, @username, bio, and privacy-scoped presence."
                    }
                    GlassDivider()
                    AboutActionRow("Terms of Service") {
                        dialogTitle = "Terms of Service"
                        dialogBody = "Use Limon Chat responsibly. Spam, harassment, and abusive content can be reported in-app and reviewed under platform moderation rules."
                    }
                    GlassDivider()
                    AboutActionRow("Open-Source Licenses") {
                        dialogTitle = "Open-Source Licenses"
                        dialogBody = "Built with Jetpack Compose, Material 3, Firebase Android SDK (Apache 2.0), Stream WebRTC Android (BSD), Coil (Apache 2.0), and Kotlin Coroutines (Apache 2.0)."
                    }
                }
            }
        }
    }

    dialogTitle?.let { title ->
        GlassConfirmDialog(
            onDismissRequest = { dialogTitle = null },
            title = title,
            body = {
                Text(dialogBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            confirmButton = { TextButton(onClick = { dialogTitle = null }) { Text("Close") } }
        )
    }
}

@Composable
private fun AboutActionRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
