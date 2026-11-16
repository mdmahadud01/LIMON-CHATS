package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.media.LimonMediaResolver
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object NotificationHelper {

    const val CHANNEL_MESSAGES = "limon_messages"
    const val CHANNEL_CALLS = "limon_calls"
    const val CHANNEL_SECURITY = "limon_security"
    const val CHANNEL_REALTIME_SYNC = "limon_realtime_sync"

    const val GROUP_MESSAGES = "limon_messages_group"
    const val GROUP_SUMMARY_ID = 77001
    const val REALTIME_SYNC_NOTIF_ID = 44090

    const val KEY_TEXT_REPLY = "key_text_reply"
    const val EXTRA_CHAT_ID = "extra_chat_id"
    const val EXTRA_PEER_UID = "extra_peer_uid"
    const val EXTRA_CALL_ID = "extra_call_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_SENDER_NAME = "extra_sender_name"

    const val ACTION_INLINE_REPLY = "com.example.action.INLINE_REPLY"
    const val ACTION_MARK_READ = "com.example.action.MARK_READ"
    const val ACTION_DECLINE_CALL = "com.example.action.DECLINE_CALL"

    private val conversationHistory = ConcurrentHashMap<String, MutableList<NotificationCompat.MessagingStyle.Message>>()

    @Volatile
    private var activeOpenChatId: String? = null

    @Volatile
    var isAppInForeground: Boolean = false
        private set

    fun setAppForeground(inForeground: Boolean) {
        isAppInForeground = inForeground
    }

    fun setActiveOpenChatId(chatId: String?) {
        activeOpenChatId = chatId
    }

    /**
     * Suppresses notifications only when the user is actively viewing [chatId]
     * in the foreground. When backgrounded or closed, notifications still fire.
     */
    fun isChatCurrentlyOpen(chatId: String): Boolean =
        isAppInForeground && !chatId.isBlank() && activeOpenChatId == chatId

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val msgChannel = NotificationChannel(
            CHANNEL_MESSAGES,
            context.getString(R.string.notif_channel_messages),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Real-time private message alerts"
            enableVibration(true)
            setShowBadge(true)
        }

        val callChannel = NotificationChannel(
            CHANNEL_CALLS,
            context.getString(R.string.notif_channel_calls),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming and ongoing WebRTC voice & video calls"
            enableVibration(true)
        }

        val secChannel = NotificationChannel(
            CHANNEL_SECURITY,
            context.getString(R.string.notif_channel_security),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Account activity and security notifications"
        }

        mgr.createNotificationChannels(listOf(msgChannel, callChannel, secChannel))
        // Remove legacy silent foreground-sync channel if present
        runCatching { mgr.deleteNotificationChannel(CHANNEL_REALTIME_SYNC) }
        runCatching { mgr.cancel(REALTIME_SYNC_NOTIF_ID) }
    }

    private const val DEDUP_PREFS = "limon_notif_dedup"

    fun shouldNotifyMessage(
        context: Context,
        uid: String,
        chatId: String,
        msgTimestampMs: Long
    ): Boolean {
        if (uid.isBlank() || chatId.isBlank() || msgTimestampMs <= 0L) return true
        val prefs = context.getSharedPreferences(DEDUP_PREFS, Context.MODE_PRIVATE)
        val key = "msg_${uid}_$chatId"
        val last = prefs.getLong(key, 0L)
        if (msgTimestampMs <= last) return false
        prefs.edit().putLong(key, msgTimestampMs).apply()
        return true
    }

    fun markChatTimestampNotifiedIfMissing(
        context: Context,
        uid: String,
        chatId: String,
        msgTimestampMs: Long
    ) {
        if (uid.isBlank() || chatId.isBlank() || msgTimestampMs <= 0L) return
        val prefs = context.getSharedPreferences(DEDUP_PREFS, Context.MODE_PRIVATE)
        val key = "msg_${uid}_$chatId"
        if (!prefs.contains(key)) {
            prefs.edit().putLong(key, msgTimestampMs).apply()
        }
    }

    fun shouldNotifyCall(context: Context, callId: String): Boolean {
        if (callId.isBlank()) return false
        val prefs = context.getSharedPreferences(DEDUP_PREFS, Context.MODE_PRIVATE)
        val key = "call_$callId"
        if (prefs.getBoolean(key, false)) return false
        prefs.edit().putBoolean(key, true).apply()
        return true
    }

    suspend fun loadCircularAvatarBitmap(
        context: Context,
        db: FirebaseFirestore?,
        url: String?
    ): Bitmap? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("limonavatar://") || url.startsWith("rtdb://")) {
            return withTimeoutOrNull(2500L) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        val file = LimonMediaResolver.resolveToLocalFile(context, db, url) ?: return@runCatching null
                        val decoded = BitmapFactory.decodeFile(file.absolutePath)
                        decoded?.let { toCircularBitmap(it) }
                    }.getOrNull()
                }
            }
        }
        return loadCircularAvatarBitmap(url)
    }

    suspend fun loadCircularAvatarBitmap(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("file://")) {
            return withContext(Dispatchers.IO) {
                runCatching {
                    val path = android.net.Uri.parse(url).path ?: return@runCatching null
                    val decoded = BitmapFactory.decodeFile(path)
                    decoded?.let { toCircularBitmap(it) }
                }.getOrNull()
            }
        }
        if (!url.startsWith("http")) return null
        return withTimeoutOrNull(2500L) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 2000
                        readTimeout = 2000
                        doInput = true
                    }
                    conn.connect()
                    val decoded = conn.inputStream.use { BitmapFactory.decodeStream(it) }
                    conn.disconnect()
                    decoded?.let { toCircularBitmap(it) }
                }.getOrNull()
            }
        }
    }

    private fun toCircularBitmap(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height).coerceIn(64, 256)
        val scaled = Bitmap.createScaledBitmap(source, size, size, true)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, size, size)
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, rect, rect, paint)
        return output
    }

    fun showMessageNotification(
        context: Context,
        chatId: String,
        peerUid: String,
        senderName: String,
        previewText: String,
        showPreview: Boolean = true,
        playSound: Boolean = true,
        vibrate: Boolean = true,
        avatarBitmap: Bitmap? = null,
        isOwnReply: Boolean = false
    ) {
        if (!hasPostPermission(context)) return
        ensureChannels(context)
        val notifId = chatId.hashCode()

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_PEER_UID, peerUid)
        }
        val openPending = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel(context.getString(R.string.reply_action))
            .build()

        val replyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_INLINE_REPLY
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_PEER_UID, peerUid)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
            putExtra(EXTRA_SENDER_NAME, senderName)
        }
        val replyFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val replyPending = PendingIntent.getBroadcast(context, notifId + 1, replyIntent, replyFlags)

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            context.getString(R.string.reply_action),
            replyPending
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .build()

        val markReadIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_READ
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val markReadPending = PendingIntent.getBroadcast(
            context,
            notifId + 2,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val markReadAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_view,
            context.getString(R.string.mark_read_action),
            markReadPending
        )
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
            .setShowsUserInterface(false)
            .build()

        val displayBody = if (showPreview || isOwnReply) previewText else "New encrypted-transport message"
        val avatarIcon = avatarBitmap?.let { IconCompat.createWithBitmap(it) }
        val mePerson = Person.Builder()
            .setName("You")
            .setKey("me")
            .build()
        val senderPerson = Person.Builder()
            .setName(senderName)
            .setKey(peerUid.ifBlank { senderName })
            .apply { if (avatarIcon != null) setIcon(avatarIcon) }
            .build()

        pushConversationShortcut(context, chatId, senderName, senderPerson, openIntent, avatarIcon)

        val msgHistory = conversationHistory.getOrPut(chatId) { mutableListOf() }
        synchronized(msgHistory) {
            val author = if (isOwnReply) null else senderPerson
            msgHistory.add(
                NotificationCompat.MessagingStyle.Message(
                    displayBody,
                    System.currentTimeMillis(),
                    author
                )
            )
            while (msgHistory.size > 6) {
                msgHistory.removeAt(0)
            }
        }

        val style = NotificationCompat.MessagingStyle(mePerson)
            .setConversationTitle(senderName)
        synchronized(msgHistory) {
            msgHistory.forEach { style.addMessage(it) }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(senderName)
            .setContentText(displayBody)
            .setStyle(style)
            .setShortcutId("chat_$chatId")
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(GROUP_MESSAGES)
            .setAutoCancel(true)
            .setOnlyAlertOnce(isOwnReply)
            .setContentIntent(openPending)
            .addAction(replyAction)
            .addAction(markReadAction)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (avatarBitmap != null) {
            builder.setLargeIcon(avatarBitmap)
        }
        if (isOwnReply || (!playSound && !vibrate)) {
            builder.setSilent(true)
        } else {
            if (!playSound) builder.setSound(null)
            if (!vibrate) builder.setVibrate(longArrayOf(0L))
        }

        val mgr = NotificationManagerCompat.from(context)
        mgr.notify(notifId, builder.build())

        val summary = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("New messages")
            .setGroup(GROUP_MESSAGES)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        mgr.notify(GROUP_SUMMARY_ID, summary)
    }

    private fun pushConversationShortcut(
        context: Context,
        chatId: String,
        senderName: String,
        person: Person,
        launchIntent: Intent,
        icon: IconCompat?
    ) {
        runCatching {
            val shortcut = ShortcutInfoCompat.Builder(context, "chat_$chatId")
                .setShortLabel(senderName.ifBlank { "Limon Chat" })
                .setLongLived(true)
                .setPerson(person)
                .setIntent(launchIntent.apply { action = Intent.ACTION_VIEW })
                .apply { if (icon != null) setIcon(icon) }
                .build()
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        }
    }

    fun clearConversationHistory(chatId: String) {
        conversationHistory.remove(chatId)
    }

    fun showIncomingCallNotification(
        context: Context,
        callId: String,
        callerName: String,
        callType: String,
        playSound: Boolean = true,
        vibrate: Boolean = true,
        callerBitmap: Bitmap? = null
    ) {
        if (!hasPostPermission(context)) return
        ensureChannels(context)
        val notifId = callId.hashCode()

        val acceptIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CALL_ID, callId)
        }
        val acceptPending = PendingIntent.getActivity(
            context,
            notifId + 10,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DECLINE_CALL
            putExtra(EXTRA_CALL_ID, callId)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val declinePending = PendingIntent.getBroadcast(
            context,
            notifId + 11,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callLabel = if (callType == "video") "Incoming Video Call" else "Incoming Voice Call"
        val callerPerson = Person.Builder()
            .setName(callerName)
            .apply {
                if (callerBitmap != null) setIcon(IconCompat.createWithBitmap(callerBitmap))
            }
            .build()

        val builder = NotificationCompat.Builder(context, CHANNEL_CALLS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(callerName)
            .setContentText(callLabel)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(acceptPending)
            .setFullScreenIntent(acceptPending, true)
            .setOngoing(true)
            .setAutoCancel(false)

        if (callerBitmap != null) {
            builder.setLargeIcon(callerBitmap)
        }
        if (!playSound && !vibrate) {
            builder.setSilent(true)
        } else {
            if (!playSound) builder.setSound(null)
            if (!vibrate) builder.setVibrate(longArrayOf(0L))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setStyle(
                NotificationCompat.CallStyle.forIncomingCall(callerPerson, declinePending, acceptPending)
                    .setIsVideo(callType == "video")
            )
        } else {
            builder
                .addAction(
                    android.R.drawable.ic_menu_call,
                    context.getString(R.string.accept_call),
                    acceptPending
                )
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    context.getString(R.string.decline_call),
                    declinePending
                )
        }

        NotificationManagerCompat.from(context).notify(notifId, builder.build())
    }

    fun showMissedCallNotification(
        context: Context,
        callId: String,
        callerName: String,
        callType: String,
        peerUid: String = ""
    ) {
        if (!hasPostPermission(context)) return
        ensureChannels(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (peerUid.isNotBlank()) putExtra(EXTRA_PEER_UID, peerUid)
        }
        val openPending = PendingIntent.getActivity(
            context,
            callId.hashCode() + 25,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val label = if (callType == "video") "Missed video call" else "Missed voice call"
        val notification = NotificationCompat.Builder(context, CHANNEL_CALLS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(callerName.ifBlank { "Missed Call" })
            .setContentText(label)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(callId.hashCode() + 100, notification)
    }

    fun cancelCallNotification(context: Context, callId: String) {
        if (callId.isNotBlank()) {
            NotificationManagerCompat.from(context).cancel(callId.hashCode())
        }
    }

    fun cancelNotification(context: Context, notifId: Int) {
        NotificationManagerCompat.from(context).cancel(notifId)
    }

    fun showSecurityNotification(context: Context, title: String, body: String) {
        if (!hasPostPermission(context)) return
        ensureChannels(context)
        val openIntent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            context,
            9001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_SECURITY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    private fun hasPostPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
}
