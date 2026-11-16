package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.UserRepository
import com.example.model.CallType
import com.example.model.PrivacyScope
import com.example.model.PublicProfile
import com.example.ui.theme.DestructiveRed
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassConfirmDialog
import com.example.ui.theme.IosGlassIconButton
import com.example.ui.theme.LiquidGlassBackground
import com.example.ui.theme.LocalLiquidGlassColors
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.UserAvatar
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun UserProfileScreen(
    uid: String,
    isMe: Boolean,
    isBlocked: Boolean,
    userRepository: UserRepository,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onStartChat: (PublicProfile) -> Unit,
    onStartCall: (PublicProfile, CallType) -> Unit,
    onBlockToggle: (PublicProfile, Boolean) -> Unit,
    onReport: (uid: String, reason: String, details: String) -> Unit
) {
    val context = LocalContext.current
    val glass = LocalLiquidGlassColors.current
    var profile by remember { mutableStateOf<PublicProfile?>(null) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var copiedNotice by remember { mutableStateOf(false) }
    var heroVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        userRepository.observeProfile(uid).collect {
            profile = it
            if (it != null) heroVisible = true
        }
    }

    val heroScale by animateFloatAsState(
        targetValue = if (heroVisible) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "profile_hero_scale"
    )
    val heroAlpha by animateFloatAsState(
        targetValue = if (heroVisible) 1f else 0f,
        animationSpec = tween(260),
        label = "profile_hero_alpha"
    )

    LiquidGlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = glass.smokyOverlay,
                    border = BorderStroke(1.dp, glass.glassHighlight),
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(10.dp, CircleShape, spotColor = Color(0x2E0F172A))
                ) {
                    IosGlassIconButton(onClick = onBack, modifier = Modifier.fillMaxSize().testTag("profile_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = glass.smokyOverlay,
                    border = BorderStroke(1.dp, glass.glassHighlight),
                    modifier = Modifier.shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0x240F172A))
                ) {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = glass.smokyOverlay,
                    border = BorderStroke(1.dp, glass.glassHighlight),
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(10.dp, CircleShape, spotColor = Color(0x2E0F172A))
                ) {
                    if (isMe) {
                        IosGlassIconButton(onClick = onEditProfile, modifier = Modifier.fillMaxSize().testTag("profile_edit_button")) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                        }
                    } else {
                        IosGlassIconButton(onClick = { showQrDialog = true }, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.QrCode2, contentDescription = "QR Code")
                        }
                    }
                }
            }

            val p = profile
            if (p == null) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = heroScale
                            scaleY = heroScale
                            alpha = heroAlpha
                        },
                    elevated = true,
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val showPhoto = isMe || (!isBlocked && p.privacyShowPhoto != PrivacyScope.NOBODY.value)
                        val showOnline = isMe || (!isBlocked && p.privacyShowOnline != PrivacyScope.NOBODY.value && p.online)
                        UserAvatar(
                            displayName = p.displayName,
                            photoUrl = if (showPhoto) p.photoUrl else null,
                            isOnline = showOnline,
                            size = 84.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(p.displayName, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "@${p.username}",
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.accent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                isBlocked -> "Blocked"
                                showOnline -> "Online now"
                                else -> "Offline"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                isBlocked -> DestructiveRed
                                showOnline -> OnlineGreen
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (p.bio.isNotBlank() && !isBlocked) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = p.bio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!isMe && !isBlocked) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileActionPill(
                            icon = Icons.Default.ChatBubble,
                            label = "Message",
                            onClick = { onStartChat(p) },
                            modifier = Modifier.weight(1f)
                        )
                        ProfileActionPill(
                            icon = Icons.Default.Call,
                            label = "Voice",
                            onClick = { onStartCall(p, CallType.VOICE) },
                            modifier = Modifier.weight(1f)
                        )
                        ProfileActionPill(
                            icon = Icons.Default.Videocam,
                            label = "Video",
                            onClick = { onStartCall(p, CallType.VIDEO) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Limon User ID", p.uid))
                                    copiedNotice = true
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("User ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(p.uid, style = MaterialTheme.typography.bodySmall)
                                if (copiedNotice) {
                                    Text("Copied to clipboard", style = MaterialTheme.typography.labelSmall, color = glass.accent)
                                }
                            }
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy User ID", tint = glass.accent)
                        }

                        val joinedStr = p.createdAt?.toDate()?.let {
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
                        } ?: "Recently"
                        Column {
                            Text("Member Since", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(joinedStr, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showQrDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("QR Profile")
                    }
                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Connect with ${p.displayName} (@${p.username}) on Limon Chat! User ID: ${p.uid}")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Limon Profile"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }

                if (!isMe) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(10.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            TextButton(
                                onClick = { onBlockToggle(p, isBlocked) },
                                modifier = Modifier.fillMaxWidth().testTag("profile_block_toggle")
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = if (isBlocked) glass.accent else DestructiveRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBlocked) "Unblock @${p.username}" else "Block @${p.username}",
                                    color = if (isBlocked) glass.accent else DestructiveRed
                                )
                            }
                            TextButton(
                                onClick = { showReportDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Report, contentDescription = null, tint = DestructiveRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Report User", color = DestructiveRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQrDialog && profile != null) {
        val p = profile!!
        GlassConfirmDialog(
            onDismissRequest = { showQrDialog = false },
            title = "@${p.username} QR Card",
            body = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    DeterministicQrMatrix(payload = "limon://user/${p.uid}/${p.username}", modifier = Modifier.size(168.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(p.displayName, style = MaterialTheme.typography.titleMedium)
                    Text("ID: ${p.uid.take(12)}...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) { Text("Done") }
            }
        )
    }

    if (showReportDialog && profile != null) {
        var reason by remember { mutableStateOf("Spam or unwanted messages") }
        var details by remember { mutableStateOf("") }
        GlassConfirmDialog(
            onDismissRequest = { showReportDialog = false },
            title = "Report @${profile!!.username}",
            body = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Details (optional)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onReport(profile!!.uid, reason, details)
                    showReportDialog = false
                }) { Text("Submit Report", color = DestructiveRed) }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProfileActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlassColors.current
    GlassCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = glass.accent)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun DeterministicQrMatrix(payload: String, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(12.dp)
    ) {
        val grid = 21
        val cell = size.width / grid
        val hashSeed = payload.hashCode()
        for (r in 0 until grid) {
            for (c in 0 until grid) {
                val isFinder = (r < 7 && c < 7) || (r < 7 && c >= grid - 7) || (r >= grid - 7 && c < 7)
                val finderFill = if (isFinder) {
                    val lr = if (r >= grid - 7) r - (grid - 7) else r
                    val lc = if (c >= grid - 7) c - (grid - 7) else c
                    lr == 0 || lr == 6 || lc == 0 || lc == 6 || (lr in 2..4 && lc in 2..4)
                } else {
                    val bit = ((r * 31 + c * 17) xor hashSeed xor payload[(r + c) % payload.length].code)
                    (bit and 1) == 0
                }
                if (finderFill) {
                    drawRect(
                        color = Color(0xFF092E24),
                        topLeft = Offset(c * cell, r * cell),
                        size = Size(cell * 0.92f, cell * 0.92f)
                    )
                }
            }
        }
    }
}

@Composable
fun EditProfileScreen(
    myProfile: PublicProfile?,
    currentPhone: String,
    onCheckUsername: suspend (String) -> Boolean,
    onUploadAvatar: (suspend (Uri) -> Result<String>)? = null,
    onRemoveAvatar: (suspend () -> Unit)? = null,
    onPhotoCommitted: ((photoUrl: String?, removePhoto: Boolean) -> Unit)? = null,
    onSave: (displayName: String, username: String, bio: String, photoUrl: String?, phone: String, removePhoto: Boolean, onDone: (Result<Unit>) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val scope = rememberCoroutineScope()

    val profileKey = myProfile?.uid ?: "initial"
    var displayName by remember(profileKey) { mutableStateOf(myProfile?.displayName ?: "") }
    var username by remember(profileKey) { mutableStateOf(myProfile?.username ?: "") }
    var bio by remember(profileKey) { mutableStateOf(myProfile?.bio ?: "") }
    var phone by remember(profileKey) { mutableStateOf(currentPhone) }
    var photoUrl by remember(profileKey) { mutableStateOf(myProfile?.photoUrl) }
    var removePhotoPending by remember(profileKey) { mutableStateOf(false) }
    var usernameAvailable by remember { mutableStateOf<Boolean?>(true) }
    var checkingUsername by remember { mutableStateOf(false) }
    var uploadingAvatar by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(myProfile?.uid, myProfile?.displayName, myProfile?.username, myProfile?.bio, myProfile?.photoUrl) {
        val p = myProfile ?: return@LaunchedEffect
        if (displayName.isBlank() && p.displayName.isNotBlank()) displayName = p.displayName
        if (username.isBlank() && p.username.isNotBlank()) username = p.username
        if (bio.isBlank() && p.bio.isNotBlank()) bio = p.bio
        if (!uploadingAvatar && !removePhotoPending && photoUrl.isNullOrBlank() && !p.photoUrl.isNullOrBlank()) {
            photoUrl = p.photoUrl
        }
    }
    LaunchedEffect(currentPhone) {
        if (phone.isBlank() && currentPhone.isNotBlank()) {
            phone = currentPhone
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && myProfile != null) {
            scope.launch {
                uploadingAvatar = true
                errorMsg = null
                val res = if (onUploadAvatar != null) {
                    onUploadAvatar(uri)
                } else {
                    runCatching {
                        val ref = Firebase.storage.reference.child("avatars/${myProfile.uid}/avatar.jpg")
                        ref.putFile(uri).await()
                        ref.downloadUrl.await().toString()
                    }
                }
                uploadingAvatar = false
                res.onSuccess { newUrl ->
                    photoUrl = newUrl
                    removePhotoPending = false
                    onPhotoCommitted?.invoke(newUrl, false)
                }.onFailure { errorMsg = it.localizedMessage ?: "Could not upload avatar" }
            }
        }
    }

    LaunchedEffect(username) {
        if (username.isBlank()) {
            usernameAvailable = false
            return@LaunchedEffect
        }
        checkingUsername = true
        delay(350L)
        usernameAvailable = onCheckUsername(username)
        checkingUsername = false
    }

    LiquidGlassBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Edit Profile", style = MaterialTheme.typography.titleLarge)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(contentAlignment = Alignment.Center) {
                        UserAvatar(
                            displayName = displayName.ifBlank { "L" },
                            photoUrl = photoUrl,
                            size = 88.dp
                        )
                        if (uploadingAvatar) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            if (!uploadingAvatar) {
                                avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(glass.accent)
                            .testTag("change_avatar_button")
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Change Photo", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                if (!photoUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        enabled = !uploadingAvatar && !saving,
                        onClick = {
                            scope.launch {
                                uploadingAvatar = true
                                onRemoveAvatar?.invoke()
                                photoUrl = null
                                removePhotoPending = true
                                onPhotoCommitted?.invoke(null, true)
                                uploadingAvatar = false
                            }
                        },
                        modifier = Modifier.testTag("remove_avatar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = DestructiveRed,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove Photo", color = DestructiveRed)
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_display_name")
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' } },
                        label = { Text("Unique Username") },
                        prefix = { Text("@") },
                        trailingIcon = {
                            when {
                                checkingUsername -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                usernameAvailable == true -> Icon(Icons.Default.CheckCircle, "Available", tint = OnlineGreen)
                                else -> Icon(Icons.Default.Error, "Unavailable", tint = DestructiveRed)
                            }
                        },
                        supportingText = {
                            Text(
                                when {
                                    checkingUsername -> "Checking availability..."
                                    usernameAvailable == true -> "Username is available"
                                    else -> "3-30 chars (letters, numbers, _) and must be unique"
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_username")
                    )

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it.take(200) },
                        label = { Text("Bio") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("edit_bio")
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.take(24) },
                        label = { Text("Phone Number (Optional, private)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    errorMsg?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = DestructiveRed)
                    }

                    Button(
                        onClick = {
                            saving = true
                            errorMsg = null
                            onSave(displayName, username, bio, photoUrl, phone, removePhotoPending) { res ->
                                saving = false
                                res.onSuccess { onBack() }
                                    .onFailure { errorMsg = it.localizedMessage ?: "Update failed" }
                            }
                        },
                        enabled = !saving && !uploadingAvatar && usernameAvailable == true && displayName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = glass.accent, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_profile_button")
                    ) {
                        if (saving) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Save Profile", color = Color.White)
                    }
                }
            }
        }
    }
}
