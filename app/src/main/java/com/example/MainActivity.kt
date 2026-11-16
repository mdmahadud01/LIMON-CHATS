package com.example

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.media.LimonMediaResolver
import com.example.media.LimonMediaUploader
import com.example.media.VoicePlayerManager
import com.example.media.VoiceRecorderManager
import com.example.model.AppSettings
import com.example.model.CallType
import com.example.model.UiState
import com.example.notifications.LimonBackgroundSyncWorker
import com.example.notifications.LimonConnectionService
import com.example.notifications.LimonRealtimeSyncService
import com.example.notifications.NotificationHelper
import com.example.security.BiometricLockHelper
import com.example.ui.navigation.Route
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AccountSettingsScreen
import com.example.ui.screens.ActiveCallScreen
import com.example.ui.screens.AppearanceSettingsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BlockedUsersScreen
import com.example.ui.screens.CallsTabScreen
import com.example.ui.screens.CameraCaptureScreen
import com.example.ui.screens.ChatLockOverviewScreen
import com.example.ui.screens.ChatLockSettingsScreen
import com.example.ui.screens.ChatsTabScreen
import com.example.ui.screens.ContactsTabScreen
import com.example.ui.screens.EditProfileScreen
import com.example.ui.screens.ImageViewerScreen
import com.example.ui.screens.LockedChatsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PrivacySettingsScreen
import com.example.ui.screens.PrivateChatScreen
import com.example.ui.screens.SecuritySettingsScreen
import com.example.ui.screens.SettingsTabScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StorageSettingsScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.DestructiveRed
import com.example.ui.theme.FloatingGlassNavBar
import com.example.ui.theme.GlassCard
import com.example.ui.theme.LimonChatTheme
import com.example.ui.theme.LiquidGlassBackground
import com.example.ui.theme.NavTabItem
import com.example.ui.theme.OnlineGreen
import com.example.viewmodel.AuthGateViewModel
import com.example.viewmodel.CallViewModel
import com.example.viewmodel.ConversationViewModel
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private var deepLinkChatId by mutableStateOf<String?>(null)
    private var deepLinkPeerUid by mutableStateOf<String?>(null)
    private var deepLinkCallId by mutableStateOf<String?>(null)
    private var isInPipModeState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeIntentExtras(intent)

        val container = (application as LimonChatApplication).container

        setContent {
            val authGateVm: AuthGateViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { AuthGateViewModel(container.authRepository, container.settingsDataStore) }
                }
            )
            val appSettings by authGateVm.settings.collectAsStateWithLifecycle()
            val firebaseUser by authGateVm.currentUser.collectAsStateWithLifecycle()

            LimonChatTheme(
                appearanceMode = appSettings.appearanceMode,
                glassStyle = appSettings.glassStyle,
                accentOption = appSettings.accentColor,
                fontScale = appSettings.fontScale
            ) {
                var splashFinished by rememberSaveable { mutableStateOf(false) }

                if (!splashFinished) {
                    SplashScreen(onFinished = { splashFinished = true })
                } else if (firebaseUser == null) {
                    UnauthenticatedFlow(
                        authGateVm = authGateVm,
                        settings = appSettings
                    )
                } else {
                    val uid = firebaseUser!!.uid
                    key(uid) {
                        AuthenticatedAppShell(
                            container = container,
                            uid = uid,
                            isInPipMode = isInPipModeState,
                            onEnterPip = { enterVideoCallPip() },
                            initialDeepLinkChatId = deepLinkChatId,
                            initialDeepLinkPeerUid = deepLinkPeerUid,
                            initialDeepLinkCallId = deepLinkCallId,
                            onClearDeepLinks = {
                                deepLinkChatId = null
                                deepLinkPeerUid = null
                                deepLinkCallId = null
                            }
                        )
                    }
                }
            }
        }
    }

    fun enterVideoCallPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(9, 16))
                    .build()
                enterPictureInPictureMode(params)
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState = isInPictureInPictureMode
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeIntentExtras(intent)
    }

    private fun consumeIntentExtras(intent: Intent?) {
        intent ?: return
        deepLinkChatId = intent.getStringExtra(NotificationHelper.EXTRA_CHAT_ID) ?: deepLinkChatId
        deepLinkPeerUid = intent.getStringExtra(NotificationHelper.EXTRA_PEER_UID) ?: deepLinkPeerUid
        deepLinkCallId = intent.getStringExtra(NotificationHelper.EXTRA_CALL_ID) ?: deepLinkCallId
    }
}

@Composable
private fun UnauthenticatedFlow(
    authGateVm: AuthGateViewModel,
    settings: AppSettings
) {
    val context = LocalContext.current
    val isSigningIn by authGateVm.isSigningIn.collectAsStateWithLifecycle()
    val authError by authGateVm.authError.collectAsStateWithLifecycle()

    LaunchedEffect(settings.onboardingCompleted) {
        if (settings.onboardingCompleted) {
            authGateVm.attemptAutoSignIn(context)
        }
    }

    if (!settings.onboardingCompleted) {
        OnboardingScreen(
            onGetStarted = {
                authGateVm.completeOnboarding()
            }
        )
    } else {
        AuthScreen(
            isLoading = isSigningIn,
            errorMessage = authError,
            onGoogleSignIn = { authGateVm.signInWithGoogle(context) },
            onDismissError = { authGateVm.clearError() }
        )
    }
}

@Composable
private fun AuthenticatedAppShell(
    container: AppContainer,
    uid: String,
    isInPipMode: Boolean,
    onEnterPip: () -> Unit,
    initialDeepLinkChatId: String?,
    initialDeepLinkPeerUid: String?,
    initialDeepLinkCallId: String?,
    onClearDeepLinks: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val mainVm: MainViewModel = viewModel(
        key = "main_$uid",
        factory = viewModelFactory {
            initializer {
                MainViewModel(
                    authRepository = container.authRepository,
                    userRepository = container.userRepository,
                    chatRepository = container.chatRepository,
                    callRepository = container.callRepository,
                    settingsDataStore = container.settingsDataStore,
                    keystoreCryptoManager = container.keystoreCryptoManager,
                    currentUserId = uid,
                    networkMonitor = container.networkMonitor,
                    appContext = context.applicationContext
                )
            }
        }
    )

    val callVm: CallViewModel = viewModel(
        key = "call_$uid",
        factory = viewModelFactory {
            initializer {
                CallViewModel(
                    appContext = context.applicationContext,
                    callRepository = container.callRepository,
                    userRepository = container.userRepository,
                    currentUserId = uid
                )
            }
        }
    )

    val isNetworkOnline by mainVm.isNetworkOnline.collectAsStateWithLifecycle()
    val settings by mainVm.settings.collectAsStateWithLifecycle()
    val myProfile by mainVm.myProfile.collectAsStateWithLifecycle()
    val privateInfo by mainVm.privateInfo.collectAsStateWithLifecycle()
    val blockedUsers by mainVm.blockedUsers.collectAsStateWithLifecycle()
    val callHistory by mainVm.callHistory.collectAsStateWithLifecycle()
    val incomingCall by mainVm.incomingCall.collectAsStateWithLifecycle()
    val chatsState by mainVm.chatsState.collectAsStateWithLifecycle()
    val searchQuery by mainVm.searchQuery.collectAsStateWithLifecycle()
    val searchResults by mainVm.searchResults.collectAsStateWithLifecycle()
    val statusBanner by mainVm.statusBanner.collectAsStateWithLifecycle()
    val cacheSizeBytes by mainVm.cacheSizeBytes.collectAsStateWithLifecycle()

    val blockedPeerUids = remember(blockedUsers) {
        blockedUsers.map { it.blockedUid }.toSet()
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(uid) {
        LimonRealtimeSyncService.schedule(context)
        LimonConnectionService.startIfSignedIn(context)
    }

    DisposableEffect(lifecycleOwner, uid) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> LimonConnectionService.startIfSignedIn(context)
                Lifecycle.Event.ON_RESUME -> {
                    NotificationHelper.setAppForeground(true)
                    mainVm.setOnlinePresence(true)
                    mainVm.syncFcmToken()
                }
                Lifecycle.Event.ON_STOP -> {
                    NotificationHelper.setAppForeground(false)
                    mainVm.setOnlinePresence(false)
                    LimonRealtimeSyncService.scheduleImmediateSync(context)
                    LimonBackgroundSyncWorker.enqueueImmediateSync(context)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            NotificationHelper.setAppForeground(false)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(statusBanner) {
        statusBanner?.let {
            snackbarHostState.showSnackbar(it)
            mainVm.consumeStatusBanner()
        }
    }

    LaunchedEffect(initialDeepLinkChatId, initialDeepLinkPeerUid, initialDeepLinkCallId) {
        when {
            !initialDeepLinkCallId.isNullOrBlank() -> {
                navController.navigate(Route.ActiveCall(initialDeepLinkCallId))
                onClearDeepLinks()
            }
            !initialDeepLinkChatId.isNullOrBlank() && !initialDeepLinkPeerUid.isNullOrBlank() -> {
                navController.navigate(Route.Chat(initialDeepLinkChatId, initialDeepLinkPeerUid))
                onClearDeepLinks()
            }
        }
    }

    var selectedHomeTab by rememberSaveable { mutableStateOf("chats") }

    val totalUnreadMessages = remember(chatsState) {
        (chatsState as? UiState.Success)?.data?.sumOf { it.unreadCount } ?: 0
    }
    val lockedChatsCount = remember(chatsState) {
        (chatsState as? UiState.Success)?.data?.count { it.preference.locked } ?: 0
    }
    val isChatLockActive = settings.chatLockSecretConfigured || lockedChatsCount > 0
    val discoveredContacts = remember(searchResults) {
        (searchResults as? UiState.Success)?.data.orEmpty()
    }

    fun requireBiometrics(title: String, subtitle: String, onVerified: () -> Unit) {
        val activity = context as? FragmentActivity
        if (activity != null) {
            BiometricLockHelper.authenticate(
                activity = activity,
                title = title,
                subtitle = subtitle,
                onSuccess = onVerified,
                onError = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
            )
        } else {
            onVerified()
        }
    }

    val startCallWithPeer: (String, CallType) -> Unit = { peerUid, callType ->
        callVm.startOutgoingCall(
            receiverUid = peerUid,
            callType = callType,
            onCallCreated = { callId ->
                navController.navigate(Route.ActiveCall(callId))
            },
            onError = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            }
        )
    }

    val iosEase = remember { CubicBezierEasing(0.22f, 1f, 0.36f, 1f) }
    val tabOrder = remember { listOf("contacts", "calls", "chats", "settings") }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Route.Home,
            enterTransition = {
                slideInHorizontally(animationSpec = tween(340, easing = iosEase)) { fullWidth -> fullWidth } +
                    fadeIn(animationSpec = tween(240, easing = iosEase))
            },
            exitTransition = {
                slideOutHorizontally(animationSpec = tween(340, easing = iosEase)) { fullWidth -> -fullWidth / 3 } +
                    scaleOut(targetScale = 0.96f, animationSpec = tween(320, easing = iosEase)) +
                    fadeOut(animationSpec = tween(220))
            },
            popEnterTransition = {
                slideInHorizontally(animationSpec = tween(340, easing = iosEase)) { fullWidth -> -fullWidth / 3 } +
                    scaleIn(initialScale = 0.96f, animationSpec = tween(320, easing = iosEase)) +
                    fadeIn(animationSpec = tween(240))
            },
            popExitTransition = {
                slideOutHorizontally(animationSpec = tween(340, easing = iosEase)) { fullWidth -> fullWidth } +
                    fadeOut(animationSpec = tween(220))
            }
        ) {
            composable<Route.Home>(
                enterTransition = { fadeIn(tween(260)) },
                exitTransition = {
                    slideOutHorizontally(tween(340, easing = iosEase)) { -it / 3 } +
                        scaleOut(targetScale = 0.96f, animationSpec = tween(320, easing = iosEase)) +
                        fadeOut(tween(220))
                },
                popEnterTransition = {
                    slideInHorizontally(tween(340, easing = iosEase)) { -it / 3 } +
                        scaleIn(initialScale = 0.96f, animationSpec = tween(320, easing = iosEase)) +
                        fadeIn(tween(240))
                }
            ) {
                LiquidGlassBackground(animateOrbs = settings.animationsEnabled) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            FloatingGlassNavBar(
                                items = listOf(
                                    NavTabItem(
                                        id = "contacts",
                                        label = "Contacts",
                                        selectedIcon = Icons.Filled.AccountCircle,
                                        unselectedIcon = Icons.Outlined.AccountCircle
                                    ),
                                    NavTabItem(
                                        id = "calls",
                                        label = "Calls",
                                        selectedIcon = Icons.Filled.Call,
                                        unselectedIcon = Icons.Outlined.Call
                                    ),
                                    NavTabItem(
                                        id = "chats",
                                        label = "Chats",
                                        selectedIcon = Icons.Filled.Forum,
                                        unselectedIcon = Icons.Outlined.Forum,
                                        badgeCount = totalUnreadMessages
                                    ),
                                    NavTabItem(
                                        id = "settings",
                                        label = "Settings",
                                        selectedIcon = Icons.Filled.Settings,
                                        unselectedIcon = Icons.Outlined.Settings
                                    )
                                ),
                                selectedId = selectedHomeTab,
                                onSelect = { selectedHomeTab = it }
                            )
                        }
                    ) { _ ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            AnimatedContent(
                                targetState = selectedHomeTab,
                                transitionSpec = {
                                    val fromIdx = tabOrder.indexOf(initialState).coerceAtLeast(0)
                                    val toIdx = tabOrder.indexOf(targetState).coerceAtLeast(0)
                                    val dir = if (toIdx >= fromIdx) 1 else -1
                                    (slideInHorizontally(tween(300, easing = iosEase)) { dir * it / 4 } +
                                        scaleIn(initialScale = 0.98f, animationSpec = tween(280, easing = iosEase)) +
                                        fadeIn(tween(240))) togetherWith
                                        (slideOutHorizontally(tween(260, easing = iosEase)) { -dir * it / 4 } +
                                            fadeOut(tween(180)))
                                },
                                label = "home_tab_transition"
                            ) { tab ->
                                when (tab) {
                                    "chats" -> ChatsTabScreen(
                                        chatsState = chatsState,
                                        myProfile = myProfile,
                                        blockedPeerUids = blockedPeerUids,
                                        onRefresh = { mainVm.refreshChats() },
                                        onOpenChat = { chatId, peerUid ->
                                            navController.navigate(Route.Chat(chatId, peerUid))
                                        },
                                        onToggleArchive = { cid, cur -> mainVm.toggleChatArchived(cid, cur) },
                                        onTogglePin = { cid, cur -> mainVm.toggleChatPinned(cid, cur) },
                                        onToggleMute = { cid, cur -> mainVm.toggleChatMuted(cid, cur) },
                                        onToggleLock = { cid, cur ->
                                            requireBiometrics(
                                                title = if (cur) "Unlock Chat" else "Lock Chat",
                                                subtitle = "Authenticate to update chat lock"
                                            ) {
                                                mainVm.toggleChatLocked(cid, cur)
                                            }
                                        },
                                        onVerifySecretCode = { code -> mainVm.verifyChatLockSecretCode(code) },
                                        onOpenLockedChats = {
                                            requireBiometrics(
                                                title = "Locked Chats",
                                                subtitle = "Authenticate to view locked conversations"
                                            ) {
                                                navController.navigate(Route.LockedChats)
                                            }
                                        },
                                        onMarkChatRead = { cid -> mainVm.markChatAsRead(cid) },
                                        onClearChatHistory = { cid -> mainVm.clearChatHistory(cid) },
                                        onBlockUser = { profile -> mainVm.blockUser(profile) },
                                        onDeleteChat = { cid -> mainVm.deleteConversation(cid) },
                                        onOpenProfile = { targetUid ->
                                            navController.navigate(Route.UserProfile(targetUid))
                                        },
                                        onOpenSearchContacts = { selectedHomeTab = "contacts" }
                                    )

                                    "contacts" -> ContactsTabScreen(
                                        searchQuery = searchQuery,
                                        searchResults = searchResults,
                                        recentSearches = settings.recentSearches,
                                        blockedPeerUids = blockedPeerUids,
                                        onQueryChange = { mainVm.updateSearchQuery(it) },
                                        onSubmitSearch = { mainVm.runSearch(it) },
                                        onClearHistory = { mainVm.clearSearchHistory() },
                                        onOpenProfile = { profile ->
                                            navController.navigate(Route.UserProfile(profile.uid))
                                        },
                                        onStartChat = { profile ->
                                            scope.launch {
                                                mainVm.openDirectChat(profile.uid)
                                                    .onSuccess { chatId ->
                                                        navController.navigate(Route.Chat(chatId, profile.uid))
                                                    }
                                                    .onFailure {
                                                        snackbarHostState.showSnackbar(it.localizedMessage ?: "Failed to open chat")
                                                    }
                                            }
                                        },
                                        onStartCall = { profile, callType ->
                                            startCallWithPeer(profile.uid, callType)
                                        }
                                    )

                                    "calls" -> CallsTabScreen(
                                        callsState = callHistory,
                                        myUid = uid,
                                        blockedPeerUids = blockedPeerUids,
                                        onCallBack = { peerUid, callType ->
                                            startCallWithPeer(peerUid, callType)
                                        },
                                        onDeleteCallLog = { callId -> mainVm.deleteCallLog(callId) }
                                    )

                                    "settings" -> SettingsTabScreen(
                                        myProfile = myProfile,
                                        privateInfo = privateInfo,
                                        blockedCount = blockedUsers.size,
                                        isChatLockActive = isChatLockActive,
                                        onOpenMyProfile = { navController.navigate(Route.UserProfile(uid)) },
                                        onOpenEditProfile = { navController.navigate(Route.EditProfile) },
                                        onOpenAppearance = { navController.navigate(Route.AppearanceSettings) },
                                        onOpenPrivacy = { navController.navigate(Route.PrivacySettings) },
                                        onOpenChatLock = { navController.navigate(Route.ChatLockOverview) },
                                        onOpenStorage = {
                                            mainVm.refreshCacheSize()
                                            navController.navigate(Route.StorageSettings)
                                        },
                                        onOpenSecurity = { navController.navigate(Route.SecuritySettings) },
                                        onOpenBlocked = { navController.navigate(Route.BlockedUsers) },
                                        onOpenAccount = { navController.navigate(Route.AccountSettings) },
                                        onOpenAbout = { navController.navigate(Route.About) },
                                        onSignOut = { mainVm.signOut(context) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            composable<Route.Chat> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.Chat>()
                DisposableEffect(route.chatId) {
                    mainVm.setActiveOpenChat(route.chatId)
                    onDispose { mainVm.setActiveOpenChat(null) }
                }

                val conversationVm: ConversationViewModel = viewModel(
                    key = "conv_${uid}_${route.chatId}",
                    factory = viewModelFactory {
                        initializer {
                            ConversationViewModel(
                                chatRepository = container.chatRepository,
                                userRepository = container.userRepository,
                                voiceRecorder = VoiceRecorderManager(context.applicationContext),
                                voicePlayer = VoicePlayerManager(
                                    cacheDir = context.applicationContext.cacheDir,
                                    context = context.applicationContext,
                                    firestore = container.firestore
                                ),
                                chatId = route.chatId,
                                peerUid = route.peerUid,
                                currentUserId = uid,
                                callRepository = container.callRepository
                            )
                        }
                    }
                )

                val capturedPhotoPath by backStackEntry.savedStateHandle
                    .getStateFlow<String?>("captured_photo_path", null)
                    .collectAsStateWithLifecycle()

                val currentChatLocked = remember(chatsState, route.chatId) {
                    (chatsState as? UiState.Success)?.data
                        ?.firstOrNull { it.chat.id == route.chatId }
                        ?.preference
                        ?.locked ?: false
                }

                PrivateChatScreen(
                    viewModel = conversationVm,
                    settings = settings,
                    discoveredContacts = discoveredContacts,
                    isNetworkOnline = isNetworkOnline,
                    isChatLocked = currentChatLocked,
                    capturedCameraPath = capturedPhotoPath,
                    onConsumeCapturedCameraPath = {
                        backStackEntry.savedStateHandle.remove<String>("captured_photo_path")
                    },
                    onToggleLock = {
                        requireBiometrics(
                            title = if (currentChatLocked) "Unlock Chat" else "Lock Chat",
                            subtitle = "Authenticate to update chat lock"
                        ) {
                            mainVm.toggleChatLocked(route.chatId, currentChatLocked)
                        }
                    },
                    onSetWallpaperPreset = { mainVm.setChatWallpaper(it) },
                    onSetCustomWallpaperUri = { mainVm.setCustomWallpaperUri(it) },
                    onSetWallpaperBlur = { mainVm.setWallpaperBlur(it) },
                    onBack = { navController.popBackStack() },
                    onOpenProfile = { targetUid ->
                        navController.navigate(Route.UserProfile(targetUid))
                    },
                    onOpenCamera = {
                        navController.navigate(Route.CameraCapture(route.chatId, route.peerUid))
                    },
                    onOpenImage = { url ->
                        navController.navigate(Route.ImageViewer(url))
                    },
                    onOpenVideo = { url ->
                        navController.navigate(Route.VideoPlayer(url))
                    },
                    onOpenViewOnceImage = { url, cid, mid, secure ->
                        navController.navigate(
                            Route.ImageViewer(
                                url = url,
                                title = if (secure) "View Once Photo" else "Photo",
                                chatId = cid,
                                messageId = mid,
                                secureMode = secure
                            )
                        )
                    },
                    onOpenViewOnceVideo = { url, cid, mid, secure ->
                        navController.navigate(
                            Route.VideoPlayer(
                                url = url,
                                title = if (secure) "View Once Video" else "Video",
                                chatId = cid,
                                messageId = mid,
                                secureMode = secure
                            )
                        )
                    },
                    onStartCall = { callType ->
                        startCallWithPeer(route.peerUid, callType)
                    },
                    onBlockPeer = { profile ->
                        mainVm.blockUser(profile)
                    },
                    onUnblockPeer = { targetUid ->
                        mainVm.unblockUser(targetUid)
                    },
                    onDeleteChat = { cid ->
                        mainVm.deleteConversation(cid)
                    }
                )
            }

            composable<Route.UserProfile>(
                enterTransition = {
                    slideInHorizontally(tween(340, easing = iosEase)) { it } +
                        scaleIn(initialScale = 0.96f, animationSpec = tween(320)) +
                        fadeIn(tween(220))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(320, easing = iosEase)) { it } +
                        scaleOut(targetScale = 0.96f, animationSpec = tween(280)) +
                        fadeOut(tween(200))
                }
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<Route.UserProfile>()
                val isBlocked = route.uid in blockedPeerUids
                UserProfileScreen(
                    uid = route.uid,
                    isMe = route.uid == uid,
                    isBlocked = isBlocked,
                    userRepository = container.userRepository,
                    onBack = { navController.popBackStack() },
                    onEditProfile = { navController.navigate(Route.EditProfile) },
                    onStartChat = { profile ->
                        scope.launch {
                            mainVm.openDirectChat(profile.uid)
                                .onSuccess { cid -> navController.navigate(Route.Chat(cid, profile.uid)) }
                        }
                    },
                    onStartCall = { profile, type -> startCallWithPeer(profile.uid, type) },
                    onBlockToggle = { profile, currentlyBlocked ->
                        if (currentlyBlocked) mainVm.unblockUser(profile.uid)
                        else mainVm.blockUser(profile)
                    },
                    onReport = { targetUid, reason, details ->
                        mainVm.reportUser(targetUid, reason, details)
                    }
                )
            }

            composable<Route.EditProfile> {
                EditProfileScreen(
                    myProfile = myProfile,
                    currentPhone = privateInfo?.phoneNumber ?: "",
                    onCheckUsername = { mainVm.checkUsernameAvailability(it) },
                    onUploadAvatar = { uri ->
                        LimonMediaUploader.uploadAvatar(
                            context = context.applicationContext,
                            db = container.firestore,
                            uid = uid,
                            uri = uri
                        )
                    },
                    onRemoveAvatar = {
                        LimonMediaUploader.deleteAvatar(
                            context = context.applicationContext,
                            db = container.firestore,
                            uid = uid
                        )
                    },
                    onPhotoCommitted = { url, remove ->
                        mainVm.updateAvatarImmediate(url, remove)
                    },
                    onSave = { displayName, username, bio, photoUrl, phone, removePhoto, onDone ->
                        mainVm.updateProfile(
                            displayName = displayName,
                            username = username,
                            bio = bio,
                            photoUrl = photoUrl,
                            phoneNumber = phone,
                            removePhoto = removePhoto,
                            onDone = onDone
                        )
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.CameraCapture>(
                enterTransition = { slideInVertically(tween(320, easing = iosEase)) { it } + fadeIn() },
                popExitTransition = { slideOutVertically(tween(300, easing = iosEase)) { it } + fadeOut() }
            ) {
                CameraCaptureScreen(
                    onPhotoCaptured = { file ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("captured_photo_path", file.absolutePath)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.ImageViewer>(
                enterTransition = { scaleIn(initialScale = 0.9f, animationSpec = tween(260)) + fadeIn() },
                popExitTransition = { scaleOut(targetScale = 0.9f, animationSpec = tween(220)) + fadeOut() }
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<Route.ImageViewer>()
                ImageViewerScreen(
                    url = route.url,
                    title = route.title,
                    secureMode = route.secureMode,
                    onConsumed = {
                        LimonMediaResolver.purgeCachedMedia(context, route.url)
                        val cid = route.chatId
                        val mid = route.messageId
                        if (!cid.isNullOrBlank() && !mid.isNullOrBlank()) {
                            scope.launch { container.chatRepository.markViewOnceViewed(cid, mid) }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.VideoPlayer>(
                enterTransition = { scaleIn(initialScale = 0.92f, animationSpec = tween(280)) + fadeIn() },
                popExitTransition = { scaleOut(targetScale = 0.92f, animationSpec = tween(240)) + fadeOut() }
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<Route.VideoPlayer>()
                VideoPlayerScreen(
                    url = route.url,
                    title = route.title,
                    firestore = container.firestore,
                    secureMode = route.secureMode,
                    onConsumed = {
                        LimonMediaResolver.purgeCachedMedia(context, route.url)
                        val cid = route.chatId
                        val mid = route.messageId
                        if (!cid.isNullOrBlank() && !mid.isNullOrBlank()) {
                            scope.launch { container.chatRepository.markViewOnceViewed(cid, mid) }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.ActiveCall>(
                enterTransition = { slideInVertically(tween(340, easing = iosEase)) { it } + fadeIn() },
                popExitTransition = { slideOutVertically(tween(300, easing = iosEase)) { it } + fadeOut() }
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<Route.ActiveCall>()
                ActiveCallScreen(
                    callId = route.callId,
                    viewModel = callVm,
                    isInPipMode = isInPipMode,
                    onEnterPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) onEnterPip else null,
                    onCallFinished = { navController.popBackStack() }
                )
            }

            composable<Route.BlockedUsers> {
                BlockedUsersScreen(
                    blockedUsers = blockedUsers,
                    onUnblock = { mainVm.unblockUser(it) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.PrivacySettings> {
                PrivacySettingsScreen(
                    myProfile = myProfile,
                    onSave = { lastSeen, online, photo, readReceipts ->
                        mainVm.updatePrivacy(lastSeen, online, photo, readReceipts)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.AppearanceSettings> {
                AppearanceSettingsScreen(
                    settings = settings,
                    onSetAppearance = { mainVm.setAppearanceMode(it) },
                    onSetGlassStyle = { mainVm.setGlassStyle(it) },
                    onSetAccent = { mainVm.setAccentColor(it) },
                    onSetBubbleStyle = { mainVm.setBubbleStyle(it) },
                    onSetChatWallpaper = { mainVm.setChatWallpaper(it) },
                    onSetWallpaperBlur = { mainVm.setWallpaperBlur(it) },
                    onSetFontScale = { mainVm.setFontScale(it) },
                    onSetAnimations = { mainVm.setAnimationsEnabled(it) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.LockedChats> {
                LockedChatsScreen(
                    chatsState = chatsState,
                    blockedPeerUids = blockedPeerUids,
                    onOpenChat = { chatId, peerUid ->
                        navController.navigate(Route.Chat(chatId, peerUid))
                    },
                    onUnlockChat = { chatId ->
                        requireBiometrics(
                            title = "Unlock Chat",
                            subtitle = "Authenticate to unlock this conversation"
                        ) {
                            mainVm.toggleChatLocked(chatId, true)
                        }
                    },
                    onUnlockAllChats = {
                        requireBiometrics(
                            title = "Unlock All Chats",
                            subtitle = "Authenticate to unlock all locked chats"
                        ) {
                            mainVm.unlockAllLockedChats()
                        }
                    },
                    onOpenChatLockSettings = {
                        navController.navigate(Route.ChatLockSettings)
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.ChatLockSettings> {
                ChatLockSettingsScreen(
                    settings = settings,
                    onSaveSecretCode = { code -> mainVm.setChatLockSecretCode(code) },
                    onRemoveSecretCode = { mainVm.removeChatLockSecretCode() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.ChatLockOverview> {
                ChatLockOverviewScreen(
                    settings = settings,
                    lockedChatsCount = lockedChatsCount,
                    onOpenLockedChats = {
                        requireBiometrics(
                            title = "Locked Chats",
                            subtitle = "Authenticate to view locked conversations"
                        ) {
                            navController.navigate(Route.LockedChats)
                        }
                    },
                    onOpenChatLockSettings = {
                        navController.navigate(Route.ChatLockSettings)
                    },
                    onClearChatLock = {
                        requireBiometrics(
                            title = "Clear Chat Lock",
                            subtitle = "Authenticate to wipe locked chats and secret code"
                        ) {
                            mainVm.clearChatLockAndWipeLockedChats()
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.StorageSettings> {
                StorageSettingsScreen(
                    settings = settings,
                    cacheSizeBytes = cacheSizeBytes,
                    onToggleAutoDownload = { mainVm.setAutoDownloadMedia(it) },
                    onToggleHighQuality = { mainVm.setHighQualityMedia(it) },
                    onToggleEnterToSend = { mainVm.setEnterToSend(it) },
                    onClearCache = { mainVm.clearLocalCache() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.SecuritySettings> {
                SecuritySettingsScreen(
                    settings = settings,
                    privateInfo = privateInfo,
                    onEnablePin = { mainVm.enableTwoStepPin(it) },
                    onDisablePin = { mainVm.disableTwoStepPin() },
                    onVerifyPin = { mainVm.verifyTwoStepPin(it) },
                    onToggleLoginAlerts = { mainVm.setLoginAlertsEnabled(it) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.AccountSettings> {
                AccountSettingsScreen(
                    uid = uid,
                    privateInfo = privateInfo,
                    onSignOut = { mainVm.signOut(context) },
                    onDeleteAccount = { onError -> mainVm.deleteAccount(context, onError) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.About> {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }

        val ringing = incomingCall
        AnimatedVisibility(
            visible = ringing != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            ringing?.let { session ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevated = true,
                    contentPadding = PaddingValues(13.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.callerName.ifBlank { "Incoming Call" },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (session.callType == CallType.VIDEO.value) "Incoming Video Call" else "Incoming Voice Call",
                                style = MaterialTheme.typography.bodySmall,
                                color = OnlineGreen
                            )
                        }
                        Row {
                            TextButton(onClick = { callVm.declineIncomingCall(session.id) }) {
                                Text("Decline", color = DestructiveRed)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { navController.navigate(Route.ActiveCall(session.id)) },
                                colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen, contentColor = Color.White)
                            ) {
                                Text("Answer", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
