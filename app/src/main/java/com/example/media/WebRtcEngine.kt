package com.example.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

enum class CallAudioRoute(val label: String) {
    EARPIECE("Earpiece"),
    SPEAKER("Speaker"),
    WIRED_HEADSET("Earphone"),
    BLUETOOTH("Bluetooth")
}

fun shouldUseProximitySensor(
    isVideoCall: Boolean,
    isSpeakerOn: Boolean,
    audioRoute: CallAudioRoute
): Boolean = !isVideoCall && !isSpeakerOn && audioRoute == CallAudioRoute.EARPIECE

class WebRtcEngine(private val context: Context) {

    val eglBase: EglBase = EglBase.create()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var audioFocusRequest: AudioFocusRequest? = null

    private val _connectionState = MutableStateFlow("Initializing")
    val connectionState = _connectionState.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera = _isFrontCamera.asStateFlow()

    private val _connectedAccessory = MutableStateFlow<CallAudioRoute?>(null)
    val connectedAccessory = _connectedAccessory.asStateFlow()

    private val _audioRoute = MutableStateFlow(CallAudioRoute.EARPIECE)
    val audioRoute = _audioRoute.asStateFlow()

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshAudioDevices(autoSwitchToAccessory = true)
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshAudioDevices(autoSwitchToAccessory = false)
        }
    }

    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null

    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null
    private var iceRestartNeededCallback: (() -> Unit)? = null

    private val iceLock = Any()
    private var isRemoteDescriptionSet = false
    private val pendingRemoteIce = mutableListOf<IceCandidate>()

    private val iceServers: List<PeerConnection.IceServer> by lazy {
        val list = mutableListOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun.cloudflare.com:3478").createIceServer()
        )
        if (BuildConfig.TURN_URL.isNotBlank()) {
            val builder = PeerConnection.IceServer.builder(BuildConfig.TURN_URL)
            if (BuildConfig.TURN_USERNAME.isNotBlank()) {
                builder.setUsername(BuildConfig.TURN_USERNAME)
            }
            if (BuildConfig.TURN_CREDENTIAL.isNotBlank()) {
                builder.setPassword(BuildConfig.TURN_CREDENTIAL)
            }
            list.add(builder.createIceServer())
        }
        list
    }

    private fun ensureFactory() {
        if (factory != null) return
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    fun attachRenderers(local: SurfaceViewRenderer?, remote: SurfaceViewRenderer?) {
        mainHandler.post {
            if (localRenderer != null && localRenderer != local) {
                localRenderer?.let { prev -> runCatching { localVideoTrack?.removeSink(prev) } }
            }
            if (remoteRenderer != null && remoteRenderer != remote) {
                remoteRenderer?.let { prev -> runCatching { remoteVideoTrack?.removeSink(prev) } }
            }
            localRenderer = local
            remoteRenderer = remote
            local?.let {
                runCatching {
                    it.init(eglBase.eglBaseContext, null)
                    it.setMirror(_isFrontCamera.value)
                    it.setEnableHardwareScaler(true)
                }
                localVideoTrack?.addSink(it)
            }
            remote?.let {
                runCatching {
                    it.init(eglBase.eglBaseContext, null)
                    it.setEnableHardwareScaler(true)
                }
                remoteVideoTrack?.addSink(it)
            }
        }
    }

    fun initializeSession(
        isVideo: Boolean,
        onIceRestartNeeded: (() -> Unit)? = null,
        onIceCandidate: (IceCandidate) -> Unit
    ) {
        synchronized(iceLock) {
            isRemoteDescriptionSet = false
            pendingRemoteIce.clear()
        }
        iceRestartNeededCallback = onIceRestartNeeded
        ensureFactory()
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = factory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    _connectionState.value = when (state) {
                        PeerConnection.IceConnectionState.CONNECTED,
                        PeerConnection.IceConnectionState.COMPLETED -> "Connected"
                        PeerConnection.IceConnectionState.CHECKING -> "Connecting..."
                        PeerConnection.IceConnectionState.DISCONNECTED -> {
                            onIceRestartNeeded?.invoke()
                            "Reconnecting..."
                        }
                        PeerConnection.IceConnectionState.FAILED -> {
                            onIceRestartNeeded?.invoke()
                            "Reconnecting (ICE)..."
                        }
                        PeerConnection.IceConnectionState.CLOSED -> "Ended"
                        else -> "Ringing"
                    }
                }

                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let(onIceCandidate)
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onAddStream(stream: MediaStream?) {
                    val track = stream?.videoTracks?.firstOrNull() ?: return
                    remoteVideoTrack = track
                    mainHandler.post { remoteRenderer?.let { track.addSink(it) } }
                }

                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(dc: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                    val track = receiver?.track() as? VideoTrack ?: return
                    remoteVideoTrack = track
                    mainHandler.post { remoteRenderer?.let { track.addSink(it) } }
                }
            }
        )

        setupLocalMedia(isVideo)
    }

    fun ensureCameraVideoStarted() {
        if (localVideoTrack != null) return
        val pcFactory = factory ?: return
        val pc = peerConnection ?: return
        val hasCameraPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCameraPerm) return

        runCatching {
            val enumerator = Camera2Enumerator(context)
            val frontName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            val deviceName = frontName ?: enumerator.deviceNames.firstOrNull()
            _isFrontCamera.value = frontName != null
            if (deviceName != null) {
                videoCapturer = enumerator.createCapturer(deviceName, null)
                surfaceTextureHelper = SurfaceTextureHelper.create("LimonCaptureThread", eglBase.eglBaseContext)
                localVideoSource = pcFactory.createVideoSource(videoCapturer?.isScreencast == true)
                videoCapturer?.initialize(surfaceTextureHelper, context, localVideoSource?.capturerObserver)
                videoCapturer?.startCapture(720, 1280, 30)
                localVideoTrack = pcFactory.createVideoTrack("limon_video", localVideoSource)
                localVideoTrack?.let { track ->
                    pc.addTrack(track, listOf("limon_stream"))
                    mainHandler.post { localRenderer?.let { track.addSink(it) } }
                }
            }
        }
    }

    private fun setupLocalMedia(isVideo: Boolean) {
        val pcFactory = factory ?: return
        val pc = peerConnection ?: return

        requestCallAudioFocus()

        val hasAudioPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasAudioPerm) {
            runCatching {
                val audioConstraints = MediaConstraints()
                localAudioSource = pcFactory.createAudioSource(audioConstraints)
                localAudioTrack = pcFactory.createAudioTrack("limon_audio", localAudioSource)
                localAudioTrack?.let { pc.addTrack(it, listOf("limon_stream")) }
            }
        }

        runCatching {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, mainHandler)
        }
        refreshAudioDevices(autoSwitchToAccessory = true)

        if (isVideo) {
            ensureCameraVideoStarted()
        }
        val initialRoute = _connectedAccessory.value
            ?: if (isVideo) CallAudioRoute.SPEAKER else CallAudioRoute.EARPIECE
        selectAudioRoute(initialRoute)
    }

    fun createOffer(iceRestart: Boolean = false, onOfferCreated: (String) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            if (iceRestart) {
                mandatory.add(MediaConstraints.KeyValuePair("IceRestart", "true"))
                peerConnection?.restartIce()
            }
        }
        peerConnection?.createOffer(
            object : SimpleSdpObserver() {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    sdp ?: return
                    peerConnection?.setLocalDescription(
                        object : SimpleSdpObserver() {
                            override fun onSetSuccess() {
                                onOfferCreated(sdp.description)
                            }
                        },
                        sdp
                    )
                }
            },
            constraints
        )
    }

    fun createAnswer(offerSdp: String, onAnswerCreated: (String) -> Unit) {
        val remoteDesc = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        peerConnection?.setRemoteDescription(
            object : SimpleSdpObserver() {
                override fun onSetSuccess() {
                    markRemoteDescriptionReadyAndFlushIce()
                    val constraints = MediaConstraints().apply {
                        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                    }
                    peerConnection?.createAnswer(
                        object : SimpleSdpObserver() {
                            override fun onCreateSuccess(sdp: SessionDescription?) {
                                sdp ?: return
                                peerConnection?.setLocalDescription(
                                    object : SimpleSdpObserver() {
                                        override fun onSetSuccess() {
                                            onAnswerCreated(sdp.description)
                                        }
                                    },
                                    sdp
                                )
                            }
                        },
                        constraints
                    )
                }
            },
            remoteDesc
        )
    }

    fun applyRemoteAnswer(answerSdp: String) {
        val desc = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
        peerConnection?.setRemoteDescription(
            object : SimpleSdpObserver() {
                override fun onSetSuccess() {
                    markRemoteDescriptionReadyAndFlushIce()
                }
            },
            desc
        )
    }

    private fun markRemoteDescriptionReadyAndFlushIce() {
        val buffered: List<IceCandidate>
        synchronized(iceLock) {
            isRemoteDescriptionSet = true
            buffered = pendingRemoteIce.toList()
            pendingRemoteIce.clear()
        }
        val pc = peerConnection ?: return
        buffered.forEach { pc.addIceCandidate(it) }
    }

    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, candidate: String) {
        val ice = IceCandidate(sdpMid, sdpMLineIndex, candidate)
        synchronized(iceLock) {
            if (!isRemoteDescriptionSet) {
                pendingRemoteIce.add(ice)
                return
            }
        }
        peerConnection?.addIceCandidate(ice)
    }

    fun setMicMuted(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
    }

    fun setVideoEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                _isFrontCamera.value = isFrontCamera
                mainHandler.post { localRenderer?.setMirror(isFrontCamera) }
            }

            override fun onCameraSwitchError(errorDescription: String?) {
                Log.w("WebRtcEngine", "Camera switch error: $errorDescription")
            }
        })
    }

    fun setSpeakerphone(enabled: Boolean) {
        if (enabled) {
            selectAudioRoute(CallAudioRoute.SPEAKER)
        } else {
            val fallback = _connectedAccessory.value ?: CallAudioRoute.EARPIECE
            selectAudioRoute(fallback)
        }
    }

    fun selectAudioRoute(route: CallAudioRoute) {
        _audioRoute.value = route
        runCatching {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val commDevices = audioManager.availableCommunicationDevices
                val targetDevice = when (route) {
                    CallAudioRoute.BLUETOOTH -> commDevices.firstOrNull { isBluetoothAudioType(it.type) }
                    CallAudioRoute.WIRED_HEADSET -> commDevices.firstOrNull { isWiredAudioType(it.type) }
                    CallAudioRoute.SPEAKER -> commDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                    CallAudioRoute.EARPIECE -> commDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
                }
                if (targetDevice != null) {
                    audioManager.setCommunicationDevice(targetDevice)
                } else {
                    audioManager.clearCommunicationDevice()
                }
            }
            @Suppress("DEPRECATION")
            when (route) {
                CallAudioRoute.SPEAKER -> {
                    if (audioManager.isBluetoothScoOn) {
                        audioManager.stopBluetoothSco()
                        audioManager.isBluetoothScoOn = false
                    }
                    audioManager.isSpeakerphoneOn = true
                }
                CallAudioRoute.BLUETOOTH -> {
                    audioManager.isSpeakerphoneOn = false
                    if (!audioManager.isBluetoothScoOn) {
                        audioManager.startBluetoothSco()
                        audioManager.isBluetoothScoOn = true
                    }
                }
                CallAudioRoute.WIRED_HEADSET,
                CallAudioRoute.EARPIECE -> {
                    if (audioManager.isBluetoothScoOn) {
                        audioManager.stopBluetoothSco()
                        audioManager.isBluetoothScoOn = false
                    }
                    audioManager.isSpeakerphoneOn = false
                }
            }
        }
    }

    fun refreshAudioDevices(autoSwitchToAccessory: Boolean) {
        val devices = runCatching {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        }.getOrNull().orEmpty()
        val hasBluetooth = devices.any { isBluetoothAudioType(it.type) }
        val hasWired = devices.any { isWiredAudioType(it.type) }
        val accessory = when {
            hasBluetooth -> CallAudioRoute.BLUETOOTH
            hasWired -> CallAudioRoute.WIRED_HEADSET
            else -> null
        }
        _connectedAccessory.value = accessory
        if (autoSwitchToAccessory && accessory != null && _audioRoute.value == CallAudioRoute.EARPIECE) {
            selectAudioRoute(accessory)
        } else if (!hasBluetooth && _audioRoute.value == CallAudioRoute.BLUETOOTH) {
            selectAudioRoute(accessory ?: CallAudioRoute.EARPIECE)
        } else if (!hasWired && _audioRoute.value == CallAudioRoute.WIRED_HEADSET) {
            selectAudioRoute(accessory ?: CallAudioRoute.EARPIECE)
        }
    }

    private fun isBluetoothAudioType(type: Int): Boolean =
        type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_HEADSET) ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_SPEAKER)

    private fun isWiredAudioType(type: Int): Boolean =
        type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && type == AudioDeviceInfo.TYPE_USB_HEADSET) ||
            type == AudioDeviceInfo.TYPE_USB_DEVICE

    private fun requestCallAudioFocus() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attrs)
                    .build()
                audioFocusRequest = req
                audioManager.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            }
        }
    }

    fun release() {
        synchronized(iceLock) {
            isRemoteDescriptionSet = false
            pendingRemoteIce.clear()
        }
        runCatching { audioManager.unregisterAudioDeviceCallback(audioDeviceCallback) }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            }
            @Suppress("DEPRECATION")
            if (audioManager.isBluetoothScoOn) {
                audioManager.stopBluetoothSco()
                audioManager.isBluetoothScoOn = false
            }
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
        }
        runCatching { videoCapturer?.stopCapture() }
        runCatching { videoCapturer?.dispose() }
        videoCapturer = null
        runCatching { surfaceTextureHelper?.dispose() }
        surfaceTextureHelper = null
        runCatching { localAudioTrack?.dispose() }
        runCatching { localAudioSource?.dispose() }
        runCatching { localVideoTrack?.dispose() }
        runCatching { localVideoSource?.dispose() }
        localVideoTrack = null
        localVideoSource = null
        localAudioTrack = null
        localAudioSource = null
        runCatching { peerConnection?.close() }
        runCatching { peerConnection?.dispose() }
        peerConnection = null
        runCatching { localRenderer?.release() }
        runCatching { remoteRenderer?.release() }
        localRenderer = null
        remoteRenderer = null
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        }
        runCatching { audioManager.mode = AudioManager.MODE_NORMAL }
        runCatching { eglBase.release() }
    }

    private open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) {
            Log.w("WebRtcEngine", "SDP create error: $error")
        }
        override fun onSetFailure(error: String?) {
            Log.w("WebRtcEngine", "SDP set error: $error")
        }
    }
}
