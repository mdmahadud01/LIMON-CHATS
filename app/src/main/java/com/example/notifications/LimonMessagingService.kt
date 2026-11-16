package com.example.notifications

import android.content.Context
import com.example.R
import com.example.data.ChatRepository
import com.example.data.DeviceIdentity
import com.example.data.SettingsDataStore
import com.example.data.UserRepository
import com.example.model.CallStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class LimonMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (token.isBlank()) return
        cacheFcmToken(applicationContext, token)
        if (Firebase.auth.currentUser == null) return
        val db = FirebaseFirestore.getInstance(getString(R.string.firestore_database_id))
        val deviceId = DeviceIdentity.getDeviceId(applicationContext)
        serviceScope.launch {
            UserRepository(db).saveFcmToken(token, deviceId)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val db = FirebaseFirestore.getInstance(getString(R.string.firestore_database_id))
        val currentUid = Firebase.auth.currentUser?.uid

        runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(4500L) {
                val showPreview = runCatching {
                    SettingsDataStore(applicationContext).currentSettings().notificationPreview
                }.getOrDefault(true)

                when (data["type"]) {
                    "message" -> {
                        val chatId = data["chatId"] ?: return@withTimeoutOrNull
                        val peerUid = data["senderId"] ?: data["peerUid"] ?: return@withTimeoutOrNull
                        if (peerUid == currentUid) return@withTimeoutOrNull

                        val senderName = data["senderName"] ?: "Limon Chat"
                        val preview = data["preview"] ?: data["body"] ?: message.notification?.body ?: "New message"
                        val messageId = data["messageId"]
                        val senderPhotoUrl = data["senderPhotoUrl"]

                        if (currentUid != null) {
                            serviceScope.launch {
                                runCatching {
                                    ChatRepository(db, applicationContext).acknowledgeChatDelivered(
                                        chatId = chatId,
                                        uid = currentUid,
                                        messageId = messageId
                                    )
                                }
                            }
                        }

                        if (NotificationHelper.isChatCurrentlyOpen(chatId)) {
                            return@withTimeoutOrNull
                        }

                        val msgTs = data["timestampMs"]?.toLongOrNull() ?: System.currentTimeMillis()
                        if (currentUid != null && !NotificationHelper.shouldNotifyMessage(applicationContext, currentUid, chatId, msgTs)) {
                            return@withTimeoutOrNull
                        }

                        val avatarBitmap = withTimeoutOrNull(1200L) {
                            NotificationHelper.loadCircularAvatarBitmap(applicationContext, db, senderPhotoUrl)
                        }
                        NotificationHelper.showMessageNotification(
                            context = applicationContext,
                            chatId = chatId,
                            peerUid = peerUid,
                            senderName = senderName,
                            previewText = preview,
                            showPreview = showPreview,
                            playSound = true,
                            vibrate = true,
                            avatarBitmap = avatarBitmap
                        )
                    }

                    "call" -> {
                        val callId = data["callId"] ?: return@withTimeoutOrNull
                        if (!NotificationHelper.shouldNotifyCall(applicationContext, callId)) {
                            return@withTimeoutOrNull
                        }

                        val callerName = data["callerName"] ?: "Incoming Call"
                        val callType = data["callType"] ?: "voice"
                        val callerPhotoUrl = data["callerPhotoUrl"]
                        val avatarBitmap = withTimeoutOrNull(1200L) {
                            NotificationHelper.loadCircularAvatarBitmap(applicationContext, db, callerPhotoUrl)
                        }
                        NotificationHelper.showIncomingCallNotification(
                            context = applicationContext,
                            callId = callId,
                            callerName = callerName,
                            callType = callType,
                            playSound = true,
                            vibrate = true,
                            callerBitmap = avatarBitmap
                        )

                        serviceScope.launch {
                            val callSnap = runCatching {
                                db.collection("calls").document(callId).get().await()
                            }.getOrNull()
                            if (callSnap != null && callSnap.exists()) {
                                val status = callSnap.getString("status")
                                if (status != null && status != CallStatus.RINGING.value) {
                                    NotificationHelper.cancelCallNotification(applicationContext, callId)
                                }
                            }
                        }
                    }

                    "call_ended" -> {
                        val callId = data["callId"] ?: return@withTimeoutOrNull
                        NotificationHelper.cancelCallNotification(applicationContext, callId)
                        if (data["status"] == CallStatus.MISSED.value) {
                            NotificationHelper.showMissedCallNotification(
                                context = applicationContext,
                                callId = callId,
                                callerName = data["callerName"] ?: "Limon User",
                                callType = data["callType"] ?: "voice",
                                peerUid = data["callerId"] ?: ""
                            )
                        }
                    }

                    "security" -> {
                        val title = data["title"] ?: "Limon Chat Security"
                        val body = data["body"] ?: "New login activity detected on your account."
                        NotificationHelper.showSecurityNotification(applicationContext, title, body)
                    }

                    else -> {
                        val title = data["title"] ?: message.notification?.title
                        val body = data["body"] ?: message.notification?.body
                        if (!body.isNullOrBlank()) {
                            NotificationHelper.showSecurityNotification(
                                applicationContext,
                                title ?: "Limon Chat",
                                body
                            )
                        } else {
                            // Wake-up / empty-data FCM tick: sync latest unread messages & ringing calls from server
                            RealtimeBackgroundSyncEngine.runSync(applicationContext)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val FCM_PREFS = "limon_fcm_prefs"
        private const val KEY_CACHED_TOKEN = "cached_fcm_token"

        fun cacheFcmToken(context: Context, token: String) {
            context.getSharedPreferences(FCM_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CACHED_TOKEN, token)
                .apply()
        }

        fun getCachedFcmToken(context: Context): String? =
            context.getSharedPreferences(FCM_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_CACHED_TOKEN, null)
    }
}
