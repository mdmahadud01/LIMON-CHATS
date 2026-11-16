package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.NetworkMonitor
import com.example.data.AuthRepository
import com.example.data.CallRepository
import com.example.data.ChatRepository
import com.example.data.DeviceIdentity
import com.example.data.GoogleSignInResult
import com.example.data.SettingsDataStore
import com.example.data.UserRepository
import com.example.model.AccentColorOption
import com.example.model.AppSettings
import com.example.model.AppearanceMode
import com.example.model.BlockedUser
import com.example.model.BubbleStyleOption
import com.example.model.CallSession
import com.example.model.ChatListEntry
import com.example.model.ChatPreference
import com.example.model.ChatWallpaperPreset
import com.example.model.GlassStyleOption
import com.example.model.PrivacyScope
import com.example.model.PrivateUserInfo
import com.example.model.PublicProfile
import com.example.model.UiState
import com.example.model.UserNotification
import com.example.notifications.LimonConnectionService
import com.example.notifications.LimonMessagingService
import com.example.notifications.NotificationHelper
import com.example.security.KeystoreCryptoManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class ChatFilterTab(val label: String) {
    ALL("All"),
    UNREAD("Unread"),
    PINNED("Pinned"),
    ARCHIVED("Archived")
}

class MainViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val callRepository: CallRepository,
    private val settingsDataStore: SettingsDataStore,
    private val keystoreCryptoManager: KeystoreCryptoManager,
    val currentUserId: String,
    private val networkMonitor: NetworkMonitor? = null,
    private val appContext: Context? = null
) : ViewModel() {

    val isNetworkOnline: StateFlow<Boolean> = networkMonitor?.isOnline ?: MutableStateFlow(true)

    val settings: StateFlow<AppSettings> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppSettings())

    val myProfile: StateFlow<PublicProfile?> = userRepository.observeMyProfile(currentUserId)
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            Log.w(TAG, "Profile stream error", e)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val privateInfo: StateFlow<PrivateUserInfo?> = userRepository.observePrivateInfo(currentUserId)
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            Log.w(TAG, "Private info stream error", e)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val blockedUsers: StateFlow<List<BlockedUser>> = userRepository.observeBlockedUsers(currentUserId)
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            Log.w(TAG, "Blocked list stream error", e)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<UserNotification>> = userRepository.observeNotifications(currentUserId)
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            Log.w(TAG, "Notifications stream error", e)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val callHistory: StateFlow<UiState<List<CallSession>>> = callRepository.observeCallHistory(currentUserId)
        .map<List<CallSession>, UiState<List<CallSession>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load call history"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val incomingCall: StateFlow<CallSession?> = callRepository.observeIncomingRingingCall(currentUserId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val peerProfilesCache = MutableStateFlow<Map<String, PublicProfile>>(emptyMap())

    val chatsState: StateFlow<UiState<List<ChatListEntry>>> = combine(
        chatRepository.observeChats(currentUserId),
        chatRepository.observeChatPreferences(currentUserId),
        peerProfilesCache
    ) { chats, prefsMap, cache ->
        val entries = chats.map { chat ->
            val peerUid = chat.memberUids.firstOrNull { it != currentUserId } ?: ""
            val pref = prefsMap[chat.id] ?: ChatPreference(chatId = chat.id)
            val unread = (chat.unreadBy[currentUserId] ?: 0L).toInt()
            ChatListEntry(
                chat = chat,
                peerProfile = cache[peerUid],
                peerUid = peerUid,
                preference = pref,
                unreadCount = unread
            )
        }
        entries
    }
        .map<List<ChatListEntry>, UiState<List<ChatListEntry>>> { entries ->
            fetchMissingPeers(entries.map { it.peerUid })
            acknowledgeIncomingChatsDelivered(entries)
            val sorted = entries.sortedWith(
                compareByDescending<ChatListEntry> { it.preference.pinned }
                    .thenByDescending { it.chat.lastMessageAt?.seconds ?: it.chat.updatedAt?.seconds ?: 0L }
            )
            UiState.Success(sorted)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Chats stream error", root)
            emit(UiState.Error(root.message ?: "Failed to load conversations"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<UiState<List<PublicProfile>>>(UiState.Success(emptyList()))
    val searchResults = _searchResults.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner = _statusBanner.asStateFlow()

    private val _cacheSizeBytes = MutableStateFlow(settingsDataStore.calculateCacheSizeBytes())
    val cacheSizeBytes = _cacheSizeBytes.asStateFlow()

    private var searchJob: Job? = null
    private var bootstrapJob: Job? = null
    private val observedPeerJobs = mutableMapOf<String, Job>()
    private val lastAckedChatAt = mutableMapOf<String, Long>()
    private val _activeOpenChatId = MutableStateFlow<String?>(null)
    private var hasSeededNotificationBaseline = false

    init {
        bootstrapSession()
        runSearch("")
        observeRealtimeDeviceNotifications()
    }

    fun setActiveOpenChat(chatId: String?) {
        _activeOpenChatId.value = chatId
        NotificationHelper.setActiveOpenChatId(chatId)
    }

    private fun observeRealtimeDeviceNotifications() {
        val ctx = appContext ?: return
        viewModelScope.launch {
            chatsState.collect { state ->
                val entries = (state as? UiState.Success)?.data ?: return@collect
                if (!hasSeededNotificationBaseline) {
                    hasSeededNotificationBaseline = true
                    entries.forEach { entry ->
                        val ts = entry.chat.lastMessageAt ?: entry.chat.updatedAt
                        if (ts != null) {
                            val tsMs = ts.seconds * 1000L + (ts.nanoseconds / 1_000_000L)
                            NotificationHelper.markChatTimestampNotifiedIfMissing(
                                ctx,
                                currentUserId,
                                entry.chat.id,
                                tsMs
                            )
                        }
                    }
                }
                entries.forEach { entry ->
                    // Suppress notification ONLY if the user is actively viewing this chat in foreground.
                    // If the app is backgrounded or locked with the inbox open, unseen messages STILL notify!
                    if (entry.unreadCount <= 0 ||
                        entry.chat.lastMessageSenderId == currentUserId ||
                        entry.preference.muted ||
                        NotificationHelper.isChatCurrentlyOpen(entry.chat.id)
                    ) {
                        return@forEach
                    }
                    val ts = entry.chat.lastMessageAt ?: entry.chat.updatedAt ?: return@forEach
                    val tsMs = ts.seconds * 1000L + (ts.nanoseconds / 1_000_000L)
                    if (!NotificationHelper.shouldNotifyMessage(ctx, currentUserId, entry.chat.id, tsMs)) {
                        return@forEach
                    }
                    val peer = entry.peerProfile ?: userRepository.getProfileOnce(entry.peerUid)
                    val senderName = peer?.displayName?.ifBlank { peer.username } ?: "Limon Chat"
                    val preview = entry.chat.lastMessageText.ifBlank {
                        when (entry.chat.lastMessageType) {
                            "image" -> "Sent a photo"
                            "video" -> "Sent a video"
                            "voice" -> "Sent a voice message"
                            "document" -> "Sent a document"
                            "location" -> "Shared a location"
                            "contact" -> "Shared a contact"
                            else -> "New message"
                        }
                    }
                    val resolvedAvatarUrl = peer?.photoUrl?.let { raw ->
                        if (raw.startsWith("limonavatar://") || raw.startsWith("rtdb://")) {
                            chatRepository.resolveMediaToLocalFile(ctx, raw)?.toURI()?.toString()
                        } else raw
                    }
                    val avatarBitmap = NotificationHelper.loadCircularAvatarBitmap(resolvedAvatarUrl)
                    NotificationHelper.showMessageNotification(
                        context = ctx,
                        chatId = entry.chat.id,
                        peerUid = entry.peerUid,
                        senderName = senderName,
                        previewText = preview,
                        showPreview = settings.value.notificationPreview,
                        playSound = true,
                        vibrate = true,
                        avatarBitmap = avatarBitmap
                    )
                }
            }
        }
        viewModelScope.launch {
            incomingCall.collectLatest { call ->
                if (call == null || call.receiverId != currentUserId) return@collectLatest
                if (!NotificationHelper.shouldNotifyCall(ctx, call.id)) return@collectLatest
                val resolvedCallerUrl = call.callerPhotoUrl?.let { raw ->
                    if (raw.startsWith("limonavatar://") || raw.startsWith("rtdb://")) {
                        chatRepository.resolveMediaToLocalFile(ctx, raw)?.toURI()?.toString()
                    } else raw
                }
                val callerBitmap = NotificationHelper.loadCircularAvatarBitmap(resolvedCallerUrl)
                NotificationHelper.showIncomingCallNotification(
                    context = ctx,
                    callId = call.id,
                    callerName = call.callerName.ifBlank { "Incoming Call" },
                    callType = call.callType,
                    playSound = true,
                    vibrate = true,
                    callerBitmap = callerBitmap
                )
            }
        }
    }

    private fun bootstrapSession() {
        val user = authRepository.currentUser() ?: return
        bootstrapJob = viewModelScope.launch {
            userRepository.ensureAccountInitialized(user)
            syncFcmTokenInternal()
        }
    }

    fun syncFcmToken() {
        viewModelScope.launch {
            bootstrapJob?.join()
            syncFcmTokenInternal()
        }
    }

    private suspend fun syncFcmTokenInternal() {
        runCatching {
            val ctx = appContext
            val deviceId = ctx?.let { DeviceIdentity.getDeviceId(it) } ?: "primary"
            val liveToken = runCatching { Firebase.messaging.token.await() }.getOrNull()
            val token = liveToken?.takeIf { it.isNotBlank() }
                ?: ctx?.let { LimonMessagingService.getCachedFcmToken(it) }
                ?: ""
            if (token.isNotBlank()) {
                if (ctx != null) LimonMessagingService.cacheFcmToken(ctx, token)
                userRepository.saveFcmToken(token, deviceId)
            }
        }
    }

    private fun acknowledgeIncomingChatsDelivered(entries: List<ChatListEntry>) {
        val toAck = entries.filter { entry ->
            entry.unreadCount > 0 && entry.chat.lastMessageSenderId != currentUserId
        }
        if (toAck.isEmpty()) return
        viewModelScope.launch {
            toAck.forEach { entry ->
                val ts = entry.chat.lastMessageAt?.seconds ?: entry.chat.updatedAt?.seconds ?: 0L
                val prev = lastAckedChatAt[entry.chat.id] ?: -1L
                if (ts > prev) {
                    lastAckedChatAt[entry.chat.id] = ts
                    chatRepository.acknowledgeChatDelivered(entry.chat.id, currentUserId)
                }
            }
        }
    }

    fun refreshChats() {
        val current = chatsState.value
        if (current is UiState.Success) {
            fetchMissingPeers(current.data.map { it.peerUid }, forceRefresh = true)
        }
    }

    private fun fetchMissingPeers(peerUids: List<String>, forceRefresh: Boolean = false) {
        val distinct = peerUids.distinct().filter { it.isNotBlank() }
        distinct.forEach { uid ->
            val existingJob = observedPeerJobs[uid]
            if (forceRefresh || existingJob == null || !existingJob.isActive) {
                existingJob?.cancel()
                observedPeerJobs[uid] = viewModelScope.launch {
                    userRepository.observeProfile(uid)
                        .catch { }
                        .collect { fresh ->
                            if (fresh != null) {
                                peerProfilesCache.value = peerProfilesCache.value + (uid to fresh)
                            }
                        }
                }
            }
        }
    }

    fun setOnlinePresence(online: Boolean) {
        viewModelScope.launch {
            bootstrapJob?.join()
            userRepository.setOnlinePresence(online)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            runSearchInternal(query)
        }
    }

    fun runSearch(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isNotBlank()) settingsDataStore.addRecentSearch(query)
            runSearchInternal(query)
        }
    }

    private suspend fun runSearchInternal(query: String) {
        _searchResults.value = UiState.Loading
        userRepository.searchUsers(query)
            .onSuccess { list ->
                val map = peerProfilesCache.value.toMutableMap()
                list.forEach { map[it.uid] = it }
                peerProfilesCache.value = map
                _searchResults.value = UiState.Success(list)
            }
            .onFailure { e ->
                _searchResults.value = UiState.Error(e.localizedMessage ?: "Search failed")
            }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { settingsDataStore.clearRecentSearches() }
    }

    suspend fun openDirectChat(peerUid: String): Result<String> {
        return chatRepository.getOrCreateChat(peerUid)
    }

    fun toggleChatMuted(chatId: String, current: Boolean) {
        viewModelScope.launch { chatRepository.setChatPreference(chatId = chatId, muted = !current) }
    }

    fun toggleChatArchived(chatId: String, current: Boolean) {
        viewModelScope.launch { chatRepository.setChatPreference(chatId = chatId, archived = !current) }
    }

    fun toggleChatPinned(chatId: String, current: Boolean) {
        viewModelScope.launch { chatRepository.setChatPreference(chatId = chatId, pinned = !current) }
    }

    fun toggleChatLocked(chatId: String, current: Boolean) {
        viewModelScope.launch {
            chatRepository.setChatPreference(chatId = chatId, locked = !current)
                .onSuccess {
                    _statusBanner.value = if (!current) "Chat moved to Locked Chats" else "Chat unlocked"
                }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun markChatAsRead(chatId: String) {
        viewModelScope.launch {
            val sendReceipts = myProfile.value?.privacyReadReceipts ?: true
            chatRepository.markChatAsRead(chatId = chatId, sendReadReceipts = sendReceipts)
            NotificationHelper.clearConversationHistory(chatId)
        }
    }

    fun clearChatHistory(chatId: String) {
        viewModelScope.launch {
            chatRepository.setChatPreference(chatId = chatId, clearHistoryNow = true)
                .onSuccess { _statusBanner.value = "Chat history cleared" }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun deleteConversation(chatId: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(chatId)
                .onSuccess {
                    NotificationHelper.clearConversationHistory(chatId)
                    _statusBanner.value = "Conversation deleted"
                }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun deleteCallLog(callId: String) {
        viewModelScope.launch {
            callRepository.deleteCallLogForMe(callId)
                .onSuccess { _statusBanner.value = "Call removed from list" }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    suspend fun checkUsernameAvailability(username: String): Boolean {
        return userRepository.isUsernameAvailable(username, currentUserId)
    }

    fun updateAvatarImmediate(photoUrl: String?, removePhoto: Boolean = false) {
        viewModelScope.launch {
            userRepository.updatePhotoUrlImmediate(photoUrl = photoUrl, removePhoto = removePhoto)
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun updateProfile(
        displayName: String,
        username: String,
        bio: String,
        photoUrl: String?,
        phoneNumber: String?,
        removePhoto: Boolean = false,
        onDone: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val res = userRepository.updateProfile(
                displayName = displayName,
                newUsername = username,
                bio = bio,
                photoUrl = photoUrl,
                phoneNumber = phoneNumber,
                removePhoto = removePhoto
            )
            _statusBanner.value = if (res.isSuccess) "Profile updated" else res.exceptionOrNull()?.localizedMessage
            onDone(res)
        }
    }

    fun updatePrivacy(
        showLastSeen: PrivacyScope,
        showOnline: PrivacyScope,
        showPhoto: PrivacyScope,
        readReceipts: Boolean
    ) {
        viewModelScope.launch {
            userRepository.updatePrivacySettings(showLastSeen, showOnline, showPhoto, readReceipts)
                .onSuccess { _statusBanner.value = "Privacy settings saved" }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun blockUser(profile: PublicProfile) {
        viewModelScope.launch {
            userRepository.blockUser(profile)
                .onSuccess { _statusBanner.value = "Blocked @${profile.username}" }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun unblockUser(uid: String) {
        viewModelScope.launch {
            userRepository.unblockUser(uid)
                .onSuccess { _statusBanner.value = "User unblocked" }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun reportUser(targetUid: String, reason: String, details: String) {
        viewModelScope.launch {
            userRepository.reportUser(targetUid, reason, details)
                .onSuccess { _statusBanner.value = "Report submitted securely" }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { userRepository.markNotificationRead(id) }
    }

    fun setAppearanceMode(mode: AppearanceMode) = viewModelScope.launch { settingsDataStore.setAppearanceMode(mode) }
    fun setGlassStyle(style: GlassStyleOption) = viewModelScope.launch { settingsDataStore.setGlassStyle(style) }
    fun setAccentColor(accent: AccentColorOption) = viewModelScope.launch { settingsDataStore.setAccentColor(accent) }
    fun setBubbleStyle(style: BubbleStyleOption) = viewModelScope.launch { settingsDataStore.setBubbleStyle(style) }
    fun setChatWallpaper(preset: ChatWallpaperPreset) = viewModelScope.launch { settingsDataStore.setChatWallpaper(preset) }
    fun setCustomWallpaperUri(uri: String?) = viewModelScope.launch { settingsDataStore.setCustomWallpaperUri(uri) }
    fun setWallpaperBlur(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setWallpaperBlur(enabled) }
    fun setFontScale(scale: Float) = viewModelScope.launch { settingsDataStore.setFontScale(scale) }
    fun setAnimationsEnabled(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setAnimationsEnabled(enabled) }
    fun setMessageNotifications(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setMessageNotifications(enabled) }
    fun setCallNotifications(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setCallNotifications(enabled) }
    fun setNotificationSound(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setNotificationSound(enabled) }
    fun setNotificationVibration(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setNotificationVibration(enabled) }
    fun setNotificationPreview(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setNotificationPreview(enabled) }
    fun setAutoDownloadMedia(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setAutoDownloadMedia(enabled) }
    fun setHighQualityMedia(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setHighQualityMedia(enabled) }
    fun setEnterToSend(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setEnterToSend(enabled) }
    fun setLoginAlertsEnabled(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setLoginAlertsEnabled(enabled) }

    fun enableTwoStepPin(pin: String) {
        viewModelScope.launch {
            keystoreCryptoManager.storeTwoStepPin(pin)
            settingsDataStore.setTwoStepEnabled(true)
            _statusBanner.value = "Two-Step PIN secured in Android Keystore"
        }
    }

    fun disableTwoStepPin() {
        viewModelScope.launch {
            keystoreCryptoManager.clearTwoStepPin()
            settingsDataStore.setTwoStepEnabled(false)
            _statusBanner.value = "Two-Step Verification disabled"
        }
    }

    fun verifyTwoStepPin(pin: String): Boolean = keystoreCryptoManager.verifyTwoStepPin(pin)

    fun setChatLockSecretCode(code: String) {
        val cleaned = code.trim()
        if (cleaned.isEmpty()) {
            _statusBanner.value = "Secret code cannot be empty"
            return
        }
        viewModelScope.launch {
            keystoreCryptoManager.storeChatLockSecretCode(cleaned)
            settingsDataStore.setChatLockSecretConfigured(true)
            _statusBanner.value = "Secret code saved for Locked Chats"
        }
    }

    fun verifyChatLockSecretCode(code: String): Boolean =
        keystoreCryptoManager.verifyChatLockSecretCode(code)

    fun removeChatLockSecretCode() {
        viewModelScope.launch {
            keystoreCryptoManager.clearChatLockSecretCode()
            settingsDataStore.setChatLockSecretConfigured(false)
            _statusBanner.value = "Secret code removed"
        }
    }

    fun unlockAllLockedChats() {
        viewModelScope.launch {
            val lockedIds = (chatsState.value as? UiState.Success)?.data
                ?.filter { it.preference.locked }
                ?.map { it.chat.id }
                .orEmpty()
            lockedIds.forEach { chatId ->
                chatRepository.setChatPreference(chatId = chatId, locked = false)
            }
            _statusBanner.value = "All chats unlocked"
        }
    }

    fun clearChatLockAndWipeLockedChats() {
        viewModelScope.launch {
            val lockedIds = (chatsState.value as? UiState.Success)?.data
                ?.filter { it.preference.locked }
                ?.map { it.chat.id }
                .orEmpty()
            chatRepository.clearChatLockAndWipeChats(lockedIds)
                .onSuccess {
                    keystoreCryptoManager.clearChatLockSecretCode()
                    settingsDataStore.setChatLockSecretConfigured(false)
                    _statusBanner.value = "Chat Lock cleared and locked messages removed"
                }
                .onFailure { _statusBanner.value = it.localizedMessage }
        }
    }

    fun refreshCacheSize() {
        _cacheSizeBytes.value = settingsDataStore.calculateCacheSizeBytes()
    }

    fun clearLocalCache() {
        _cacheSizeBytes.value = settingsDataStore.clearLocalCache()
        _statusBanner.value = "Local media cache cleared"
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            NotificationHelper.setActiveOpenChatId(null)
            LimonConnectionService.stop(context)
            userRepository.clearDeviceFcmToken(DeviceIdentity.getDeviceId(context))
            userRepository.setOnlinePresence(false)
            authRepository.signOut(context)
        }
    }

    fun deleteAccount(context: Context, onError: (String) -> Unit) {
        viewModelScope.launch {
            userRepository.deleteAccountData()
                .onSuccess {
                    authRepository.deleteAccount(context)
                        .onFailure { onError(it.localizedMessage ?: "Re-authentication required to delete account") }
                }
                .onFailure { onError(it.localizedMessage ?: "Failed to delete profile data") }
        }
    }

    fun consumeStatusBanner() {
        _statusBanner.value = null
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}

class AuthGateViewModel(
    private val authRepository: AuthRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppSettings())

    val currentUser: StateFlow<FirebaseUser?> = authRepository.authStateFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), authRepository.currentUser())

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn = _isSigningIn.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsDataStore.setOnboardingCompleted(true)
        }
    }

    fun attemptAutoSignIn(context: Context) {
        if (authRepository.currentUser() != null) return
        viewModelScope.launch {
            authRepository.attemptSilentSignIn(context)
        }
    }

    fun signInWithGoogle(context: Context) {
        _isSigningIn.value = true
        _authError.value = null
        viewModelScope.launch {
            when (val result = authRepository.signInWithGoogle(context)) {
                is GoogleSignInResult.Success -> _isSigningIn.value = false
                is GoogleSignInResult.Cancelled -> _isSigningIn.value = false
                is GoogleSignInResult.Error -> {
                    _isSigningIn.value = false
                    _authError.value = result.message
                }
            }
        }
    }

    fun clearError() {
        _authError.value = null
    }
}
