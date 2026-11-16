package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.example.R
import com.example.data.CallRepository
import com.example.data.ChatRepository
import com.example.data.UserRepository
import com.example.model.CallStatus
import com.example.model.MessageType
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val db = FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uid = Firebase.auth.currentUser?.uid
                when (intent.action) {
                    NotificationHelper.ACTION_INLINE_REPLY -> {
                        val replyText = RemoteInput.getResultsFromIntent(intent)
                            ?.getCharSequence(NotificationHelper.KEY_TEXT_REPLY)
                            ?.toString()
                            ?.trim()
                        val chatId = intent.getStringExtra(NotificationHelper.EXTRA_CHAT_ID)
                        val peerUid = intent.getStringExtra(NotificationHelper.EXTRA_PEER_UID)
                        val senderName = intent.getStringExtra(NotificationHelper.EXTRA_SENDER_NAME) ?: "Limon Chat"
                        val notifId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 0)

                        if (!replyText.isNullOrEmpty() && !chatId.isNullOrBlank() && !peerUid.isNullOrBlank() && uid != null) {
                            val chatRepo = ChatRepository(db)
                            val userRepo = UserRepository(db)
                            val myProfile = userRepo.getProfileOnce(uid)
                            val sendRes = chatRepo.sendMessage(
                                chatId = chatId,
                                peerUid = peerUid,
                                type = MessageType.TEXT,
                                text = replyText
                            )
                            chatRepo.markLatestMessagesRead(
                                chatId = chatId,
                                uid = uid,
                                sendReadReceipts = myProfile?.privacyReadReceipts ?: true
                            )
                            if (sendRes.isSuccess) {
                                val myName = myProfile?.displayName ?: "Limon User"
                                userRepo.sendInAppNotification(
                                    recipientUid = peerUid,
                                    type = "message",
                                    senderName = myName,
                                    title = myName,
                                    body = replyText.take(120),
                                    chatId = chatId
                                )
                                NotificationHelper.showMessageNotification(
                                    context = appContext,
                                    chatId = chatId,
                                    peerUid = peerUid,
                                    senderName = senderName,
                                    previewText = replyText,
                                    showPreview = true,
                                    playSound = false,
                                    vibrate = false,
                                    isOwnReply = true
                                )
                            } else if (notifId != 0) {
                                NotificationHelper.cancelNotification(appContext, notifId)
                            }
                        } else if (notifId != 0) {
                            NotificationHelper.cancelNotification(appContext, notifId)
                        }
                    }

                    NotificationHelper.ACTION_MARK_READ -> {
                        val chatId = intent.getStringExtra(NotificationHelper.EXTRA_CHAT_ID)
                        val notifId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 0)
                        if (!chatId.isNullOrBlank() && uid != null) {
                            val myProfile = UserRepository(db).getProfileOnce(uid)
                            ChatRepository(db).markLatestMessagesRead(
                                chatId = chatId,
                                uid = uid,
                                sendReadReceipts = myProfile?.privacyReadReceipts ?: true
                            )
                            NotificationHelper.clearConversationHistory(chatId)
                        }
                        if (notifId != 0) NotificationHelper.cancelNotification(appContext, notifId)
                    }

                    NotificationHelper.ACTION_DECLINE_CALL -> {
                        val callId = intent.getStringExtra(NotificationHelper.EXTRA_CALL_ID)
                        val notifId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 0)
                        if (!callId.isNullOrBlank() && uid != null) {
                            CallRepository(db).endCall(callId, CallStatus.DECLINED, 0L)
                        }
                        CallForegroundService.stop(appContext)
                        if (notifId != 0) NotificationHelper.cancelNotification(appContext, notifId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
