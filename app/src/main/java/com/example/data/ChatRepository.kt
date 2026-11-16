package com.example.data

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.media.LimonMediaResolver
import com.example.media.LimonMediaUploader
import com.example.model.ChatMessage
import com.example.model.ChatPreference
import com.example.model.ChatThread
import com.example.model.ChatWallpaperPreset
import com.example.model.DeliveryStatus
import com.example.model.MessageType
import com.example.model.PublicProfile
import com.example.model.TypingStatus
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

sealed interface UploadProgressState {
    data class InProgress(val bytesTransferred: Long, val totalBytes: Long, val progress: Float) : UploadProgressState
    data class Completed(val downloadUrl: String, val sizeBytes: Long) : UploadProgressState
    data class Failed(val message: String) : UploadProgressState
}

class ChatRepository(
    private val db: FirebaseFirestore,
    private val appContext: Context? = null
) {

    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        appContext = context.applicationContext
    )

    private val auth = Firebase.auth
    private val activeUploads = mutableMapOf<String, StorageTask<UploadTask.TaskSnapshot>>()

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    fun deterministicChatId(uidA: String, uidB: String): String =
        listOf(uidA, uidB).sorted().joinToString("_")

    fun observeChats(uid: String): Flow<List<ChatThread>> {
        val path = "chats"
        return db.collection("chats")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChatThread::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.lastMessageAt ?: it.updatedAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeChatPreferences(uid: String): Flow<Map<String, ChatPreference>> {
        val path = "users/$uid/chatPreferences"
        return db.collection("users").document(uid).collection("chatPreferences")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChatPreference::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).associateBy { it.chatId.ifBlank { it.id } }
            }
            .catch { error ->
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeChat(chatId: String): Flow<ChatThread?> {
        val path = "chats/$chatId"
        return db.collection("chats").document(chatId)
            .snapshots()
            .map { snap ->
                snap.toObject(ChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    fun observeMessages(chatId: String, uid: String, limitCount: Long): Flow<List<ChatMessage>> {
        val path = "chats/$chatId/messages"
        return db.collection("chats").document(chatId).collection("messages")
            .whereArrayContains("memberUids", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limitCount)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                    .filter { uid !in it.deletedForUsers }
                    .sortedWith(
                        compareBy<ChatMessage> { it.createdAt?.seconds ?: Long.MAX_VALUE }
                            .thenBy { it.createdAt?.nanoseconds ?: Int.MAX_VALUE }
                            .thenBy { it.id }
                    )
            }
            .catch { error ->
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observePeerTyping(chatId: String, myUid: String): Flow<Boolean> {
        val path = "chats/$chatId/typing"
        return db.collection("chats").document(chatId).collection("typing")
            .whereArrayContains("memberUids", myUid)
            .snapshots()
            .map { snapshot ->
                val nowSeconds = Timestamp.now().seconds
                snapshot.toObjects(
                    TypingStatus::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).any { status ->
                    status.uid != myUid &&
                        status.isTyping &&
                        (nowSeconds - (status.updatedAt?.seconds ?: 0L)) <= 8L
                }
            }
            .catch { error ->
                if (auth.currentUser?.uid == myUid) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                emit(false)
            }
    }

    suspend fun getOrCreateChat(peerUid: String): Result<String> = runCatching {
        val myUid = requireUserId()
        require(peerUid.isNotBlank() && peerUid != myUid) { "Invalid recipient" }
        val chatId = deterministicChatId(myUid, peerUid)
        val chatRef = db.collection("chats").document(chatId)
        val snap = runCatching { chatRef.get().await() }.getOrNull()
        if (snap == null || !snap.exists()) {
            val payload = mapOf(
                "memberUids" to listOf(myUid, peerUid).sorted(),
                "lastMessageText" to "",
                "lastMessageType" to MessageType.TEXT.value,
                "lastMessageSenderId" to myUid,
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "unreadBy" to mapOf(myUid to 0L, peerUid to 0L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            chatRef.set(payload).await()
        }
        chatId
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.CREATE, "chats")
    }

    suspend fun sendMessage(
        chatId: String,
        peerUid: String,
        type: MessageType,
        text: String,
        mediaUrl: String? = null,
        mediaName: String? = null,
        mediaSizeBytes: Long? = null,
        mediaDurationMs: Long? = null,
        waveform: List<Double>? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null,
        contact: PublicProfile? = null,
        replyTo: ChatMessage? = null,
        replyToSenderName: String? = null,
        forwardedFrom: String? = null,
        messageIdOverride: String? = null,
        isViewOnce: Boolean = false
    ): Result<String> = runCatching {
        val myUid = requireUserId()
        val members = listOf(myUid, peerUid).sorted()
        val chatRef = db.collection("chats").document(chatId)
        val chatSnap = runCatching { chatRef.get().await() }.getOrNull()

        if (chatSnap == null || !chatSnap.exists()) {
            val initChat = mapOf(
                "memberUids" to members,
                "lastMessageText" to "",
                "lastMessageType" to MessageType.TEXT.value,
                "lastMessageSenderId" to myUid,
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "unreadBy" to mapOf(myUid to 0L, peerUid to 0L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            chatRef.set(initChat).await()
        }

        val messageId = messageIdOverride?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val msgRef = chatRef.collection("messages").document(messageId)
        val cleanText = text.trim().take(4000)

        val payload = mutableMapOf<String, Any>(
            "chatId" to chatId,
            "senderId" to myUid,
            "memberUids" to members,
            "type" to type.value,
            "text" to cleanText,
            "deliveryStatus" to DeliveryStatus.SENT.value,
            "deletedForEveryone" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!mediaUrl.isNullOrBlank()) payload["mediaUrl"] = mediaUrl.take(1000)
        if (!mediaName.isNullOrBlank()) payload["mediaName"] = mediaName.take(200)
        if (mediaSizeBytes != null) payload["mediaSizeBytes"] = mediaSizeBytes
        if (mediaDurationMs != null) payload["mediaDurationMs"] = mediaDurationMs
        if (!waveform.isNullOrEmpty()) payload["waveform"] = waveform.take(100)
        if (latitude != null) payload["latitude"] = latitude
        if (longitude != null) payload["longitude"] = longitude
        if (!locationName.isNullOrBlank()) payload["locationName"] = locationName.trim().take(200)
        if (contact != null) {
            payload["contactName"] = contact.displayName.take(80)
            payload["contactUsername"] = contact.username.take(40)
            payload["contactUid"] = contact.uid.take(128)
        }
        if (replyTo != null) {
            payload["replyToId"] = replyTo.id
            payload["replyToText"] = replyTo.text.ifBlank { replyTo.type.uppercase() }.take(300)
            payload["replyToSenderName"] = (replyToSenderName ?: "Message").take(80)
        }
        if (!forwardedFrom.isNullOrBlank()) {
            payload["forwardedFrom"] = forwardedFrom.take(80)
        }
        val effectiveViewOnce = isViewOnce && (type == MessageType.IMAGE || type == MessageType.VIDEO)
        if (effectiveViewOnce) {
            payload["isViewOnce"] = true
            payload["viewOnceViewed"] = false
        }

        val preview = when (type) {
            MessageType.TEXT -> cleanText.take(500)
            MessageType.IMAGE -> when {
                effectiveViewOnce -> if (cleanText.isNotEmpty()) "View once photo: $cleanText".take(500) else "View once photo"
                cleanText.isNotEmpty() -> "Photo: $cleanText".take(500)
                else -> "Photo"
            }
            MessageType.VIDEO -> when {
                effectiveViewOnce -> if (cleanText.isNotEmpty()) "View once video: $cleanText".take(500) else "View once video"
                cleanText.isNotEmpty() -> "Video: $cleanText".take(500)
                else -> "Video"
            }
            MessageType.VOICE -> "Voice message"
            MessageType.DOCUMENT -> "File: ${mediaName ?: "Document"}".take(500)
            MessageType.LOCATION -> locationName?.takeIf { it.isNotBlank() }?.let { "Location: $it" }?.take(500) ?: "Shared Location"
            MessageType.CONTACT -> "Contact: ${contact?.displayName ?: "User"}".take(500)
        }

        val existingUnread = (chatSnap?.get("unreadBy") as? Map<*, *>)
            ?.get(peerUid)?.let { (it as? Number)?.toLong() ?: 0L } ?: 0L

        db.runBatch { batch ->
            batch.set(msgRef, payload)
            batch.update(
                chatRef,
                mapOf(
                    "lastMessageText" to preview,
                    "lastMessageType" to type.value,
                    "lastMessageSenderId" to myUid,
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "unreadBy.$peerUid" to (existingUnread + 1L),
                    "unreadBy.$myUid" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()

        messageId
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.CREATE, "chats/$chatId/messages")
    }

    suspend fun acknowledgeDeliveredAndRead(
        chatId: String,
        messages: List<ChatMessage>,
        markRead: Boolean,
        sendReadReceipts: Boolean
    ) {
        val myUid = auth.currentUser?.uid ?: return
        val toUpdate = messages.filter { msg ->
            msg.senderId != myUid &&
                ((markRead && sendReadReceipts && msg.deliveryStatus != DeliveryStatus.READ.value) ||
                    (!markRead && msg.deliveryStatus == DeliveryStatus.SENT.value))
        }
        if (toUpdate.isNotEmpty()) {
            try {
                db.runBatch { batch ->
                    toUpdate.take(40).forEach { msg ->
                        val ref = db.collection("chats").document(chatId).collection("messages").document(msg.id)
                        val newStatus = if (markRead && sendReadReceipts) DeliveryStatus.READ.value else DeliveryStatus.DELIVERED.value
                        batch.update(ref, "deliveryStatus", newStatus)
                    }
                }.await()
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages")
                }
            }
        }
        if (markRead) {
            try {
                db.collection("chats").document(chatId)
                    .update("unreadBy.$myUid", 0L).await()
                setChatPreference(chatId = chatId, lastReadAtNow = true)
            } catch (_: Exception) {
            }
        }
    }

    suspend fun acknowledgeChatDelivered(chatId: String, uid: String, messageId: String? = null) {
        if (auth.currentUser?.uid != uid) return
        try {
            if (!messageId.isNullOrBlank()) {
                val msgRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
                val snap = msgRef.get().await()
                val sender = snap.getString("senderId")
                val status = snap.getString("deliveryStatus")
                if (sender != null && sender != uid && status == DeliveryStatus.SENT.value) {
                    msgRef.update("deliveryStatus", DeliveryStatus.DELIVERED.value).await()
                }
                return
            }
            val recent = db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("memberUids", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(25)
                .get()
                .await()
                .toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            acknowledgeDeliveredAndRead(
                chatId = chatId,
                messages = recent,
                markRead = false,
                sendReadReceipts = false
            )
        } catch (_: Exception) {
        }
    }

    suspend fun markLatestMessagesRead(chatId: String, uid: String, sendReadReceipts: Boolean = true) {
        if (auth.currentUser?.uid != uid) return
        try {
            val recent = db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("memberUids", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .await()
                .toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            acknowledgeDeliveredAndRead(
                chatId = chatId,
                messages = recent,
                markRead = true,
                sendReadReceipts = sendReadReceipts
            )
        } catch (e: Exception) {
            if (e !is CancellationException) {
                handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages")
            }
        }
    }

    suspend fun setTyping(chatId: String, peerUid: String, isTyping: Boolean) {
        val myUid = auth.currentUser?.uid ?: return
        val members = listOf(myUid, peerUid).sorted()
        val ref = db.collection("chats").document(chatId).collection("typing").document(myUid)
        try {
            ref.set(
                mapOf(
                    "uid" to myUid,
                    "memberUids" to members,
                    "isTyping" to isTyping,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
        }
    }

    suspend fun editMessage(chatId: String, messageId: String, newText: String): Result<Unit> = runCatching {
        requireUserId()
        val cleaned = newText.trim().take(4000)
        require(cleaned.isNotEmpty()) { "Message cannot be empty" }
        db.collection("chats").document(chatId).collection("messages").document(messageId)
            .update(
                mapOf(
                    "text" to cleaned,
                    "editedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/$messageId")
    }

    suspend fun deleteMessageForEveryone(chatId: String, messageId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("chats").document(chatId).collection("messages").document(messageId)
            .update(
                mapOf(
                    "deletedForEveryone" to true,
                    "text" to "This message was deleted"
                )
            ).await()
        Unit
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/$messageId")
    }

    suspend fun deleteMessageForMe(chatId: String, messageId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        db.collection("chats").document(chatId).collection("messages").document(messageId)
            .update("deletedForUsers", FieldValue.arrayUnion(uid)).await()
        Unit
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/$messageId")
    }

    suspend fun toggleReaction(chatId: String, message: ChatMessage, emoji: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val current = message.reactions[uid]
        val ref = db.collection("chats").document(chatId).collection("messages").document(message.id)
        if (current == emoji) {
            ref.update("reactions.$uid", FieldValue.delete()).await()
        } else {
            ref.update("reactions.$uid", emoji).await()
        }
        Unit
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/${message.id}")
    }

    suspend fun markViewOnceViewed(chatId: String, messageId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("chats").document(chatId).collection("messages").document(messageId)
            .update("viewOnceViewed", true).await()
        Unit
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/$messageId")
    }

    suspend fun pinMessage(chatId: String, message: ChatMessage?): Result<Unit> = runCatching {
        requireUserId()
        val updates = if (message == null) {
            mapOf(
                "pinnedMessageId" to FieldValue.delete(),
                "pinnedMessageText" to FieldValue.delete(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        } else {
            mapOf(
                "pinnedMessageId" to message.id,
                "pinnedMessageText" to message.text.ifBlank { message.type.uppercase() }.take(300),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }
        db.collection("chats").document(chatId).update(updates).await()
        Unit
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId")
    }

    suspend fun setSharedWallpaper(
        chatId: String,
        preset: ChatWallpaperPreset?,
        customUrl: String?,
        blur: Boolean?
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (preset == null) {
            updates["sharedWallpaperPreset"] = FieldValue.delete()
            updates["sharedWallpaperUrl"] = FieldValue.delete()
            updates["sharedWallpaperBlur"] = FieldValue.delete()
            updates["sharedWallpaperBy"] = FieldValue.delete()
        } else {
            updates["sharedWallpaperPreset"] = preset.name
            if (!customUrl.isNullOrBlank()) {
                updates["sharedWallpaperUrl"] = customUrl.take(1000)
            } else {
                updates["sharedWallpaperUrl"] = FieldValue.delete()
            }
            updates["sharedWallpaperBlur"] = blur ?: false
            updates["sharedWallpaperBy"] = uid
        }
        db.collection("chats").document(chatId).update(updates).await()
        Unit
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId")
    }

    suspend fun setChatPreference(
        chatId: String,
        muted: Boolean? = null,
        archived: Boolean? = null,
        pinned: Boolean? = null,
        locked: Boolean? = null,
        lastReadAtNow: Boolean = false,
        clearHistoryNow: Boolean = false
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("chatPreferences").document(chatId)
        val snap = runCatching { ref.get().await() }.getOrNull()
        val current = snap?.toObject(ChatPreference::class.java) ?: ChatPreference(chatId = chatId)
        val payload = mutableMapOf<String, Any>(
            "chatId" to chatId,
            "muted" to (muted ?: current.muted),
            "archived" to (archived ?: current.archived),
            "pinned" to (pinned ?: current.pinned),
            "locked" to (locked ?: current.locked),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (lastReadAtNow) payload["lastReadAt"] = FieldValue.serverTimestamp()
        if (clearHistoryNow) payload["clearedAt"] = FieldValue.serverTimestamp()
        ref.set(payload, SetOptions.merge()).await()
        Unit
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.WRITE, "users/chatPreferences/$chatId")
    }

    suspend fun clearChatLockAndWipeChats(chatIds: List<String>): Result<Unit> = runCatching {
        requireUserId()
        chatIds.distinct().forEach { chatId ->
            setChatPreference(
                chatId = chatId,
                locked = false,
                clearHistoryNow = true
            ).getOrThrow()
        }
        Unit
    }

    suspend fun deleteConversation(chatId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        runCatching {
            db.collection("users").document(uid).collection("chatPreferences").document(chatId).delete().await()
        }
        db.collection("chats").document(chatId).delete().await()
        Unit
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.DELETE, "chats/$chatId")
    }

    suspend fun markChatAsRead(chatId: String, sendReadReceipts: Boolean = true): Result<Unit> = runCatching {
        val uid = requireUserId()
        markLatestMessagesRead(chatId = chatId, uid = uid, sendReadReceipts = sendReadReceipts)
        Unit
    }

    fun uploadMediaWithProgress(
        uploadId: String,
        chatId: String,
        folder: String,
        fileName: String,
        uri: Uri?,
        localFile: File?
    ): Flow<UploadProgressState> {
        val ctx = appContext
        if (ctx != null) {
            return LimonMediaUploader.uploadChatMediaWithProgress(
                context = ctx,
                db = db,
                uploadId = uploadId,
                chatId = chatId,
                folder = folder,
                fileName = fileName,
                uri = uri,
                localFile = localFile
            )
        }
        return callbackFlow {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                trySend(UploadProgressState.Failed("Sign in required to upload media"))
                close()
                return@callbackFlow
            }
            val storageRef = Firebase.storage.reference
                .child("chats/$chatId/$folder/${UUID.randomUUID()}_$fileName")

            val task = when {
                localFile != null && localFile.exists() -> storageRef.putFile(Uri.fromFile(localFile))
                uri != null -> storageRef.putFile(uri)
                else -> {
                    trySend(UploadProgressState.Failed("No media file selected"))
                    close()
                    return@callbackFlow
                }
            }
            activeUploads[uploadId] = task

            task.addOnProgressListener { snap ->
                val total = snap.totalByteCount.coerceAtLeast(1L)
                val ratio = (snap.bytesTransferred.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                trySend(UploadProgressState.InProgress(snap.bytesTransferred, total, ratio))
            }.addOnSuccessListener { snap ->
                storageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        activeUploads.remove(uploadId)
                        trySend(
                            UploadProgressState.Completed(
                                downloadUrl = downloadUri.toString(),
                                sizeBytes = snap.totalByteCount
                            )
                        )
                        close()
                    }
                    .addOnFailureListener { e ->
                        activeUploads.remove(uploadId)
                        trySend(UploadProgressState.Failed(e.localizedMessage ?: "Failed to resolve media URL"))
                        close()
                    }
            }.addOnFailureListener { e ->
                activeUploads.remove(uploadId)
                trySend(
                    UploadProgressState.Failed(
                        e.localizedMessage ?: "Media upload failed"
                    )
                )
                close()
            }

            awaitClose {
                activeUploads.remove(uploadId)
            }
        }
    }

    fun cancelUpload(uploadId: String) {
        LimonMediaUploader.cancelUpload(uploadId)
        activeUploads.remove(uploadId)?.cancel()
    }

    suspend fun resolveMediaToLocalFile(
        context: Context,
        url: String,
        fallbackName: String? = null
    ): File? {
        return LimonMediaResolver.resolveToLocalFile(context, db, url, fallbackName)
    }
}
