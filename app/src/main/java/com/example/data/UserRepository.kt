package com.example.data

import android.content.Context
import android.os.Build
import com.example.R
import com.example.model.BlockedUser
import com.example.model.PrivacyScope
import com.example.model.PrivateUserInfo
import com.example.model.PublicProfile
import com.example.model.UserNotification
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UserRepository(private val db: FirebaseFirestore) {

    private val accountInitMutex = Mutex()

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    fun observeMyProfile(uid: String): Flow<PublicProfile?> {
        val path = "publicProfiles/$uid"
        return db.collection("publicProfiles").document(uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    PublicProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (error !is CancellationException || error.cause != null) {
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(error, OperationType.GET, path)
                    }
                }
                throw error
            }
    }

    fun observePrivateInfo(uid: String): Flow<PrivateUserInfo?> {
        val path = "users/$uid/private/info"
        return db.collection("users").document(uid)
            .collection("private").document("info")
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    PrivateUserInfo::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observeProfile(uid: String): Flow<PublicProfile?> =
        db.collection("publicProfiles").document(uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    PublicProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun getProfileOnce(uid: String): PublicProfile? {
        val path = "publicProfiles/$uid"
        return try {
            db.collection("publicProfiles").document(uid).get().await()
                .toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    suspend fun ensureAccountInitialized(firebaseUser: FirebaseUser): Result<PublicProfile> =
        accountInitMutex.withLock {
            runCatching {
                val uid = firebaseUser.uid
                val profileRef = db.collection("publicProfiles").document(uid)
                val existing = profileRef.get().await()
                val existingProfile = existing.toObject(
                    PublicProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}".take(80)

                val privateRef = db.collection("users").document(uid).collection("private").document("info")
                val privateMap = mapOf(
                    "userId" to uid,
                    "email" to (firebaseUser.email ?: ""),
                    "emailVerified" to firebaseUser.isEmailVerified,
                    "activeSessions" to listOf("$deviceName (Current)"),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                privateRef.set(privateMap, com.google.firebase.firestore.SetOptions.merge()).await()

                if (existingProfile != null) {
                    profileRef.update(
                        mapOf(
                            "online" to true,
                            "lastSeen" to FieldValue.serverTimestamp(),
                            "activeDevice" to deviceName,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    return@runCatching existingProfile.copy(online = true, activeDevice = deviceName)
                }

                val baseHandle = (firebaseUser.email?.substringBefore("@") ?: firebaseUser.displayName ?: "limon")
                    .replace(Regex("[^a-zA-Z0-9_]"), "_")
                    .take(18)
                    .trim('_')
                    .ifEmpty { "user" }
                    .let { if (it.length < 3) "${it}_limon" else it }

                val candidateUsername = "${baseHandle}_${uid.takeLast(4).lowercase()}"
                    .replace(Regex("[^a-zA-Z0-9_]"), "_")
                val lower = candidateUsername.lowercase()

                db.runTransaction { txn ->
                    val unameRef = db.collection("usernames").document(lower)
                    val unameSnap = txn.get(unameRef)
                    val finalUsername = if (!unameSnap.exists()) {
                        candidateUsername
                    } else {
                        "user_${uid.take(8).lowercase()}"
                    }
                    val finalLower = finalUsername.lowercase()
                    val finalUnameRef = db.collection("usernames").document(finalLower)

                    txn.set(
                        finalUnameRef,
                        mapOf(
                            "uid" to uid,
                            "username" to finalUsername,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val displayName = firebaseUser.displayName?.take(60)?.ifBlank { null } ?: finalUsername
                    val photoUrl = firebaseUser.photoUrl?.toString()?.take(500)
                    val profilePayload = mutableMapOf<String, Any>(
                        "uid" to uid,
                        "username" to finalUsername,
                        "usernameLowercase" to finalLower,
                        "displayName" to displayName,
                        "bio" to "Hey there! I am on Limon Chat.",
                        "online" to true,
                        "lastSeen" to FieldValue.serverTimestamp(),
                        "activeDevice" to deviceName,
                        "privacyShowLastSeen" to PrivacyScope.EVERYONE.value,
                        "privacyShowOnline" to PrivacyScope.EVERYONE.value,
                        "privacyShowPhoto" to PrivacyScope.EVERYONE.value,
                        "privacyReadReceipts" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (!photoUrl.isNullOrBlank()) {
                        profilePayload["photoUrl"] = photoUrl
                    }
                    txn.set(profileRef, profilePayload)
                    finalUsername
                }.await()

                profileRef.get().await().toObject(
                    PublicProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: error("Profile initialization failed")
            }.onFailure { e ->
                handleFirestoreError(e, OperationType.WRITE, "publicProfiles/${firebaseUser.uid}")
            }
        }

    suspend fun isUsernameAvailable(username: String, currentUid: String): Boolean {
        val cleaned = username.trim()
        if (!Regex("^[a-zA-Z0-9_]{3,30}$").matches(cleaned)) return false
        val lower = cleaned.lowercase()
        return try {
            val doc = db.collection("usernames").document(lower).get().await()
            !doc.exists() || doc.getString("uid") == currentUid
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "usernames/$lower")
            false
        }
    }

    suspend fun updateProfile(
        displayName: String,
        newUsername: String,
        bio: String,
        photoUrl: String?,
        phoneNumber: String? = null,
        removePhoto: Boolean = false
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cleanDisplay = displayName.trim().take(60)
        val cleanUser = newUsername.trim()
        val cleanBio = bio.trim().take(200)
        require(cleanDisplay.isNotEmpty()) { "Display name cannot be empty" }
        require(Regex("^[a-zA-Z0-9_]{3,30}$").matches(cleanUser)) {
            "Username must be 3-30 characters (letters, numbers, underscores)"
        }
        val newLower = cleanUser.lowercase()

        db.runTransaction { txn ->
            val profileRef = db.collection("publicProfiles").document(uid)
            val profileSnap = txn.get(profileRef)
            val oldLower = profileSnap.getString("usernameLowercase") ?: ""
            val newUnameRef = db.collection("usernames").document(newLower)
            val newUnameSnap = txn.get(newUnameRef)

            if (newLower != oldLower) {
                if (newUnameSnap.exists() && newUnameSnap.getString("uid") != uid) {
                    throw IllegalStateException("Username @$cleanUser is already taken")
                }
                if (oldLower.isNotBlank()) {
                    txn.delete(db.collection("usernames").document(oldLower))
                }
                txn.set(
                    newUnameRef,
                    mapOf(
                        "uid" to uid,
                        "username" to cleanUser,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val updates = mutableMapOf<String, Any>(
                "username" to cleanUser,
                "usernameLowercase" to newLower,
                "displayName" to cleanDisplay,
                "bio" to cleanBio,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (removePhoto) {
                updates["photoUrl"] = FieldValue.delete()
            } else if (!photoUrl.isNullOrBlank()) {
                updates["photoUrl"] = photoUrl.take(500)
            }
            txn.update(profileRef, updates)
        }.await()

        if (phoneNumber != null) {
            db.collection("users").document(uid).collection("private").document("info")
                .set(
                    mapOf(
                        "userId" to uid,
                        "phoneNumber" to phoneNumber.trim().take(24),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
        }
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.UPDATE, "publicProfiles")
    }

    suspend fun updatePhotoUrlImmediate(
        photoUrl: String?,
        removePhoto: Boolean = false
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val updates = mutableMapOf<String, Any>(
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (removePhoto || photoUrl.isNullOrBlank()) {
            updates["photoUrl"] = FieldValue.delete()
        } else {
            updates["photoUrl"] = photoUrl.take(500)
        }
        db.collection("publicProfiles").document(uid).update(updates).await()
        Unit
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.UPDATE, "publicProfiles")
    }

    suspend fun updatePrivacySettings(
        showLastSeen: PrivacyScope,
        showOnline: PrivacyScope,
        showPhoto: PrivacyScope,
        readReceipts: Boolean
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        db.collection("publicProfiles").document(uid).update(
            mapOf(
                "privacyShowLastSeen" to showLastSeen.value,
                "privacyShowOnline" to showOnline.value,
                "privacyShowPhoto" to showPhoto.value,
                "privacyReadReceipts" to readReceipts,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.UPDATE, "publicProfiles")
    }

    suspend fun setOnlinePresence(online: Boolean) {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}".take(80)
        val profileRef = db.collection("publicProfiles").document(uid)
        try {
            val shouldCreateIfMissing = accountInitMutex.withLock {
                val snap = profileRef.get().await()
                if (snap.exists()) {
                    profileRef.update(
                        mapOf(
                            "online" to online,
                            "lastSeen" to FieldValue.serverTimestamp(),
                            "activeDevice" to deviceName,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    false
                } else {
                    online
                }
            }
            if (shouldCreateIfMissing) {
                ensureAccountInitialized(currentUser)
            }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                handleFirestoreError(e, OperationType.UPDATE, "publicProfiles/$uid")
            }
        }
    }

    suspend fun saveFcmToken(token: String, deviceId: String = "primary") {
        val uid = auth.currentUser?.uid ?: return
        val cleanDeviceId = deviceId.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40).ifBlank { "primary" }
        try {
            db.collection("users").document(uid).collection("private").document("info")
                .set(
                    mapOf(
                        "userId" to uid,
                        "fcmToken" to token,
                        "fcmTokens" to mapOf(cleanDeviceId to token),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/private/info")
        }
    }

    suspend fun clearDeviceFcmToken(deviceId: String) {
        val uid = auth.currentUser?.uid ?: return
        val cleanDeviceId = deviceId.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40).ifBlank { "primary" }
        try {
            db.collection("users").document(uid).collection("private").document("info")
                .update(
                    mapOf(
                        "fcmTokens.$cleanDeviceId" to FieldValue.delete(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        } catch (_: Exception) {
        }
    }

    suspend fun searchUsers(query: String): Result<List<PublicProfile>> = runCatching {
        val myUid = requireUserId()
        val trimmed = query.trim()
        val cleanHandle = trimmed.removePrefix("@").lowercase()

        if (cleanHandle.isEmpty()) {
            return@runCatching db.collection("publicProfiles")
                .limit(25)
                .get()
                .await()
                .toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .filter { it.uid != myUid }
        }

        val results = LinkedHashMap<String, PublicProfile>()

        if (trimmed.length >= 8 && !trimmed.startsWith("@")) {
            val exactDoc = db.collection("publicProfiles").document(trimmed).get().await()
            exactDoc.toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.let {
                if (it.uid != myUid) results[it.uid] = it
            }
        }

        val usernameMatches = db.collection("publicProfiles")
            .orderBy("usernameLowercase")
            .startAt(cleanHandle)
            .endAt(cleanHandle + "\uf8ff")
            .limit(20)
            .get()
            .await()
            .toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        usernameMatches.forEach { if (it.uid != myUid) results[it.uid] = it }

        if (results.size < 10) {
            val directory = db.collection("publicProfiles")
                .limit(40)
                .get()
                .await()
                .toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            directory.forEach { profile ->
                if (profile.uid != myUid &&
                    (profile.displayName.contains(trimmed, ignoreCase = true) ||
                        profile.usernameLowercase.contains(cleanHandle))
                ) {
                    results[profile.uid] = profile
                }
            }
        }
        results.values.toList()
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.LIST, "publicProfiles")
    }

    fun observeBlockedUsers(uid: String): Flow<List<BlockedUser>> {
        val path = "users/$uid/blocks"
        return db.collection("users").document(uid).collection("blocks")
            .snapshots()
            .map { it.toObjects(BlockedUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun blockUser(target: PublicProfile): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(target.uid != uid) { "Cannot block yourself" }
        val payload = mapOf(
            "blockedUid" to target.uid,
            "blockedDisplayName" to target.displayName.ifBlank { target.username },
            "blockedUsername" to target.username,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).collection("blocks").document(target.uid)
            .set(payload).await()
        Unit
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.CREATE, "users/blocks")
    }

    suspend fun unblockUser(blockedUid: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        db.collection("users").document(uid).collection("blocks").document(blockedUid)
            .delete().await()
        Unit
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.DELETE, "users/blocks/$blockedUid")
    }

    suspend fun reportUser(targetUid: String, reason: String, details: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val id = UUID.randomUUID().toString()
        val payload = mutableMapOf<String, Any>(
            "reporterId" to uid,
            "reportedUid" to targetUid,
            "reason" to reason.take(100),
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (details.isNotBlank()) {
            payload["details"] = details.trim().take(1000)
        }
        db.collection("reports").document(id).set(payload).await()
        Unit
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.CREATE, "reports")
    }

    fun observeNotifications(uid: String): Flow<List<UserNotification>> {
        val path = "users/$uid/notifications"
        return db.collection("users").document(uid).collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { it.toObjects(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun markNotificationRead(notificationId: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            db.collection("users").document(uid).collection("notifications").document(notificationId)
                .update("read", true).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid/notifications/$notificationId")
        }
    }

    suspend fun sendInAppNotification(
        recipientUid: String,
        type: String,
        senderName: String,
        title: String,
        body: String,
        chatId: String? = null
    ) {
        val senderUid = auth.currentUser?.uid ?: return
        if (recipientUid == senderUid) return
        val id = UUID.randomUUID().toString()
        val payload = mutableMapOf<String, Any>(
            "type" to type,
            "senderId" to senderUid,
            "senderName" to senderName.take(80).ifBlank { "Limon User" },
            "title" to title.take(120),
            "body" to body.take(500),
            "read" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (!chatId.isNullOrBlank()) {
            payload["chatId"] = chatId
        }
        try {
            db.collection("users").document(recipientUid).collection("notifications").document(id)
                .set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$recipientUid/notifications/$id")
        }
    }

    suspend fun deleteAccountData(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val profile = getProfileOnce(uid)
        if (profile != null && profile.usernameLowercase.isNotBlank()) {
            runCatching {
                db.collection("usernames").document(profile.usernameLowercase).delete().await()
            }
        }
        runCatching {
            db.collection("publicAvatars").document(uid).delete().await()
        }
        runCatching {
            db.collection("users").document(uid).collection("private").document("info").delete().await()
        }
        db.collection("publicProfiles").document(uid).delete().await()
        Unit
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.DELETE, "publicProfiles")
    }
}
