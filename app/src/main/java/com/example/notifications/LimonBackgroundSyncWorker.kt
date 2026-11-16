package com.example.notifications

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.ChatRepository
import com.example.data.SettingsDataStore
import com.example.model.CallSession
import com.example.model.CallStatus
import com.example.model.ChatThread
import com.example.model.PublicProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

object RealtimeBackgroundSyncEngine {

    private const val TAG = "LimonBgSyncEngine"

    suspend fun runSync(context: Context) {
        val appCtx = context.applicationContext
        val uid = Firebase.auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance(appCtx.getString(R.string.firestore_database_id))
        val settings = runCatching { SettingsDataStore(appCtx).currentSettings() }.getOrNull()

        withTimeoutOrNull(20_000L) {
            if (settings?.messageNotifications != false) {
                checkUnreadChats(
                    context = appCtx,
                    db = db,
                    uid = uid,
                    showPreview = settings?.notificationPreview ?: true,
                    playSound = settings?.notificationSound ?: true,
                    vibrate = settings?.notificationVibration ?: true
                )
            }
            if (settings?.callNotifications != false) {
                checkIncomingCalls(
                    context = appCtx,
                    db = db,
                    uid = uid,
                    playSound = settings?.notificationSound ?: true,
                    vibrate = settings?.notificationVibration ?: true
                )
            }
        }
    }

    private suspend fun checkUnreadChats(
        context: Context,
        db: FirebaseFirestore,
        uid: String,
        showPreview: Boolean,
        playSound: Boolean,
        vibrate: Boolean
    ) {
        val chatsSnap = runCatching {
            db.collection("chats")
                .whereArrayContains("memberUids", uid)
                .get(Source.SERVER)
                .await()
        }.getOrElse {
            runCatching {
                db.collection("chats")
                    .whereArrayContains("memberUids", uid)
                    .get()
                    .await()
            }.getOrNull()
        } ?: return

        val prefsSnap = runCatching {
            db.collection("users").document(uid).collection("chatPreferences").get().await()
        }.getOrNull()
        val mutedChatIds = prefsSnap?.documents
            ?.filter { it.getBoolean("muted") == true }
            ?.map { it.id }
            ?.toSet()
            ?: emptySet()

        val repo = ChatRepository(db, context)

        chatsSnap.toObjects(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .forEach { chat ->
                val unread = (chat.unreadBy[uid] ?: 0L).toInt()
                if (unread <= 0 ||
                    chat.lastMessageSenderId == uid ||
                    chat.id in mutedChatIds ||
                    NotificationHelper.isChatCurrentlyOpen(chat.id)
                ) {
                    return@forEach
                }
                val ts = chat.lastMessageAt ?: chat.updatedAt ?: return@forEach
                val tsMs = ts.seconds * 1000L + (ts.nanoseconds / 1_000_000L)
                if (!NotificationHelper.shouldNotifyMessage(context, uid, chat.id, tsMs)) {
                    return@forEach
                }

                runCatching { repo.acknowledgeChatDelivered(chat.id, uid) }
                val peerUid = chat.memberUids.firstOrNull { it != uid } ?: chat.lastMessageSenderId
                val peerProfile = if (peerUid.isNotBlank()) {
                    runCatching {
                        db.collection("publicProfiles").document(peerUid).get().await()
                            .toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.getOrNull()
                } else null

                val senderName = peerProfile?.displayName?.ifBlank { peerProfile.username } ?: "Limon Chat"
                val preview = chat.lastMessageText.ifBlank {
                    when (chat.lastMessageType) {
                        "image" -> "Sent a photo"
                        "video" -> "Sent a video"
                        "voice" -> "Sent a voice message"
                        "document" -> "Sent a document"
                        "location" -> "Shared a location"
                        "contact" -> "Shared a contact"
                        else -> "New message"
                    }
                }
                val avatarBitmap = NotificationHelper.loadCircularAvatarBitmap(
                    context = context,
                    db = db,
                    url = peerProfile?.photoUrl
                )
                NotificationHelper.showMessageNotification(
                    context = context,
                    chatId = chat.id,
                    peerUid = peerUid,
                    senderName = senderName,
                    previewText = preview,
                    showPreview = showPreview,
                    playSound = playSound,
                    vibrate = vibrate,
                    avatarBitmap = avatarBitmap
                )
            }
    }

    private suspend fun checkIncomingCalls(
        context: Context,
        db: FirebaseFirestore,
        uid: String,
        playSound: Boolean,
        vibrate: Boolean
    ) {
        val callsSnap = runCatching {
            db.collection("calls")
                .whereArrayContains("memberUids", uid)
                .whereEqualTo("status", CallStatus.RINGING.value)
                .get(Source.SERVER)
                .await()
        }.getOrNull() ?: return

        val nowMs = System.currentTimeMillis()
        callsSnap.toObjects(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .forEach { call ->
                if (call.receiverId != uid || uid in call.deletedForUsers) return@forEach
                val createdMs = call.createdAt?.let { it.seconds * 1000L } ?: nowMs
                if (nowMs - createdMs > 45_000L) return@forEach
                if (!NotificationHelper.shouldNotifyCall(context, call.id)) return@forEach

                val callerAvatar = NotificationHelper.loadCircularAvatarBitmap(
                    context = context,
                    db = db,
                    url = call.callerPhotoUrl
                )
                NotificationHelper.showIncomingCallNotification(
                    context = context,
                    callId = call.id,
                    callerName = call.callerName.ifBlank { "Incoming Call" },
                    callType = call.callType,
                    playSound = playSound,
                    vibrate = vibrate,
                    callerBitmap = callerAvatar
                )
            }
    }
}

class LimonBackgroundSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            RealtimeBackgroundSyncEngine.runSync(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Worker sync error", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "LimonBgSyncWorker"
        private const val PERIODIC_WORK_NAME = "limon_periodic_realtime_sync"
        private const val IMMEDIATE_WORK_NAME = "limon_immediate_realtime_sync"

        fun schedulePeriodic(context: Context) {
            val appCtx = context.applicationContext
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<LimonBackgroundSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()
            runCatching {
                WorkManager.getInstance(appCtx).enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            }
        }

        fun enqueueImmediateSync(context: Context) {
            val appCtx = context.applicationContext
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<LimonBackgroundSyncWorker>()
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            runCatching {
                WorkManager.getInstance(appCtx).enqueueUniqueWork(
                    IMMEDIATE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
        }
    }
}
