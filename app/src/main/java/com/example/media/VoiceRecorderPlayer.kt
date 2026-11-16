package com.example.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class RecordedVoiceResult(
    val file: File,
    val durationMs: Long,
    val waveform: List<Double>
)

class VoiceRecorderManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs: Long = 0L
    private var accumulatedMs: Long = 0L
    private var meterJob: Job? = null
    private val samples = mutableListOf<Double>()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused = _isPaused.asStateFlow()

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs = _elapsedMs.asStateFlow()

    private val _liveAmplitudes = MutableStateFlow<List<Double>>(emptyList())
    val liveAmplitudes = _liveAmplitudes.asStateFlow()

    fun startRecording(scope: CoroutineScope): Boolean {
        stopInternal()
        val file = File(context.cacheDir, "voice_${UUID.randomUUID()}.m4a")
        val profiles = listOf(
            Pair(96000, 44100),
            Pair(32000, 16000)
        )
        for ((bitRate, sampleRate) in profiles) {
            val started = runCatching {
                val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
                rec.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(bitRate)
                    setAudioSamplingRate(sampleRate)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }
                recorder = rec
                true
            }.getOrDefault(false)
            if (started) {
                outputFile = file
                startedAtMs = System.currentTimeMillis()
                accumulatedMs = 0L
                samples.clear()
                _isRecording.value = true
                _isPaused.value = false
                _elapsedMs.value = 0L
                _liveAmplitudes.value = emptyList()

                meterJob = scope.launch(Dispatchers.Default) {
                    while (isActive && _isRecording.value) {
                        if (!_isPaused.value) {
                            val amp = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                            val normalized = (amp / 32767.0).coerceIn(0.15, 1.0)
                            samples.add(normalized)
                            _liveAmplitudes.value = samples.takeLast(32)
                            _elapsedMs.value = accumulatedMs + (System.currentTimeMillis() - startedAtMs)
                        }
                        delay(90L)
                    }
                }
                return true
            } else {
                runCatching { recorder?.release() }
                recorder = null
            }
        }
        stopInternal()
        file.delete()
        return false
    }

    fun pauseRecording(): Boolean {
        if (!_isRecording.value || _isPaused.value) return false
        return runCatching {
            recorder?.pause()
            accumulatedMs += (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L)
            _elapsedMs.value = accumulatedMs
            _isPaused.value = true
            true
        }.getOrDefault(false)
    }

    fun resumeRecording(): Boolean {
        if (!_isRecording.value || !_isPaused.value) return false
        return runCatching {
            recorder?.resume()
            startedAtMs = System.currentTimeMillis()
            _isPaused.value = false
            true
        }.getOrDefault(false)
    }

    fun stopAndFinish(): RecordedVoiceResult? {
        val file = outputFile ?: return null
        val activeSegment = if (_isPaused.value) 0L else (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L)
        val duration = (accumulatedMs + activeSegment).coerceAtLeast(350L)
        val wave = downsampleWaveform(samples)
        stopInternal()
        return if (file.exists() && file.length() > 0) {
            RecordedVoiceResult(file = file, durationMs = duration, waveform = wave)
        } else {
            file.delete()
            null
        }
    }

    fun cancelRecording() {
        val file = outputFile
        stopInternal()
        file?.delete()
    }

    private fun stopInternal() {
        meterJob?.cancel()
        meterJob = null
        _isRecording.value = false
        _isPaused.value = false
        accumulatedMs = 0L
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
    }

    private fun downsampleWaveform(raw: List<Double>): List<Double> {
        if (raw.isEmpty()) return List(24) { 0.35 }
        val target = 28
        if (raw.size <= target) return raw
        val step = raw.size.toDouble() / target
        return List(target) { i ->
            val idx = (i * step).toInt().coerceIn(0, raw.lastIndex)
            raw[idx]
        }
    }
}

data class VoicePlaybackState(
    val playingMessageId: String? = null,
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val currentMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1.0f,
    val error: String? = null
)

class VoicePlayerManager(
    private val cacheDir: File? = null,
    private val context: Context? = null,
    private val firestore: FirebaseFirestore? = null
) {

    private var player: MediaPlayer? = null
    private var progressJob: Job? = null
    private var prepareJob: Job? = null

    private val _state = MutableStateFlow(VoicePlaybackState())
    val state = _state.asStateFlow()

    fun togglePlay(
        messageId: String,
        url: String,
        scope: CoroutineScope
    ) {
        val current = _state.value
        if (current.playingMessageId == messageId && player != null) {
            if (current.isPlaying) {
                runCatching { player?.pause() }
                _state.value = current.copy(isPlaying = false, error = null)
            } else {
                runCatching { player?.start() }
                _state.value = current.copy(isPlaying = true, error = null)
            }
            return
        }

        stop()
        _state.value = _state.value.copy(
            playingMessageId = messageId,
            isPlaying = false,
            progress = 0f,
            error = null
        )

        prepareJob = scope.launch {
            try {
                val resolvedPath = withContext(Dispatchers.IO) {
                    val appCtx = context
                    if (appCtx != null) {
                        LimonMediaResolver.resolveToLocalFile(appCtx, firestore, url, "$messageId.m4a")
                            ?.absolutePath
                    } else null
                }
                val dataSource = resolvedPath ?: run {
                    if (url.startsWith("rtdb://") || url.startsWith("limonmedia://")) {
                        throw IllegalStateException("Voice message unavailable")
                    }
                    url
                }

                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    setDataSource(dataSource)
                    setOnPreparedListener { readyPlayer ->
                        applySpeed(readyPlayer, _state.value.speed)
                        readyPlayer.start()
                        _state.value = _state.value.copy(
                            playingMessageId = messageId,
                            isPlaying = true,
                            durationMs = readyPlayer.duration.toLong().coerceAtLeast(1L),
                            error = null
                        )
                        startProgressLoop(scope)
                    }
                    setOnCompletionListener {
                        stop()
                    }
                    setOnErrorListener { _, _, _ ->
                        val currentSpeed = _state.value.speed
                        stop()
                        _state.value = VoicePlaybackState(
                            speed = currentSpeed,
                            error = "Unable to play voice message"
                        )
                        true
                    }
                    prepareAsync()
                }
                player = mp
            } catch (_: Exception) {
                val currentSpeed = _state.value.speed
                stop()
                _state.value = VoicePlaybackState(
                    speed = currentSpeed,
                    error = "Unable to play voice message"
                )
            }
        }
    }

    fun seekTo(fraction: Float) {
        val mp = player ?: return
        val dur = mp.duration.coerceAtLeast(1)
        val target = (dur * fraction.coerceIn(0f, 1f)).toInt()
        runCatching { mp.seekTo(target) }
        _state.value = _state.value.copy(
            progress = fraction.coerceIn(0f, 1f),
            currentMs = target.toLong()
        )
    }

    fun cycleSpeed() {
        val next = when (_state.value.speed) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        _state.value = _state.value.copy(speed = next)
        player?.let { applySpeed(it, next) }
    }

    private fun applySpeed(mp: MediaPlayer, speed: Float) {
        runCatching {
            val params = mp.playbackParams
            params.speed = speed
            mp.playbackParams = params
        }
    }

    private fun startProgressLoop(scope: CoroutineScope) {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive && player != null) {
                val mp = player ?: break
                val dur = mp.duration.toLong().coerceAtLeast(1L)
                val pos = runCatching { mp.currentPosition.toLong() }.getOrDefault(0L)
                _state.value = _state.value.copy(
                    currentMs = pos,
                    durationMs = dur,
                    progress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                )
                delay(100L)
            }
        }
    }

    fun stop() {
        prepareJob?.cancel()
        prepareJob = null
        progressJob?.cancel()
        progressJob = null
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        _state.value = VoicePlaybackState(speed = _state.value.speed)
    }
}
