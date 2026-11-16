package com.example.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CallRepository
import com.example.data.ChatRepository
import com.example.data.UploadProgressState
import com.example.data.UserRepository
import com.example.media.LimonMediaResolver
import com.example.media.VoicePlayerManager
import com.example.media.VoiceRecorderManager
import com.example.model.CallSession
import com.example.model.ChatMessage
import com.example.model.ChatPreference
import com.example.model.ChatThread
import com.example.model.ChatWallpaperPreset
import com.example.model.MessageType
import com.example.model.PublicProfile
import com.example.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class PendingUpload(
    val uploadId: String,
    val type: MessageType,
    val fileName: String,
    val progress: Float,
    val error: String? = null,
    val uri: Uri? = null,
    val localFile: File? = null,
    val durationMs: Long? = null,
    val waveform: List<Double>? = null,
    val caption: String = "",
    val isViewOnce: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModel(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    val voiceRecorder: VoiceRecorderManager,
    val voicePlayer: VoicePlayerManager,
    val chatId: String,
    val peerUid: String,
    val currentUserId: String,
    private val callRepository: CallRepository? = null
) : ViewModel() {

    private val pageLimit = MutableStateFlow(40L)

    val chatThread: StateFlow<ChatThread?> = chatRepository.observeChat(chatId)
        .catch { e -> Log.w(TAG, "Chat observe error", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val peerProfile: StateFlow<PublicProfile?> = userRepository.observeProfile(peerUid)
        .catch { e -> Log.w(TAG, "Peer observe error", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val myProfile: StateFlow<PublicProfile?> = userRepository.observeMyProfile(currentUserId)
        .catch { e -> Log.w(TAG, "My profile observe error", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val chatPreference: StateFlow<ChatPreference> = chatRepository.observeChatPreferences(currentUserId)
        .map { it[chatId] ?: ChatPreference(chatId = chatId) }
        .catch { emit(ChatPreference(chatId = chatId)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ChatPreference(chatId = chatId))

    val isPeerTyping: StateFlow<Boolean> = chatRepository.observePeerTyping(chatId, currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val isBlockedByMe: StateFlow<Boolean> = userRepository.observeBlockedUsers(currentUserId)
        .map { list -> list.any { it.blockedUid == peerUid } }
        .catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val callLogs: StateFlow<List<CallSession>> = combine(
        callRepository?.observeCallsBetween(currentUserId, peerUid) ?: flowOf(emptyList()),
        chatPreference
    ) { calls, pref ->
        val clearedAtSec = pref.clearedAt?.seconds
        if (clearedAtSec != null) {
            calls.filter { (it.createdAt?.seconds ?: Long.MAX_VALUE) > clearedAtSec }
        } else {
            calls
        }
    }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val messagesState: StateFlow<UiState<List<ChatMessage>>> = pageLimit
        .flatMapLatest { limit ->
            chatRepository.observeMessages(chatId, currentUserId, limit)
        }
        .map<List<ChatMessage>, UiState<List<ChatMessage>>> { messages ->
            val clearedAtSec = chatPreference.value.clearedAt?.seconds
            val visible = if (clearedAtSec != null) {
                messages.filter { (it.createdAt?.seconds ?: Long.MAX_VALUE) > clearedAtSec }
            } else messages
            acknowledgeMessages(visible)
            UiState.Success(visible)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load messages"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _replyingTo = MutableStateFlow<ChatMessage?>(null)
    val replyingTo = _replyingTo.asStateFlow()

    private val _editingMessage = MutableStateFlow<ChatMessage?>(null)
    val editingMessage = _editingMessage.asStateFlow()

    private val _selectedMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMessageIds = _selectedMessageIds.asStateFlow()

    private val _pendingUpload = MutableStateFlow<PendingUpload?>(null)
    val pendingUpload = _pendingUpload.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage = _feedbackMessage.asStateFlow()

    private val _isChatForeground = MutableStateFlow(true)
    val isChatForeground = _isChatForeground.asStateFlow()

    private val _scrollToBottomSignal = MutableStateFlow(0)
    val scrollToBottomSignal = _scrollToBottomSignal.asStateFlow()

    val isVoicePaused: StateFlow<Boolean> = voiceRecorder.isPaused
    val liveVoiceAmplitudes: StateFlow<List<Double>> = voiceRecorder.liveAmplitudes

    private var typingDebounceJob: Job? = null
    private var lastTypingSentMs: Long = 0L

    fun setChatForeground(active: Boolean) {
        _isChatForeground.value = active
        if (active) {
            val current = (messagesState.value as? UiState.Success)?.data
            if (!current.isNullOrEmpty()) {
                acknowledgeMessages(current)
            }
        }
    }

    private fun acknowledgeMessages(messages: List<ChatMessage>) {
        viewModelScope.launch {
            val sendReceipts = myProfile.value?.privacyReadReceipts ?: true
            val markRead = _isChatForeground.value
            chatRepository.acknowledgeDeliveredAndRead(
                chatId = chatId,
                messages = messages,
                markRead = markRead,
                sendReadReceipts = sendReceipts
            )
        }
    }

    fun loadOlderMessages() {
        pageLimit.value = (pageLimit.value + 40L).coerceAtMost(400L)
    }

    fun onInputTextChanged(text: String) {
        val now = System.currentTimeMillis()
        if (text.isNotBlank()) {
            if (now - lastTypingSentMs > 2000L) {
                lastTypingSentMs = now
                viewModelScope.launch { chatRepository.setTyping(chatId, peerUid, true) }
            }
            typingDebounceJob?.cancel()
            typingDebounceJob = viewModelScope.launch {
                delay(4000L)
                chatRepository.setTyping(chatId, peerUid, false)
            }
        } else {
            typingDebounceJob?.cancel()
            viewModelScope.launch { chatRepository.setTyping(chatId, peerUid, false) }
        }
    }

    fun sendTextOrApplyEdit(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val editing = _editingMessage.value
        if (editing != null) {
            viewModelScope.launch {
                chatRepository.editMessage(chatId, editing.id, trimmed)
                    .onSuccess { _editingMessage.value = null }
                    .onFailure { _feedbackMessage.value = it.localizedMessage ?: "Edit failed" }
            }
            return
        }

        val reply = _replyingTo.value
        val replySenderName = if (reply?.senderId == currentUserId) "You" else peerProfile.value?.displayName ?: "Contact"
        _replyingTo.value = null
        typingDebounceJob?.cancel()

        viewModelScope.launch {
            chatRepository.setTyping(chatId, peerUid, false)
            chatRepository.sendMessage(
                chatId = chatId,
                peerUid = peerUid,
                type = MessageType.TEXT,
                text = trimmed,
                replyTo = reply,
                replyToSenderName = replySenderName
            ).onSuccess {
                _scrollToBottomSignal.value += 1
                val myName = myProfile.value?.displayName ?: "Limon User"
                userRepository.sendInAppNotification(
                    recipientUid = peerUid,
                    type = "message",
                    senderName = myName,
                    title = myName,
                    body = trimmed.take(120),
                    chatId = chatId
                )
            }.onFailure { e ->
                _feedbackMessage.value = e.localizedMessage ?: "Message failed (check block/connection)"
            }
        }
    }

    fun sendLocation(lat: Double, lng: Double, locationName: String? = null) {
        viewModelScope.launch {
            val label = locationName?.takeIf { it.isNotBlank() }
                ?: "Location (${String.format("%.4f", lat)}, ${String.format("%.4f", lng)})"
            chatRepository.sendMessage(
                chatId = chatId,
                peerUid = peerUid,
                type = MessageType.LOCATION,
                text = label,
                latitude = lat,
                longitude = lng,
                locationName = locationName
            ).onSuccess {
                _scrollToBottomSignal.value += 1
                val myName = myProfile.value?.displayName ?: "Limon User"
                userRepository.sendInAppNotification(
                    recipientUid = peerUid,
                    type = "message",
                    senderName = myName,
                    title = myName,
                    body = "Shared location: $label".take(120),
                    chatId = chatId
                )
            }.onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun sendContactCard(contact: PublicProfile) {
        viewModelScope.launch {
            chatRepository.sendMessage(
                chatId = chatId,
                peerUid = peerUid,
                type = MessageType.CONTACT,
                text = "${contact.displayName} (@${contact.username})",
                contact = contact
            ).onSuccess {
                _scrollToBottomSignal.value += 1
                val myName = myProfile.value?.displayName ?: "Limon User"
                userRepository.sendInAppNotification(
                    recipientUid = peerUid,
                    type = "message",
                    senderName = myName,
                    title = myName,
                    body = "Shared contact: ${contact.displayName}".take(120),
                    chatId = chatId
                )
            }.onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun uploadAndSendMedia(
        type: MessageType,
        fileName: String,
        uri: Uri? = null,
        localFile: File? = null,
        durationMs: Long? = null,
        waveform: List<Double>? = null,
        caption: String = "",
        isViewOnce: Boolean = false
    ) {
        val uploadId = UUID.randomUUID().toString()
        val folder = when (type) {
            MessageType.IMAGE -> "images"
            MessageType.VIDEO -> "videos"
            MessageType.VOICE -> "voice"
            else -> "files"
        }
        val effectiveViewOnce = isViewOnce && (type == MessageType.IMAGE || type == MessageType.VIDEO)
        _pendingUpload.value = PendingUpload(
            uploadId = uploadId,
            type = type,
            fileName = fileName,
            progress = 0f,
            uri = uri,
            localFile = localFile,
            durationMs = durationMs,
            waveform = waveform,
            caption = caption,
            isViewOnce = effectiveViewOnce
        )

        viewModelScope.launch {
            chatRepository.uploadMediaWithProgress(
                uploadId = uploadId,
                chatId = chatId,
                folder = folder,
                fileName = fileName,
                uri = uri,
                localFile = localFile
            ).collect { state ->
                when (state) {
                    is UploadProgressState.InProgress -> {
                        _pendingUpload.value = _pendingUpload.value?.copy(progress = state.progress, error = null)
                    }
                    is UploadProgressState.Completed -> {
                        _pendingUpload.value = null
                        chatRepository.sendMessage(
                            chatId = chatId,
                            peerUid = peerUid,
                            type = type,
                            text = caption,
                            mediaUrl = state.downloadUrl,
                            mediaName = fileName,
                            mediaSizeBytes = state.sizeBytes,
                            mediaDurationMs = durationMs,
                            waveform = waveform,
                            isViewOnce = effectiveViewOnce
                        ).onSuccess {
                            _scrollToBottomSignal.value += 1
                            val myName = myProfile.value?.displayName ?: "Limon User"
                            val bodyPreview = when (type) {
                                MessageType.IMAGE -> if (effectiveViewOnce) "Sent a view-once photo" else "Sent a photo"
                                MessageType.VIDEO -> if (effectiveViewOnce) "Sent a view-once video" else "Sent a video"
                                MessageType.VOICE -> "Sent a voice message"
                                MessageType.DOCUMENT -> "Sent $fileName"
                                else -> caption.ifBlank { "Sent an attachment" }
                            }
                            userRepository.sendInAppNotification(
                                recipientUid = peerUid,
                                type = "message",
                                senderName = myName,
                                title = myName,
                                body = bodyPreview.take(120),
                                chatId = chatId
                            )
                        }.onFailure { _feedbackMessage.value = it.localizedMessage }
                    }
                    is UploadProgressState.Failed -> {
                        _pendingUpload.value = _pendingUpload.value?.copy(error = state.message)
                    }
                }
            }
        }
    }

    fun retryFailedUpload() {
        val pending = _pendingUpload.value ?: return
        uploadAndSendMedia(
            type = pending.type,
            fileName = pending.fileName,
            uri = pending.uri,
            localFile = pending.localFile,
            durationMs = pending.durationMs,
            waveform = pending.waveform,
            caption = pending.caption,
            isViewOnce = pending.isViewOnce
        )
    }

    fun consumeViewOnceMedia(context: Context, message: ChatMessage) {
        if (!message.isViewOnce || message.senderId == currentUserId || message.viewOnceViewed) return
        LimonMediaResolver.purgeCachedMedia(context, message.mediaUrl)
        viewModelScope.launch {
            chatRepository.markViewOnceViewed(chatId, message.id)
        }
    }

    fun cancelCurrentUpload() {
        _pendingUpload.value?.let { chatRepository.cancelUpload(it.uploadId) }
        _pendingUpload.value = null
    }

    fun startVoiceRecording() {
        if (!voiceRecorder.startRecording(viewModelScope)) {
            _feedbackMessage.value = "Could not start microphone recording"
        }
    }

    fun finishAndSendVoiceRecording() {
        val res = voiceRecorder.stopAndFinish()
        if (res == null) {
            _feedbackMessage.value = "Voice recording too short"
            return
        }
        uploadAndSendMedia(
            type = MessageType.VOICE,
            fileName = res.file.name,
            localFile = res.file,
            durationMs = res.durationMs,
            waveform = res.waveform
        )
    }

    fun pauseVoiceRecording() {
        voiceRecorder.pauseRecording()
    }

    fun resumeVoiceRecording() {
        voiceRecorder.resumeRecording()
    }

    fun cancelVoiceRecording() {
        voiceRecorder.cancelRecording()
    }

    fun setReplyTarget(message: ChatMessage?) {
        _replyingTo.value = message
        if (message != null) _editingMessage.value = null
    }

    fun setEditTarget(message: ChatMessage?) {
        _editingMessage.value = message
        if (message != null) _replyingTo.value = null
    }

    fun toggleReaction(message: ChatMessage, emoji: String) {
        viewModelScope.launch {
            chatRepository.toggleReaction(chatId, message, emoji)
                .onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun pinMessage(message: ChatMessage?) {
        viewModelScope.launch {
            chatRepository.pinMessage(chatId, message)
                .onSuccess { _feedbackMessage.value = if (message != null) "Message pinned" else "Message unpinned" }
        }
    }

    fun deleteMessageForMe(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessageForMe(chatId, messageId)
                .onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun deleteMessageForEveryone(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessageForEveryone(chatId, messageId)
                .onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun deleteCallLog(callId: String) {
        viewModelScope.launch {
            callRepository?.deleteCallLogForMe(callId)
                ?.onSuccess { _feedbackMessage.value = "Call removed for you" }
                ?.onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun setSharedWallpaper(
        preset: ChatWallpaperPreset?,
        customUrl: String?,
        blur: Boolean?
    ) {
        viewModelScope.launch {
            chatRepository.setSharedWallpaper(chatId, preset, customUrl, blur)
                .onSuccess {
                    if (preset != null) {
                        _feedbackMessage.value = "Wallpaper shared with chat partner"
                    }
                }
                .onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun forwardMessageTo(targetPeerUid: String, message: ChatMessage) {
        viewModelScope.launch {
            val targetChatId = chatRepository.getOrCreateChat(targetPeerUid).getOrElse {
                _feedbackMessage.value = it.localizedMessage
                return@launch
            }
            val sourceName = if (message.senderId == currentUserId) {
                myProfile.value?.displayName ?: "You"
            } else {
                peerProfile.value?.displayName ?: "Contact"
            }
            chatRepository.sendMessage(
                chatId = targetChatId,
                peerUid = targetPeerUid,
                type = MessageType.from(message.type),
                text = message.text,
                mediaUrl = message.mediaUrl,
                mediaName = message.mediaName,
                mediaSizeBytes = message.mediaSizeBytes,
                mediaDurationMs = message.mediaDurationMs,
                waveform = message.waveform,
                latitude = message.latitude,
                longitude = message.longitude,
                forwardedFrom = sourceName
            ).onSuccess {
                _feedbackMessage.value = "Message forwarded"
            }.onFailure {
                _feedbackMessage.value = it.localizedMessage
            }
        }
    }

    fun toggleMessageSelection(messageId: String) {
        val current = _selectedMessageIds.value.toMutableSet()
        if (!current.add(messageId)) current.remove(messageId)
        _selectedMessageIds.value = current
    }

    fun clearSelection() {
        _selectedMessageIds.value = emptySet()
    }

    fun deleteSelectedMessagesForMe() {
        val ids = _selectedMessageIds.value.toList()
        _selectedMessageIds.value = emptySet()
        viewModelScope.launch {
            ids.forEach { chatRepository.deleteMessageForMe(chatId, it) }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatRepository.setChatPreference(chatId = chatId, clearHistoryNow = true)
                .onSuccess { _feedbackMessage.value = "Chat history cleared" }
        }
    }

    fun toggleMute() {
        viewModelScope.launch {
            chatRepository.setChatPreference(chatId = chatId, muted = !chatPreference.value.muted)
        }
    }

    fun toggleArchive() {
        viewModelScope.launch {
            chatRepository.setChatPreference(chatId = chatId, archived = !chatPreference.value.archived)
        }
    }

    fun toggleLock() {
        val next = !chatPreference.value.locked
        viewModelScope.launch {
            chatRepository.setChatPreference(chatId = chatId, locked = next)
                .onSuccess {
                    _feedbackMessage.value = if (next) "Chat locked" else "Chat unlocked"
                }
                .onFailure { _feedbackMessage.value = it.localizedMessage }
        }
    }

    fun toggleVoicePlayback(message: ChatMessage) {
        val url = message.mediaUrl ?: return
        voicePlayer.togglePlay(message.id, url, viewModelScope)
    }

    suspend fun resolveMediaFile(context: Context, url: String, fallbackName: String? = null): File? {
        return chatRepository.resolveMediaToLocalFile(context, url, fallbackName)
    }

    fun consumeFeedback() {
        _feedbackMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        voicePlayer.stop()
        voiceRecorder.cancelRecording()
        CoroutineScope(Dispatchers.IO).launch {
            chatRepository.setTyping(chatId, peerUid, false)
        }
    }

    private companion object {
        const val TAG = "ConversationVM"
    }
}
