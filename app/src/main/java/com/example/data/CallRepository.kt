package com.example.data

import android.content.Context
import com.example.R
import com.example.model.CallSession
import com.example.model.CallStatus
import com.example.model.CallType
import com.example.model.IceCandidateRecord
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CallRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in.")

    fun observeCallHistory(uid: String): Flow<List<CallSession>> {
        val path = "calls"
        return db.collection("calls")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CallSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                    .filter { uid !in it.deletedForUsers }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeCallsBetween(uid: String, peerUid: String): Flow<List<CallSession>> {
        return observeCallHistory(uid).map { calls ->
            calls.filter { call ->
                (call.callerId == uid && call.receiverId == peerUid) ||
                    (call.callerId == peerUid && call.receiverId == uid)
            }.sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }
    }

    fun observeIncomingRingingCall(uid: String): Flow<CallSession?> {
        val path = "calls"
        return db.collection("calls")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snapshot ->
                val nowSec = Timestamp.now().seconds
                snapshot.toObjects(
                    CallSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).firstOrNull { call ->
                    call.receiverId == uid &&
                        call.status == CallStatus.RINGING.value &&
                        uid !in call.deletedForUsers &&
                        (nowSec - (call.createdAt?.seconds ?: nowSec)) <= 45L
                }
            }
            .catch { error ->
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                emit(null)
            }
    }

    fun observeCallSession(callId: String): Flow<CallSession?> {
        val path = "calls/$callId"
        return db.collection("calls").document(callId)
            .snapshots()
            .map { snap ->
                snap.toObject(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    fun observeRemoteIceCandidates(callId: String, myUid: String): Flow<List<IceCandidateRecord>> {
        val path = "calls/$callId/iceCandidates"
        return db.collection("calls").document(callId).collection("iceCandidates")
            .whereArrayContains("memberUids", myUid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    IceCandidateRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).filter { it.senderId != myUid }
            }
            .catch { error ->
                if (error !is CancellationException) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                emit(emptyList())
            }
    }

    suspend fun startCall(
        receiverUid: String,
        callerName: String,
        callerPhotoUrl: String?,
        receiverName: String,
        receiverPhotoUrl: String?,
        callType: CallType,
        offerSdp: String?
    ): Result<String> = runCatching {
        val myUid = requireUserId()
        require(receiverUid != myUid) { "Cannot call yourself" }
        val callId = UUID.randomUUID().toString()
        val payload = mutableMapOf<String, Any>(
            "callerId" to myUid,
            "receiverId" to receiverUid,
            "memberUids" to listOf(myUid, receiverUid).sorted(),
            "callerName" to callerName.take(80).ifBlank { "Limon User" },
            "receiverName" to receiverName.take(80).ifBlank { "Limon User" },
            "callType" to callType.value,
            "status" to CallStatus.RINGING.value,
            "durationSeconds" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!callerPhotoUrl.isNullOrBlank()) payload["callerPhotoUrl"] = callerPhotoUrl.take(500)
        if (!receiverPhotoUrl.isNullOrBlank()) payload["receiverPhotoUrl"] = receiverPhotoUrl.take(500)
        if (!offerSdp.isNullOrBlank()) payload["offerSdp"] = offerSdp.take(32768)

        db.collection("calls").document(callId).set(payload).await()
        callId
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.CREATE, "calls")
    }

    suspend fun updateOfferSdp(callId: String, offerSdp: String) {
        try {
            db.collection("calls").document(callId).update(
                mapOf(
                    "offerSdp" to offerSdp.take(32768),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "calls/$callId")
        }
    }

    suspend fun markCallAcceptedPendingSdp(callId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("calls").document(callId).update(
            mapOf(
                "status" to CallStatus.ACCEPTED.value,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.UPDATE, "calls/$callId")
    }

    suspend fun acceptCall(callId: String, answerSdp: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("calls").document(callId).update(
            mapOf(
                "status" to CallStatus.ACCEPTED.value,
                "answerSdp" to answerSdp.take(32768),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.UPDATE, "calls/$callId")
    }

    suspend fun endCall(
        callId: String,
        finalStatus: CallStatus,
        durationSeconds: Long
    ) {
        try {
            db.collection("calls").document(callId).update(
                mapOf(
                    "status" to finalStatus.value,
                    "durationSeconds" to durationSeconds.coerceAtLeast(0L),
                    "endedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            if (e !is CancellationException) {
                handleFirestoreError(e, OperationType.UPDATE, "calls/$callId")
            }
        }
    }

    suspend fun deleteCallLogForMe(callId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        db.collection("calls").document(callId).update(
            mapOf(
                "deletedForUsers" to FieldValue.arrayUnion(uid),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure { e ->
        handleFirestoreError(e, OperationType.UPDATE, "calls/$callId")
    }

    suspend fun sendIceCandidate(
        callId: String,
        peerUid: String,
        sdpMid: String,
        sdpMLineIndex: Int,
        candidate: String
    ) {
        val myUid = auth.currentUser?.uid ?: return
        val id = UUID.randomUUID().toString()
        val payload = mapOf(
            "senderId" to myUid,
            "memberUids" to listOf(myUid, peerUid).sorted(),
            "sdpMid" to sdpMid.take(64),
            "sdpMLineIndex" to sdpMLineIndex,
            "candidate" to candidate.take(2048),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            db.collection("calls").document(callId).collection("iceCandidates").document(id)
                .set(payload).await()
        } catch (_: Exception) {
        }
    }
}
