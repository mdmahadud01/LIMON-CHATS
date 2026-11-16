package com.example.data

import android.content.Context
import android.util.Base64
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

data class RtdbMediaPayload(
    val fileName: String,
    val mimeType: String,
    val chunks: List<ByteArray>
)

/**
 * Secondary Firebase Realtime Database connector (`limon-a1919`) for storing and syncing
 * profile photos, chat photos/videos/voice/files, and shared chat wallpapers across users,
 * with bounded timeouts so uploads and reads seamlessly fall back to Firestore if needed.
 */
object LimonRtdbProvider {

    private const val RTDB_APP_NAME = "limon_rtdb"
    private const val RTDB_URL = "https://limon-a1919-default-rtdb.firebaseio.com"
    private const val WRITE_TIMEOUT_MS = 4_500L
    private const val READ_TIMEOUT_MS = 6_500L

    @Volatile
    private var cachedDb: FirebaseDatabase? = null

    fun getDatabase(context: Context): FirebaseDatabase? {
        cachedDb?.let { return it }
        return synchronized(this) {
            cachedDb?.let { return@synchronized it }
            val db = runCatching {
                val appCtx = context.applicationContext
                val existing = FirebaseApp.getApps(appCtx).firstOrNull { it.name == RTDB_APP_NAME }
                val rtdbApp = existing ?: FirebaseApp.initializeApp(
                    appCtx,
                    FirebaseOptions.Builder()
                        .setApiKey("AIzaSyC85uJCaKGca5tzhUOPAqEfdSNdw9SpnXw")
                        .setApplicationId("1:302363928723:web:bde78aa2684eb088a34566")
                        .setDatabaseUrl(RTDB_URL)
                        .setProjectId("limon-a1919")
                        .setStorageBucket("limon-a1919.firebasestorage.app")
                        .setGcmSenderId("302363928723")
                        .build(),
                    RTDB_APP_NAME
                )
                FirebaseDatabase.getInstance(rtdbApp, RTDB_URL)
            }.getOrNull()
            cachedDb = db
            db
        }
    }

    suspend fun saveAvatarBytes(
        context: Context,
        uid: String,
        bytes: ByteArray,
        versionTs: Long
    ): Boolean {
        val db = getDatabase(context) ?: return false
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return withTimeoutOrNull(WRITE_TIMEOUT_MS) {
            runCatching {
                db.reference.child("avatars").child(uid).setValue(
                    mapOf(
                        "uid" to uid,
                        "mimeType" to "image/jpeg",
                        "data" to encoded,
                        "updatedAt" to versionTs
                    )
                ).await()
                true
            }.getOrDefault(false)
        } ?: false
    }

    suspend fun deleteAvatar(context: Context, uid: String): Boolean {
        val db = getDatabase(context) ?: return false
        return withTimeoutOrNull(WRITE_TIMEOUT_MS) {
            runCatching {
                db.reference.child("avatars").child(uid).removeValue().await()
                true
            }.getOrDefault(false)
        } ?: false
    }

    suspend fun readAvatarBytes(context: Context, uid: String): ByteArray? {
        val db = getDatabase(context) ?: return null
        return withTimeoutOrNull(READ_TIMEOUT_MS) {
            runCatching {
                val snap = db.reference.child("avatars").child(uid).child("data").get().await()
                val encoded = snap.getValue(String::class.java) ?: return@runCatching null
                Base64.decode(encoded, Base64.DEFAULT)
            }.getOrNull()
        }
    }

    suspend fun saveSharedWallpaperBytes(
        context: Context,
        chatId: String,
        bytes: ByteArray,
        versionTs: Long
    ): Boolean {
        val db = getDatabase(context) ?: return false
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return withTimeoutOrNull(WRITE_TIMEOUT_MS) {
            runCatching {
                db.reference.child("sharedWallpapers").child(chatId).setValue(
                    mapOf(
                        "chatId" to chatId,
                        "mimeType" to "image/jpeg",
                        "data" to encoded,
                        "updatedAt" to versionTs
                    )
                ).await()
                true
            }.getOrDefault(false)
        } ?: false
    }

    suspend fun readSharedWallpaperBytes(context: Context, chatId: String): ByteArray? {
        val db = getDatabase(context) ?: return null
        return withTimeoutOrNull(READ_TIMEOUT_MS) {
            runCatching {
                val snap = db.reference.child("sharedWallpapers").child(chatId).child("data").get().await()
                val encoded = snap.getValue(String::class.java) ?: return@runCatching null
                Base64.decode(encoded, Base64.DEFAULT)
            }.getOrNull()
        }
    }

    suspend fun saveChatMediaMeta(
        context: Context,
        chatId: String,
        mediaId: String,
        meta: Map<String, Any>
    ): Boolean {
        val db = getDatabase(context) ?: return false
        return withTimeoutOrNull(WRITE_TIMEOUT_MS) {
            runCatching {
                db.reference.child("chatMedia").child(chatId).child(mediaId).child("meta")
                    .setValue(meta).await()
                true
            }.getOrDefault(false)
        } ?: false
    }

    suspend fun saveChatMediaChunk(
        context: Context,
        chatId: String,
        mediaId: String,
        index: Int,
        encodedChunk: String
    ): Boolean {
        val db = getDatabase(context) ?: return false
        return withTimeoutOrNull(WRITE_TIMEOUT_MS) {
            runCatching {
                db.reference.child("chatMedia").child(chatId).child(mediaId)
                    .child("chunks").child(index.toString())
                    .setValue(encodedChunk).await()
                true
            }.getOrDefault(false)
        } ?: false
    }

    suspend fun readChatMedia(
        context: Context,
        chatId: String,
        mediaId: String
    ): RtdbMediaPayload? {
        val db = getDatabase(context) ?: return null
        return withTimeoutOrNull(READ_TIMEOUT_MS) {
            runCatching {
                val mediaSnap = db.reference.child("chatMedia").child(chatId).child(mediaId).get().await()
                if (!mediaSnap.exists()) return@runCatching null
                val metaSnap = mediaSnap.child("meta")
                val fileName = metaSnap.child("fileName").getValue(String::class.java) ?: "$mediaId.bin"
                val mimeType = metaSnap.child("mimeType").getValue(String::class.java) ?: "application/octet-stream"
                val chunksSnap = mediaSnap.child("chunks")
                val ordered = chunksSnap.children
                    .sortedBy { it.key?.toIntOrNull() ?: 0 }
                    .mapNotNull { chunk ->
                        val b64 = chunk.getValue(String::class.java) ?: return@mapNotNull null
                        Base64.decode(b64, Base64.DEFAULT)
                    }
                if (ordered.isEmpty()) return@runCatching null
                RtdbMediaPayload(fileName = fileName, mimeType = mimeType, chunks = ordered)
            }.getOrNull()
        }
    }
}
