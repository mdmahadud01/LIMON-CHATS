package com.example.notifications

import android.Manifest
import android.app.ActivityManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Process
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class CallForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isPendingStart.set(false)
        isServiceRunning.set(true)

        val shouldStopImmediately = intent?.action == ACTION_STOP || stopRequested.getAndSet(false)
        NotificationHelper.ensureChannels(this)

        if (!hasRecordAudioPermission(this)) {
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }

        val peerName = intent?.getStringExtra(EXTRA_PEER_NAME) ?: "Active Call"
        val callType = intent?.getStringExtra(EXTRA_CALL_TYPE) ?: "voice"
        val callId = intent?.getStringExtra(EXTRA_CALL_ID) ?: ""

        val openIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NotificationHelper.EXTRA_CALL_ID, callId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            501,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_CALLS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(peerName)
            .setContentText(if (callType == "video") "Limon Video Call in progress" else "Limon Voice Call in progress")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        val startedForeground = startForegroundWithFallback(notification, callType)
        if (!startedForeground) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundCallCount.incrementAndGet()
        isForegroundActive.set(true)
        if (shouldStopImmediately) {
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithFallback(notification: Notification, callType: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return runCatching {
                startForeground(NOTIF_ID, notification)
                true
            }.getOrDefault(false)
        }

        val includeCamera = callType == "video" &&
            hasCameraPermission(this) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val primaryType = if (includeCamera) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        }

        val primaryOk = try {
            startForeground(NOTIF_ID, notification, primaryType)
            true
        } catch (_: SecurityException) {
            false
        }
        if (primaryOk) return true

        if (includeCamera) {
            val micOk = try {
                startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
                true
            } catch (_: SecurityException) {
                false
            }
            if (micOk) return true
        }
        return false
    }

    private fun stopForegroundCompat() {
        isForegroundActive.set(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        isForegroundActive.set(false)
        isServiceRunning.set(false)
        isPendingStart.set(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIF_ID = 4401
        private const val ACTION_STOP = "com.example.action.STOP_CALL_SERVICE"
        private const val EXTRA_PEER_NAME = "extra_peer_name"
        private const val EXTRA_CALL_TYPE = "extra_call_type"
        private const val EXTRA_CALL_ID = "extra_call_id"

        private val isPendingStart = AtomicBoolean(false)
        private val isServiceRunning = AtomicBoolean(false)
        private val isForegroundActive = AtomicBoolean(false)
        private val stopRequested = AtomicBoolean(false)
        internal val startForegroundCallCount = AtomicInteger(0)

        fun hasRecordAudioPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        fun hasCameraPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        private fun isAppVisibleOrForeground(context: Context): Boolean {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return true
            val processes = am.runningAppProcesses ?: return true
            if (processes.isEmpty()) return true
            val myPid = Process.myPid()
            val current = processes.firstOrNull { it.pid == myPid } ?: return true
            return current.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
        }

        fun start(context: Context, callId: String, peerName: String, callType: String) {
            if (!hasRecordAudioPermission(context)) return
            if (!isAppVisibleOrForeground(context)) return
            stopRequested.set(false)
            isPendingStart.set(true)
            val intent = Intent(context, CallForegroundService::class.java).apply {
                putExtra(EXTRA_CALL_ID, callId)
                putExtra(EXTRA_PEER_NAME, peerName)
                putExtra(EXTRA_CALL_TYPE, callType)
            }
            val launched = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                true
            }.getOrDefault(false)
            if (!launched) {
                isPendingStart.set(false)
            }
        }

        fun stop(context: Context) {
            if (isPendingStart.get() && !isForegroundActive.get()) {
                stopRequested.set(true)
                return
            }
            if (!isServiceRunning.get() && !isForegroundActive.get()) {
                stopRequested.set(false)
                return
            }
            stopRequested.set(false)
            runCatching {
                context.stopService(Intent(context, CallForegroundService::class.java))
            }
        }

        internal fun resetStateForTesting() {
            isPendingStart.set(false)
            isServiceRunning.set(false)
            isForegroundActive.set(false)
            stopRequested.set(false)
            startForegroundCallCount.set(0)
        }
    }
}
