package com.example.viewmodel

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CallRepository
import com.example.data.UserRepository
import com.example.media.CallAudioRoute
import com.example.media.WebRtcEngine
import com.example.model.CallSession
import com.example.model.CallStatus
import com.example.model.CallType
import com.example.notifications.CallForegroundService
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer

class CallViewModel(
    private val appContext: Context,
    private val callRepository: CallRepository,
    private val userRepository: UserRepository,
    val currentUserId: String
) : ViewModel() {

    private var rtcEngine: WebRtcEngine? = null
    private var observedCallId: String? = null
    private var sessionJob: Job? = null
    private var iceJob: Job? = null
    private var connectionStateJob: Job? = null
    private var cameraStateJob: Job? = null
    private var durationJob: Job? = null
    private var ringTimeoutJob: Job? = null
    private var iceRestartJob: Job? = null
    private var audioRouteJob: Job? = null
    private var accessoryJob: Job? = null
    private var activeRingtone: Ringtone? = null

    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val processedIceIds = mutableSetOf<String>()
    private var lastAppliedAnswerSdp: String? = null
    private var lastAppliedOfferSdp: String? = null
    private var offerSent = false
    private var answerSent = false
    private var iceRestartAttempts = 0

    private val _activeCall = MutableStateFlow<CallSession?>(null)
    val activeCall = _activeCall.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Idle")
    val connectionStatus = _connectionStatus.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn = _isSpeakerOn.asStateFlow()

    private val _audioRoute = MutableStateFlow(CallAudioRoute.EARPIECE)
    val audioRoute = _audioRoute.asStateFlow()

    private val _connectedAccessory = MutableStateFlow<CallAudioRoute?>(null)
    val connectedAccessory = _connectedAccessory.asStateFlow()

    private val _isVideoEnabled = MutableStateFlow(true)
    val isVideoEnabled = _isVideoEnabled.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera = _isFrontCamera.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0L)
    val durationSeconds = _durationSeconds.asStateFlow()

    private val _rtcReadyTick = MutableStateFlow(0)
    val rtcReadyTick = _rtcReadyTick.asStateFlow()

    private val appAudioCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            syncDeviceAccessorySnapshot()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            syncDeviceAccessorySnapshot()
        }
    }

    init {
        syncDeviceAccessorySnapshot()
        runCatching { audioManager.registerAudioDeviceCallback(appAudioCallback, mainHandler) }
    }

    private fun syncDeviceAccessorySnapshot() {
        val outputs = runCatching { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) }.getOrNull().orEmpty()
        val hasBt = outputs.any {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == AudioDeviceInfo.TYPE_BLE_HEADSET) ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
        }
        val hasWired = outputs.any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && it.type == AudioDeviceInfo.TYPE_USB_HEADSET) ||
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE
        }
        val detected = when {
            hasBt -> CallAudioRoute.BLUETOOTH
            hasWired -> CallAudioRoute.WIRED_HEADSET
            else -> null
        }
        _connectedAccessory.value = detected
        if (rtcEngine == null && detected != null && !_isSpeakerOn.value) {
            _audioRoute.value = detected
        } else if (rtcEngine == null && detected == null && !_isSpeakerOn.value) {
            _audioRoute.value = CallAudioRoute.EARPIECE
        }
    }

    private fun hasAudioPermission(): Boolean = CallForegroundService.hasRecordAudioPermission(appContext)

    fun attachVideoRenderers(local: SurfaceViewRenderer?, remote: SurfaceViewRenderer?) {
        rtcEngine?.attachRenderers(local, remote)
    }

    fun onPermissionsUpdated(hasAudio: Boolean, hasCamera: Boolean) {
        if (hasCamera) {
            rtcEngine?.ensureCameraVideoStarted()
        }
        val current = _activeCall.value
        if (hasAudio && current != null) {
            maybeStartRtcForSession(current)
        }
    }

    fun startOutgoingCall(
        receiverUid: String,
        callType: CallType,
        onCallCreated: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        resetCallSessionState()
        _activeCall.value = null
        _durationSeconds.value = 0L
        viewModelScope.launch {
            val caller = userRepository.getProfileOnce(currentUserId)
            val receiver = userRepository.getProfileOnce(receiverUid)
            val callerName = caller?.displayName ?: "Limon User"
            val receiverName = receiver?.displayName ?: "Contact"

            val res = callRepository.startCall(
                receiverUid = receiverUid,
                callerName = callerName,
                callerPhotoUrl = caller?.photoUrl,
                receiverName = receiverName,
                receiverPhotoUrl = receiver?.photoUrl,
                callType = callType,
                offerSdp = null
            )

            res.onSuccess { callId ->
                _isVideoEnabled.value = callType == CallType.VIDEO
                _isSpeakerOn.value = callType == CallType.VIDEO
                offerSent = false
                answerSent = false
                iceRestartAttempts = 0
                lastAppliedAnswerSdp = null
                lastAppliedOfferSdp = null
                observeCallSession(callId)
                startRingTimeout(callId)

                if (hasAudioPermission()) {
                    _connectionStatus.value = "Calling..."
                    initRtc(callId, receiverUid, isVideo = callType == CallType.VIDEO, isCaller = true)
                    CallForegroundService.start(appContext, callId, receiverName, callType.value)
                    offerSent = true
                    rtcEngine?.createOffer { offerSdp ->
                        viewModelScope.launch { callRepository.updateOfferSdp(callId, offerSdp) }
                    }
                } else {
                    _connectionStatus.value = "Grant microphone permission to connect"
                }

                userRepository.sendInAppNotification(
                    recipientUid = receiverUid,
                    type = "call",
                    senderName = callerName,
                    title = if (callType == CallType.VIDEO) "Incoming Video Call" else "Incoming Voice Call",
                    body = "$callerName is calling you on Limon Chat"
                )
                onCallCreated(callId)
            }.onFailure { e ->
                onError(e.localizedMessage ?: "Cannot start call (recipient may have blocked calls)")
            }
        }
    }

    fun joinOrAcceptCall(callId: String) {
        NotificationHelper.cancelCallNotification(appContext, callId)
        stopRingtone()
        if (observedCallId != callId) {
            _activeCall.value = null
            _durationSeconds.value = 0L
        }
        observeCallSession(callId)
        val currentSession = _activeCall.value
        if (currentSession != null && currentSession.id == callId) {
            maybeStartRtcForSession(currentSession)
        }
    }

    private fun startRingTimeout(callId: String) {
        ringTimeoutJob?.cancel()
        ringTimeoutJob = viewModelScope.launch {
            delay(RING_TIMEOUT_MS)
            val current = _activeCall.value
            if (current?.id == callId && current.status == CallStatus.RINGING.value) {
                callRepository.endCall(callId, CallStatus.MISSED, 0L)
            }
        }
    }

    private fun observeCallSession(callId: String) {
        if (observedCallId == callId && sessionJob?.isActive == true) return
        observedCallId = callId
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            callRepository.observeCallSession(callId).collect { session ->
                _activeCall.value = session
                if (session == null) return@collect

                val isTerminal = session.status in listOf(
                    CallStatus.ENDED.value,
                    CallStatus.DECLINED.value,
                    CallStatus.MISSED.value
                )
                if (isTerminal) {
                    NotificationHelper.cancelCallNotification(appContext, callId)
                    stopRingtone()
                    cleanupRtc()
                    return@collect
                }

                if (session.status != CallStatus.RINGING.value) {
                    ringTimeoutJob?.cancel()
                    stopRingtone()
                } else {
                    if (ringTimeoutJob?.isActive != true) {
                        startRingTimeout(callId)
                    }
                    if (session.receiverId == currentUserId && rtcEngine == null) {
                        startRingtone()
                    }
                }

                maybeStartRtcForSession(session)

                val isCaller = session.callerId == currentUserId
                if (isCaller && session.status == CallStatus.ACCEPTED.value) {
                    session.answerSdp?.takeIf { it.contains("m=") && it != lastAppliedAnswerSdp }?.let { sdp ->
                        lastAppliedAnswerSdp = sdp
                        rtcEngine?.applyRemoteAnswer(sdp)
                    }
                } else if (!isCaller && session.status == CallStatus.ACCEPTED.value && rtcEngine != null) {
                    session.offerSdp?.takeIf { it.contains("m=") && it != lastAppliedOfferSdp }?.let { newOffer ->
                        lastAppliedOfferSdp = newOffer
                        rtcEngine?.createAnswer(newOffer) { newAnswer ->
                            viewModelScope.launch { callRepository.acceptCall(callId, newAnswer) }
                        }
                    }
                }

                if (session.status == CallStatus.ACCEPTED.value && durationJob == null) {
                    startDurationTimer()
                }
            }
        }
    }

    private fun maybeStartRtcForSession(session: CallSession) {
        if (!hasAudioPermission()) {
            _connectionStatus.value = "Grant microphone permission to connect"
            return
        }
        val callId = session.id
        val isCaller = session.callerId == currentUserId
        val peerUid = if (isCaller) session.receiverId else session.callerId
        val isVideo = session.callType == CallType.VIDEO.value
        val peerName = if (isCaller) session.receiverName else session.callerName

        if (rtcEngine == null) {
            _isVideoEnabled.value = isVideo
            _isSpeakerOn.value = isVideo
            if (isCaller) {
                _connectionStatus.value = "Calling..."
                initRtc(callId, peerUid, isVideo = isVideo, isCaller = true)
                CallForegroundService.start(appContext, callId, peerName, session.callType)
                if (!offerSent && session.offerSdp.isNullOrBlank()) {
                    offerSent = true
                    rtcEngine?.createOffer { offerSdp ->
                        viewModelScope.launch { callRepository.updateOfferSdp(callId, offerSdp) }
                    }
                }
            } else if ((session.status == CallStatus.RINGING.value || session.status == CallStatus.ACCEPTED.value) && !answerSent) {
                NotificationHelper.cancelCallNotification(appContext, callId)
                stopRingtone()
                initRtc(callId, peerUid, isVideo = isVideo, isCaller = false)
                CallForegroundService.start(appContext, callId, peerName, session.callType)
                answerSent = true
                val offer = session.offerSdp
                if (!offer.isNullOrBlank() && offer.contains("m=")) {
                    lastAppliedOfferSdp = offer
                    rtcEngine?.createAnswer(offer) { answerSdp ->
                        viewModelScope.launch { callRepository.acceptCall(callId, answerSdp) }
                    }
                } else {
                    _connectionStatus.value = "Connecting..."
                    viewModelScope.launch { callRepository.markCallAcceptedPendingSdp(callId) }
                }
            }
        }
    }

    private fun initRtc(
        callId: String,
        peerUid: String,
        isVideo: Boolean,
        isCaller: Boolean
    ) {
        cleanupRtc(stopForegroundService = false)
        lastAppliedAnswerSdp = null
        processedIceIds.clear()
        val engine = WebRtcEngine(appContext)
        rtcEngine = engine
        engine.initializeSession(
            isVideo = isVideo,
            onIceRestartNeeded = {
                if (isCaller) scheduleIceRestart(callId)
            }
        ) { candidate ->
            viewModelScope.launch {
                callRepository.sendIceCandidate(
                    callId = callId,
                    peerUid = peerUid,
                    sdpMid = candidate.sdpMid ?: "0",
                    sdpMLineIndex = candidate.sdpMLineIndex,
                    candidate = candidate.sdp
                )
            }
        }
        _rtcReadyTick.value += 1

        connectionStateJob = viewModelScope.launch {
            engine.connectionState.collect { status ->
                _connectionStatus.value = status
            }
        }

        cameraStateJob = viewModelScope.launch {
            engine.isFrontCamera.collect { isFront ->
                _isFrontCamera.value = isFront
            }
        }

        audioRouteJob = viewModelScope.launch {
            engine.audioRoute.collect { route ->
                _audioRoute.value = route
                _isSpeakerOn.value = route == CallAudioRoute.SPEAKER
            }
        }

        accessoryJob = viewModelScope.launch {
            engine.connectedAccessory.collect { acc ->
                if (acc != null) {
                    _connectedAccessory.value = acc
                } else {
                    syncDeviceAccessorySnapshot()
                }
            }
        }

        iceJob = viewModelScope.launch {
            callRepository.observeRemoteIceCandidates(callId, currentUserId).collect { list ->
                list.forEach { rec ->
                    if (processedIceIds.add(rec.id)) {
                        engine.addRemoteIceCandidate(rec.sdpMid, rec.sdpMLineIndex, rec.candidate)
                    }
                }
            }
        }
    }

    private fun scheduleIceRestart(callId: String) {
        if (iceRestartAttempts >= MAX_ICE_RESTARTS || iceRestartJob?.isActive == true) return
        iceRestartJob = viewModelScope.launch {
            delay(2000L)
            val current = _activeCall.value
            val engine = rtcEngine
            if (current?.id == callId && current.status == CallStatus.ACCEPTED.value && engine != null) {
                iceRestartAttempts += 1
                engine.createOffer(iceRestart = true) { newOffer ->
                    viewModelScope.launch {
                        callRepository.updateOfferSdp(callId, newOffer)
                    }
                }
            }
        }
    }

    private fun startDurationTimer() {
        if (durationJob != null) return
        _durationSeconds.value = 0L
        durationJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                _durationSeconds.value += 1L
            }
        }
    }

    private fun startRingtone() {
        if (activeRingtone?.isPlaying == true) return
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val ringtone = RingtoneManager.getRingtone(appContext, uri)
            ringtone?.play()
            activeRingtone = ringtone
        }
    }

    private fun stopRingtone() {
        runCatching { activeRingtone?.stop() }
        activeRingtone = null
    }

    fun toggleMute() {
        val next = !_isMuted.value
        _isMuted.value = next
        rtcEngine?.setMicMuted(next)
    }

    fun toggleSpeaker() {
        val acc = _connectedAccessory.value
        val nextRoute = if (acc != null) {
            when (_audioRoute.value) {
                acc -> CallAudioRoute.SPEAKER
                CallAudioRoute.SPEAKER -> acc
                else -> acc
            }
        } else {
            if (_audioRoute.value == CallAudioRoute.SPEAKER) {
                CallAudioRoute.EARPIECE
            } else {
                CallAudioRoute.SPEAKER
            }
        }
        _audioRoute.value = nextRoute
        _isSpeakerOn.value = nextRoute == CallAudioRoute.SPEAKER
        rtcEngine?.selectAudioRoute(nextRoute)
    }

    fun toggleVideo() {
        val next = !_isVideoEnabled.value
        _isVideoEnabled.value = next
        rtcEngine?.setVideoEnabled(next)
    }

    fun switchCamera() {
        rtcEngine?.switchCamera()
    }

    fun declineIncomingCall(callId: String) {
        NotificationHelper.cancelCallNotification(appContext, callId)
        stopRingtone()
        viewModelScope.launch {
            callRepository.endCall(callId, CallStatus.DECLINED, 0L)
            resetCallSessionState()
        }
    }

    fun endActiveCall(callId: String) {
        NotificationHelper.cancelCallNotification(appContext, callId)
        stopRingtone()
        val dur = _durationSeconds.value
        val wasAccepted = _activeCall.value?.status == CallStatus.ACCEPTED.value
        val finalStatus = if (wasAccepted) CallStatus.ENDED else CallStatus.MISSED
        viewModelScope.launch {
            callRepository.endCall(callId, finalStatus, dur)
            resetCallSessionState()
        }
    }

    private fun resetCallSessionState() {
        ringTimeoutJob?.cancel()
        ringTimeoutJob = null
        sessionJob?.cancel()
        sessionJob = null
        observedCallId = null
        offerSent = false
        answerSent = false
        iceRestartAttempts = 0
        stopRingtone()
        cleanupRtc()
    }

    fun cleanupRtc(stopForegroundService: Boolean = true) {
        iceRestartJob?.cancel()
        iceRestartJob = null
        durationJob?.cancel()
        durationJob = null
        iceJob?.cancel()
        iceJob = null
        connectionStateJob?.cancel()
        connectionStateJob = null
        cameraStateJob?.cancel()
        cameraStateJob = null
        audioRouteJob?.cancel()
        audioRouteJob = null
        accessoryJob?.cancel()
        accessoryJob = null
        rtcEngine?.release()
        rtcEngine = null
        syncDeviceAccessorySnapshot()
        if (stopForegroundService) {
            CallForegroundService.stop(appContext)
        }
    }

    override fun onCleared() {
        super.onCleared()
        runCatching { audioManager.unregisterAudioDeviceCallback(appAudioCallback) }
        resetCallSessionState()
    }

    private companion object {
        const val RING_TIMEOUT_MS = 45_000L
        const val MAX_ICE_RESTARTS = 3
    }
}
