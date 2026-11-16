package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.webkit.MimeTypeMap
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.imageLoader
import coil.request.Options
import com.example.data.LimonRtdbProvider
import com.example.data.UploadProgressState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object LimonMediaUploader {

    private const val CHUNK_RAW_BYTES = 420_000
    private const val MAX_CHUNKS = 80
    private const val MAX_CHUNKED_SIZE_BYTES = 26_214_400L

    private val cancelledIds = ConcurrentHashMap.newKeySet<String>()

    fun cancelUpload(uploadId: String) {
        cancelledIds.add(uploadId)
    }

    fun uploadChatMediaWithProgress(
        context: Context,
        db: FirebaseFirestore,
        uploadId: String,
        chatId: String,
        folder: String,
        fileName: String,
        uri: Uri?,
        localFile: File?
    ): Flow<UploadProgressState> = callbackFlow {
        val uid = Firebase.auth.currentUser?.uid
        if (uid == null) {
            trySend(UploadProgressState.Failed("Sign in required to upload media"))
            close()
            return@callbackFlow
        }
        cancelledIds.remove(uploadId)

        val job = launch(Dispatchers.IO) {
            try {
                trySend(UploadProgressState.InProgress(0L, 1L, 0.04f))

                val stagedFile = stageUploadFile(
                    context = context,
                    folder = folder,
                    fileName = fileName,
                    uri = uri,
                    localFile = localFile
                )
                if (stagedFile == null || !stagedFile.exists() || stagedFile.length() <= 0L) {
                    trySend(UploadProgressState.Failed("Could not read selected media file"))
                    close()
                    return@launch
                }

                if (cancelledIds.contains(uploadId)) {
                    trySend(UploadProgressState.Failed("Upload cancelled"))
                    close()
                    return@launch
                }

                val sizeBytes = stagedFile.length().coerceAtLeast(1L)
                require(sizeBytes <= MAX_CHUNKED_SIZE_BYTES) {
                    "File exceeds 25 MB direct transfer limit"
                }

                val mediaId = "m_${UUID.randomUUID().toString().replace("-", "").take(20)}"
                cacheMediaLocally(context, mediaId, fileName, stagedFile)

                trySend(UploadProgressState.InProgress(0L, sizeBytes, 0.10f))

                // 1. Primary: upload to Realtime Database (limon-a1919) so all users can load photos/videos/voice
                val rtdbUrl = uploadChunkedToRtdb(
                    context = context,
                    uid = uid,
                    chatId = chatId,
                    mediaId = mediaId,
                    folder = folder,
                    fileName = fileName,
                    file = stagedFile,
                    uploadId = uploadId
                ) { transferred, total ->
                    val ratio = 0.10f + 0.86f * (transferred.toFloat() / total.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                    trySend(UploadProgressState.InProgress(transferred, total, ratio))
                }

                if (cancelledIds.contains(uploadId)) {
                    trySend(UploadProgressState.Failed("Upload cancelled"))
                    close()
                    return@launch
                }

                if (!rtdbUrl.isNullOrBlank()) {
                    trySend(UploadProgressState.Completed(rtdbUrl, sizeBytes))
                    close()
                    return@launch
                }

                // 2. Fallback: upload chunked media into Firestore chats/{chatId}/media/{mediaId}
                val chunkedUrl = uploadChunkedToFirestore(
                    db = db,
                    uid = uid,
                    chatId = chatId,
                    mediaId = mediaId,
                    folder = folder,
                    fileName = fileName,
                    file = stagedFile,
                    uploadId = uploadId
                ) { transferred, total ->
                    val ratio = 0.12f + 0.86f * (transferred.toFloat() / total.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                    trySend(UploadProgressState.InProgress(transferred, total, ratio))
                }

                trySend(UploadProgressState.Completed(chunkedUrl, sizeBytes))
                close()
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                trySend(UploadProgressState.Failed(e.localizedMessage ?: "Media upload failed"))
                close()
            } finally {
                cancelledIds.remove(uploadId)
            }
        }

        awaitClose {
            cancelledIds.add(uploadId)
            job.cancel()
        }
    }

    suspend fun uploadAvatar(
        context: Context,
        db: FirebaseFirestore,
        uid: String,
        uri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = decodeAndCompressImage(context, uri, maxDimension = 360, quality = 82)
                ?: error("Could not read selected profile photo")
            val versionTs = System.currentTimeMillis()

            // Immediately write to local cache so UI updates without waiting
            val avatarDir = File(context.cacheDir, "avatar_cache").apply { mkdirs() }
            File(avatarDir, "$uid.jpg").writeBytes(bytes)
            File(avatarDir, "$uid.ver").writeText(versionTs.toString())

            // Save to Realtime Database (limon-a1919) so other users can view profile photos
            val rtdbSaved = LimonRtdbProvider.saveAvatarBytes(context, uid, bytes, versionTs)

            // Also mirror to Firestore publicAvatars/{uid} as resilient cross-user backup
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (encoded.length <= 740_000) {
                runCatching {
                    db.collection("publicAvatars").document(uid).set(
                        mapOf(
                            "uid" to uid,
                            "mimeType" to "image/jpeg",
                            "data" to encoded,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            if (rtdbSaved) "rtdb://avatars/$uid?v=$versionTs" else "limonavatar://$uid?v=$versionTs"
        }
    }

    suspend fun deleteAvatar(
        context: Context,
        db: FirebaseFirestore,
        uid: String
    ) = withContext(Dispatchers.IO) {
        clearAvatarCache(context, uid)
        runCatching { LimonRtdbProvider.deleteAvatar(context, uid) }
        runCatching { db.collection("publicAvatars").document(uid).delete().await() }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun clearAvatarCache(context: Context, uid: String) {
        val avatarDir = File(context.cacheDir, "avatar_cache")
        if (avatarDir.exists()) {
            avatarDir.listFiles()?.forEach { file ->
                if (file.name.startsWith(uid)) {
                    file.delete()
                }
            }
        }
        runCatching {
            val loader = context.imageLoader
            loader.memoryCache?.clear()
        }
    }

    suspend fun uploadSharedWallpaper(
        context: Context,
        db: FirebaseFirestore,
        chatId: String,
        uri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = Firebase.auth.currentUser?.uid ?: error("Sign in required")
            val bytes = decodeAndCompressImage(context, uri, maxDimension = 1080, quality = 78)
                ?: error("Could not read selected wallpaper image")
            val versionTs = System.currentTimeMillis()

            val wallDir = File(context.cacheDir, "wallpaper_cache").apply { mkdirs() }
            File(wallDir, "$chatId.jpg").writeBytes(bytes)
            File(wallDir, "$chatId.ver").writeText(versionTs.toString())

            val rtdbSaved = LimonRtdbProvider.saveSharedWallpaperBytes(context, chatId, bytes, versionTs)
            if (rtdbSaved) {
                return@runCatching "rtdb://wallpapers/$chatId?v=$versionTs"
            }

            // Fallback: Firestore chunked media inside chats/{chatId}/media
            val mediaId = "wp_${versionTs}"
            val tmpFile = File(context.cacheDir, "${mediaId}.jpg").apply { writeBytes(bytes) }
            try {
                uploadChunkedToFirestore(
                    db = db,
                    uid = uid,
                    chatId = chatId,
                    mediaId = mediaId,
                    folder = "images",
                    fileName = "wallpaper.jpg",
                    file = tmpFile,
                    uploadId = mediaId,
                    onProgress = { _, _ -> }
                )
            } finally {
                tmpFile.delete()
            }
        }
    }

    private suspend fun uploadChunkedToRtdb(
        context: Context,
        uid: String,
        chatId: String,
        mediaId: String,
        folder: String,
        fileName: String,
        file: File,
        uploadId: String,
        onProgress: (Long, Long) -> Unit
    ): String? {
        val totalBytes = file.length().coerceAtLeast(1L)
        val chunkCount = (((totalBytes + CHUNK_RAW_BYTES - 1L) / CHUNK_RAW_BYTES).toInt()).coerceIn(1, MAX_CHUNKS)
        val mimeType = inferMimeType(folder, fileName)

        val metaSaved = LimonRtdbProvider.saveChatMediaMeta(
            context = context,
            chatId = chatId,
            mediaId = mediaId,
            meta = mapOf(
                "chatId" to chatId,
                "senderId" to uid,
                "fileName" to fileName.take(200),
                "mimeType" to mimeType.take(80),
                "sizeBytes" to totalBytes,
                "chunkCount" to chunkCount,
                "createdAt" to System.currentTimeMillis()
            )
        )
        if (!metaSaved) return null

        var transferred = 0L
        file.inputStream().use { input ->
            val buffer = ByteArray(CHUNK_RAW_BYTES)
            var index = 0
            while (index < chunkCount) {
                if (cancelledIds.contains(uploadId)) {
                    throw CancellationException("Upload cancelled")
                }
                val read = readFullChunk(input, buffer)
                if (read <= 0) break
                val slice = if (read == buffer.size) buffer else buffer.copyOf(read)
                val b64 = Base64.encodeToString(slice, Base64.NO_WRAP)
                val chunkOk = LimonRtdbProvider.saveChatMediaChunk(context, chatId, mediaId, index, b64)
                if (!chunkOk) return null
                transferred += read
                onProgress(transferred, totalBytes)
                index += 1
            }
        }
        return "rtdb://chats/$chatId/media/$mediaId"
    }

    private suspend fun uploadChunkedToFirestore(
        db: FirebaseFirestore,
        uid: String,
        chatId: String,
        mediaId: String,
        folder: String,
        fileName: String,
        file: File,
        uploadId: String,
        onProgress: (Long, Long) -> Unit
    ): String {
        val totalBytes = file.length().coerceAtLeast(1L)
        val chatSnap = db.collection("chats").document(chatId).get().await()
        @Suppress("UNCHECKED_CAST")
        val memberUids = (chatSnap.get("memberUids") as? List<String>)
            ?.takeIf { it.size == 2 && uid in it }
            ?: error("You must be a chat member to upload media")

        val mediaRef = db.collection("chats").document(chatId).collection("media").document(mediaId)
        val chunkCount = (((totalBytes + CHUNK_RAW_BYTES - 1L) / CHUNK_RAW_BYTES).toInt()).coerceIn(1, MAX_CHUNKS)
        require(totalBytes <= chunkCount.toLong() * CHUNK_RAW_BYTES) {
            "Media exceeds maximum chunk count"
        }

        val mimeType = inferMimeType(folder, fileName)
        mediaRef.set(
            mapOf(
                "chatId" to chatId,
                "senderId" to uid,
                "memberUids" to memberUids,
                "fileName" to fileName.take(200),
                "mimeType" to mimeType.take(80),
                "sizeBytes" to totalBytes,
                "chunkCount" to chunkCount,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()

        var transferred = 0L
        file.inputStream().use { input ->
            val buffer = ByteArray(CHUNK_RAW_BYTES)
            var index = 0
            while (index < chunkCount) {
                if (cancelledIds.contains(uploadId)) {
                    throw CancellationException("Upload cancelled")
                }
                val read = readFullChunk(input, buffer)
                if (read <= 0) break
                val slice = if (read == buffer.size) buffer else buffer.copyOf(read)
                val b64 = Base64.encodeToString(slice, Base64.NO_WRAP)
                mediaRef.collection("chunks").document(index.toString()).set(
                    mapOf(
                        "senderId" to uid,
                        "memberUids" to memberUids,
                        "index" to index,
                        "data" to b64
                    )
                ).await()
                transferred += read
                onProgress(transferred, totalBytes)
                index += 1
            }
        }

        return "limonmedia://chats/$chatId/media/$mediaId"
    }

    private fun cacheMediaLocally(context: Context, mediaId: String, fileName: String, source: File) {
        runCatching {
            val mediaDir = File(context.cacheDir, "media_cache").apply { mkdirs() }
            val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val target = File(mediaDir, "${mediaId}_$safeName")
            source.copyTo(target, overwrite = true)
        }
    }

    private fun readFullChunk(input: java.io.InputStream, buffer: ByteArray): Int {
        var offset = 0
        while (offset < buffer.size) {
            val count = input.read(buffer, offset, buffer.size - offset)
            if (count == -1) break
            offset += count
        }
        return offset
    }

    private fun stageUploadFile(
        context: Context,
        folder: String,
        fileName: String,
        uri: Uri?,
        localFile: File?
    ): File? {
        if (localFile != null && localFile.exists() && localFile.length() > 0L) {
            return if (folder == "images") {
                compressFileImage(context, localFile) ?: localFile
            } else {
                localFile
            }
        }
        uri ?: return null
        val stageDir = File(context.cacheDir, "upload_stage").apply { mkdirs() }
        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifBlank { "media_bin" }
        val out = File(stageDir, "${UUID.randomUUID()}_$safeName")

        if (folder == "images") {
            val bytes = decodeAndCompressImage(context, uri, maxDimension = 1440, quality = 82)
            if (bytes != null && bytes.isNotEmpty()) {
                out.writeBytes(bytes)
                return out
            }
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { dest -> input.copyTo(dest) }
        } ?: return null

        return if (out.exists() && out.length() > 0L) out else null
    }

    private fun compressFileImage(context: Context, file: File): File? = runCatching {
        val bytes = decodeAndCompressImage(context, Uri.fromFile(file), maxDimension = 1440, quality = 84)
            ?: return@runCatching null
        val out = File(context.cacheDir, "img_cmp_${UUID.randomUUID()}.jpg")
        out.writeBytes(bytes)
        out
    }.getOrNull()

    private fun decodeAndCompressImage(
        context: Context,
        uri: Uri,
        maxDimension: Int,
        quality: Int
    ): ByteArray? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        var sampleSize = 1
        var w = bounds.outWidth
        var h = bounds.outHeight
        while (w / 2 >= maxDimension || h / 2 >= maxDimension) {
            w /= 2
            h /= 2
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return@runCatching null

        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val rotated = applyExifRotation(decoded, orientation)

        val maxSide = maxOf(rotated.width, rotated.height)
        val scaled = if (maxSide > maxDimension) {
            val ratio = maxDimension.toFloat() / maxSide.toFloat()
            Bitmap.createScaledBitmap(
                rotated,
                (rotated.width * ratio).toInt().coerceAtLeast(1),
                (rotated.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else {
            rotated
        }

        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        if (scaled !== rotated) rotated.recycle()
        if (rotated !== decoded) decoded.recycle()
        scaled.recycle()
        baos.toByteArray()
    }.getOrNull()

    private fun applyExifRotation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrDefault(bitmap)
    }

    private fun inferMimeType(folder: String, fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val mapped = if (ext.isNotBlank()) MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) else null
        if (!mapped.isNullOrBlank()) return mapped
        return when (folder) {
            "images" -> "image/jpeg"
            "videos" -> "video/mp4"
            "voice" -> "audio/mp4"
            else -> "application/octet-stream"
        }
    }
}

object LimonMediaResolver {

    internal fun stripQuery(url: String): String = url.substringBefore('?')

    internal fun extractVersion(url: String): String? =
        url.substringAfter('?', "").split('&')
            .firstOrNull { it.startsWith("v=") }
            ?.removePrefix("v=")
            ?.takeIf { it.isNotBlank() }

    suspend fun resolveToLocalFile(
        context: Context,
        db: FirebaseFirestore?,
        url: String,
        fallbackFileName: String? = null
    ): File? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        val version = extractVersion(url)
        val cleanUrl = stripQuery(url)

        if (cleanUrl.startsWith("file://")) {
            val f = File(Uri.parse(cleanUrl).path ?: return@withContext null)
            return@withContext f.takeIf { it.exists() && it.length() > 0L }
        }
        if (cleanUrl.startsWith("/")) {
            val f = File(cleanUrl)
            return@withContext f.takeIf { it.exists() && it.length() > 0L }
        }

        // RTDB Avatar or Legacy Firestore Avatar
        if (cleanUrl.startsWith("rtdb://avatars/") || cleanUrl.startsWith("limonavatar://")) {
            val uid = cleanUrl
                .removePrefix("rtdb://avatars/")
                .removePrefix("limonavatar://")
                .trim('/', ' ')
            if (uid.isBlank()) return@withContext null

            val avatarDir = File(context.cacheDir, "avatar_cache").apply { mkdirs() }
            val target = File(avatarDir, "$uid.jpg")
            val verFile = File(avatarDir, "$uid.ver")
            val cachedVer = if (verFile.exists()) verFile.readText().trim() else null
            val versionMatches = version == null || version == cachedVer
            if (versionMatches && target.exists() && target.length() > 0L &&
                (System.currentTimeMillis() - target.lastModified() < 120_000L)
            ) {
                return@withContext target
            }

            // Try Realtime Database first, then Firestore publicAvatars fallback
            val bytes = LimonRtdbProvider.readAvatarBytes(context, uid)
                ?: runCatching {
                    val snap = db?.collection("publicAvatars")?.document(uid)?.get()?.await()
                    val encoded = snap?.getString("data") ?: return@runCatching null
                    Base64.decode(encoded, Base64.DEFAULT)
                }.getOrNull()

            if (bytes != null && bytes.isNotEmpty()) {
                val tmp = File(avatarDir, "$uid.tmp")
                tmp.writeBytes(bytes)
                if (target.exists()) target.delete()
                tmp.renameTo(target)
                if (version != null) verFile.writeText(version)
                return@withContext target.takeIf { it.exists() && it.length() > 0L }
            }
            return@withContext target.takeIf { it.exists() && it.length() > 0L }
        }

        // RTDB Shared Wallpaper
        if (cleanUrl.startsWith("rtdb://wallpapers/")) {
            val chatId = cleanUrl.removePrefix("rtdb://wallpapers/").trim('/', ' ')
            if (chatId.isBlank()) return@withContext null
            val wallDir = File(context.cacheDir, "wallpaper_cache").apply { mkdirs() }
            val target = File(wallDir, "$chatId.jpg")
            val verFile = File(wallDir, "$chatId.ver")
            val cachedVer = if (verFile.exists()) verFile.readText().trim() else null
            if ((version == null || version == cachedVer) && target.exists() && target.length() > 0L) {
                return@withContext target
            }
            val bytes = LimonRtdbProvider.readSharedWallpaperBytes(context, chatId)
            if (bytes != null && bytes.isNotEmpty()) {
                val tmp = File(wallDir, "$chatId.tmp")
                tmp.writeBytes(bytes)
                if (target.exists()) target.delete()
                tmp.renameTo(target)
                if (version != null) verFile.writeText(version)
                return@withContext target.takeIf { it.exists() && it.length() > 0L }
            }
            return@withContext target.takeIf { it.exists() && it.length() > 0L }
        }

        // RTDB Chat Media or Legacy Firestore Chunked Chat Media
        if (cleanUrl.startsWith("rtdb://chats/") || cleanUrl.startsWith("limonmedia://")) {
            val cleaned = cleanUrl
                .removePrefix("rtdb://")
                .removePrefix("limonmedia://")
                .trim('/')
            val parts = cleaned.split("/")
            if (parts.size == 4 && parts[0] == "chats" && parts[2] == "media") {
                val chatId = parts[1]
                val mediaId = parts[3]
                val mediaDir = File(context.cacheDir, "media_cache").apply { mkdirs() }
                val cached = mediaDir.listFiles()?.firstOrNull {
                    it.name.startsWith("${mediaId}_") && it.length() > 0L
                }
                if (cached != null) return@withContext cached

                // 1. Try RTDB if scheme is rtdb:// (or as fallback)
                if (cleanUrl.startsWith("rtdb://")) {
                    val rtdbPayload = LimonRtdbProvider.readChatMedia(context, chatId, mediaId)
                    if (rtdbPayload != null) {
                        val safeName = rtdbPayload.fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                        val target = File(mediaDir, "${mediaId}_$safeName")
                        val tmp = File(mediaDir, "$mediaId.tmp")
                        tmp.outputStream().use { out ->
                            rtdbPayload.chunks.forEach { out.write(it) }
                        }
                        if (tmp.length() > 0L) {
                            tmp.renameTo(target)
                            return@withContext target
                        } else {
                            tmp.delete()
                        }
                    }
                }

                // 2. Fallback / Primary for limonmedia:// -> Firestore
                if (db != null) {
                    return@withContext runCatching {
                        val mediaRef = db.collection("chats").document(chatId).collection("media").document(mediaId)
                        val meta = mediaRef.get().await()
                        if (!meta.exists()) return@runCatching null
                        val fileName = meta.getString("fileName") ?: fallbackFileName ?: "$mediaId.bin"
                        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                        val target = File(mediaDir, "${mediaId}_$safeName")
                        if (target.exists() && target.length() > 0L) return@runCatching target

                        val chunksSnap = mediaRef.collection("chunks").get().await()
                        val ordered = chunksSnap.documents.sortedBy { doc ->
                            (doc.getLong("index") ?: doc.id.toLongOrNull() ?: 0L)
                        }
                        val tmp = File(mediaDir, "$mediaId.tmp")
                        tmp.outputStream().use { out ->
                            ordered.forEach { chunkDoc ->
                                val b64 = chunkDoc.getString("data") ?: return@forEach
                                val bytes = Base64.decode(b64, Base64.DEFAULT)
                                out.write(bytes)
                            }
                        }
                        if (tmp.length() > 0L) {
                            tmp.renameTo(target)
                            target
                        } else {
                            tmp.delete()
                            null
                        }
                    }.getOrNull()
                }
            }
            return@withContext null
        }

        // http/https URL fallback
        val dir = File(context.cacheDir, "media_cache").apply { mkdirs() }
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(24)
        val ext = fallbackFileName?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() } ?: "bin"
        val target = File(dir, "$hash.$ext")
        if (target.exists() && target.length() > 0L) return@withContext target
        return@withContext runCatching {
            val tmp = File(dir, "$hash.tmp")
            URL(url).openStream().use { input ->
                tmp.outputStream().use { out -> input.copyTo(out) }
            }
            if (tmp.length() > 0L) {
                tmp.renameTo(target)
                target
            } else {
                tmp.delete()
                null
            }
        }.getOrNull()
    }

    @OptIn(ExperimentalCoilApi::class)
    fun purgeCachedMedia(context: Context, url: String?) {
        if (url.isNullOrBlank()) return
        runCatching {
            val cleanUrl = stripQuery(url)
            val mediaDir = File(context.cacheDir, "media_cache")
            if (mediaDir.exists()) {
                if (cleanUrl.startsWith("rtdb://chats/") || cleanUrl.startsWith("limonmedia://")) {
                    val mediaId = cleanUrl.trimEnd('/').substringAfterLast('/')
                    if (mediaId.isNotBlank()) {
                        mediaDir.listFiles()?.forEach { file ->
                            if (file.name.startsWith("${mediaId}_") || file.name.startsWith("$mediaId.")) {
                                file.delete()
                            }
                        }
                    }
                } else {
                    val hash = MessageDigest.getInstance("SHA-256")
                        .digest(url.toByteArray())
                        .joinToString("") { "%02x".format(it) }
                        .take(24)
                    mediaDir.listFiles()?.forEach { file ->
                        if (file.name.startsWith(hash)) {
                            file.delete()
                        }
                    }
                }
            }
            context.imageLoader.memoryCache?.clear()
        }
    }
}

class LimonCoilFetcherFactory(
    private val appContext: Context,
    private val db: FirebaseFirestore
) : Fetcher.Factory<Uri> {

    override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
        val scheme = data.scheme ?: return null
        if (scheme != "limonmedia" && scheme != "limonavatar" && scheme != "rtdb") return null
        return Fetcher {
            val localFile = LimonMediaResolver.resolveToLocalFile(appContext, db, data.toString())
                ?: throw IllegalStateException("Media unavailable: $data")
            SourceResult(
                source = ImageSource(
                    file = localFile.toOkioPath(),
                    fileSystem = FileSystem.SYSTEM,
                    diskCacheKey = data.toString()
                ),
                mimeType = "image/*",
                dataSource = DataSource.NETWORK
            )
        }
    }
}
