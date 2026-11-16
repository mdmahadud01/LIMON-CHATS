package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.CallRepository
import com.example.data.ChatRepository
import com.example.data.UserRepository
import com.example.model.CallStatus
import com.example.model.CallType
import com.example.model.ChatWallpaperPreset
import com.example.model.DeliveryStatus
import com.example.model.MessageType
import com.example.model.PrivacyScope
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class LimonChatRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun accountInitialization_and_usernameUniqueness_enforcedByRules(): Unit = runBlocking {
        val aliceUid = signInTestUser("fresh_${UUID.randomUUID().toString().take(8)}@test.com")
        val aliceUser = checkNotNull(auth.currentUser)
        val userRepo = UserRepository(firestore)

        withTimeout(TIMEOUT_MS) { userRepo.setOnlinePresence(true) }

        val initRes = withTimeout(TIMEOUT_MS) { userRepo.ensureAccountInitialized(aliceUser) }
        assertTrue("Expected account initialization to succeed: ${initRes.exceptionOrNull()}", initRes.isSuccess)
        withTimeout(TIMEOUT_MS) { userRepo.setOnlinePresence(false) }
        withTimeout(TIMEOUT_MS) { userRepo.setOnlinePresence(true) }

        val uniqueHandle = "limon_${UUID.randomUUID().toString().take(8)}"
        val updateRes = withTimeout(TIMEOUT_MS) {
            userRepo.updateProfile("Alice Limon", uniqueHandle, "Glass explorer", null, "+15550101")
        }
        assertTrue("Expected profile update to succeed: ${updateRes.exceptionOrNull()}", updateRes.isSuccess)

        val bobUid = signInTestUser(BOB_EMAIL)
        val bobUser = checkNotNull(auth.currentUser)
        withTimeout(TIMEOUT_MS) { userRepo.ensureAccountInitialized(bobUser) }

        val duplicateAttempt = withTimeout(TIMEOUT_MS) {
            userRepo.updateProfile("Bob Imposter", uniqueHandle, "Trying duplicate", null)
        }
        assertTrue("Duplicate username must fail", duplicateAttempt.isFailure)

        try {
            withTimeout(TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("private").document("info")
                    .get().await()
            }
            fail("Expected PERMISSION_DENIED when Bob reads Alice private info")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException() ?: throw e
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }

        val privacyRes = withTimeout(TIMEOUT_MS) {
            userRepo.updatePrivacySettings(PrivacyScope.CONTACTS, PrivacyScope.EVERYONE, PrivacyScope.NOBODY, true)
        }
        assertTrue("Own privacy update must succeed ($bobUid)", privacyRes.isSuccess)
    }

    @Test
    fun realTimeChat_messagingAndReadReceipts_andBlockingEnforcement(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val userRepo = UserRepository(firestore)
        withTimeout(TIMEOUT_MS) { userRepo.ensureAccountInitialized(checkNotNull(auth.currentUser)) }

        val bobUid = signInTestUser(BOB_EMAIL)
        withTimeout(TIMEOUT_MS) { userRepo.ensureAccountInitialized(checkNotNull(auth.currentUser)) }

        withTimeout(TIMEOUT_MS) { userRepo.unblockUser(aliceUid) }

        signInTestUser(ALICE_EMAIL)
        withTimeout(TIMEOUT_MS) { userRepo.unblockUser(bobUid) }
        val chatRepo = ChatRepository(firestore)
        val chatId = withTimeout(TIMEOUT_MS) { chatRepo.getOrCreateChat(bobUid).getOrThrow() }

        val sendRes = withTimeout(TIMEOUT_MS) {
            chatRepo.sendMessage(
                chatId = chatId,
                peerUid = bobUid,
                type = MessageType.TEXT,
                text = "Hello Liquid Glass!"
            )
        }
        assertTrue("Alice should send message: ${sendRes.exceptionOrNull()}", sendRes.isSuccess)
        val messageId = sendRes.getOrThrow()

        signInTestUser(BOB_EMAIL)
        withTimeout(TIMEOUT_MS) {
            chatRepo.acknowledgeChatDelivered(chatId, bobUid, messageId)
        }
        val deliveredSnap = withTimeout(TIMEOUT_MS) {
            firestore.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .get().await()
        }
        assertEquals(DeliveryStatus.DELIVERED.value, deliveredSnap.getString("deliveryStatus"))

        val bobMessages = withTimeout(FLOW_TIMEOUT_MS) {
            chatRepo.observeMessages(chatId, bobUid, 20L).first { list -> list.any { it.id == messageId } }
        }
        assertTrue(bobMessages.any { it.id == messageId && it.text == "Hello Liquid Glass!" })

        withTimeout(TIMEOUT_MS) {
            chatRepo.markLatestMessagesRead(chatId = chatId, uid = bobUid, sendReadReceipts = true)
        }

        val updatedMsg = withTimeout(TIMEOUT_MS) {
            firestore.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .get().await()
        }
        assertEquals(DeliveryStatus.READ.value, updatedMsg.getString("deliveryStatus"))

        signInTestUser(CHARLIE_EMAIL)
        try {
            withTimeout(TIMEOUT_MS) {
                firestore.collection("chats").document(chatId).get().await()
            }
            fail("Expected PERMISSION_DENIED for non-member reading chat")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException() ?: throw e
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }

        signInTestUser(BOB_EMAIL)
        val aliceProfile = withTimeout(TIMEOUT_MS) { userRepo.getProfileOnce(aliceUid) }!!
        withTimeout(TIMEOUT_MS) { userRepo.blockUser(aliceProfile) }

        signInTestUser(ALICE_EMAIL)
        val blockedSend = withTimeout(TIMEOUT_MS) {
            chatRepo.sendMessage(chatId, bobUid, MessageType.TEXT, "Should be blocked")
        }
        assertTrue("Blocked sender must be denied by security rules", blockedSend.isFailure)

        signInTestUser(BOB_EMAIL)
        withTimeout(TIMEOUT_MS) { userRepo.unblockUser(aliceUid) }
        Unit
    }

    @Test
    fun sharedWallpaper_persistsForBothMembers_andCanBeCleared(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val bobUid = signInTestUser(BOB_EMAIL)
        val userRepo = UserRepository(firestore)
        withTimeout(TIMEOUT_MS) { userRepo.unblockUser(aliceUid) }

        signInTestUser(ALICE_EMAIL)
        withTimeout(TIMEOUT_MS) { userRepo.unblockUser(bobUid) }
        val chatRepo = ChatRepository(firestore)
        val chatId = withTimeout(TIMEOUT_MS) { chatRepo.getOrCreateChat(bobUid).getOrThrow() }

        val setRes = withTimeout(TIMEOUT_MS) {
            chatRepo.setSharedWallpaper(
                chatId = chatId,
                preset = ChatWallpaperPreset.CUSTOM,
                customUrl = "rtdb://wallpapers/$chatId?v=42",
                blur = true
            )
        }
        assertTrue("Shared wallpaper update failed: ${setRes.exceptionOrNull()}", setRes.isSuccess)

        signInTestUser(BOB_EMAIL)
        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            chatRepo.observeChat(chatId).first { it?.sharedWallpaperPreset == ChatWallpaperPreset.CUSTOM.name }
        }
        assertEquals("rtdb://wallpapers/$chatId?v=42", observed?.sharedWallpaperUrl)
        assertEquals(true, observed?.sharedWallpaperBlur)
        assertEquals(aliceUid, observed?.sharedWallpaperBy)

        val clearRes = withTimeout(TIMEOUT_MS) {
            chatRepo.setSharedWallpaper(chatId = chatId, preset = null, customUrl = null, blur = null)
        }
        assertTrue("Clearing shared wallpaper failed: ${clearRes.exceptionOrNull()}", clearRes.isSuccess)
        val cleared = withTimeout(FLOW_TIMEOUT_MS) {
            chatRepo.observeChat(chatId).first { it?.sharedWallpaperPreset == null }
        }
        assertNull(cleared?.sharedWallpaperUrl)
    }

    @Test
    fun locationMessage_and_multiDeviceFcmTokens_persistCleanly(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val userRepo = UserRepository(firestore)
        withTimeout(TIMEOUT_MS) { userRepo.ensureAccountInitialized(checkNotNull(auth.currentUser)) }

        withTimeout(TIMEOUT_MS) {
            userRepo.saveFcmToken("token_phone_a", "device_a")
            userRepo.saveFcmToken("token_tablet_b", "device_b")
        }
        val privateSnap = withTimeout(TIMEOUT_MS) {
            firestore.collection("users").document(aliceUid)
                .collection("private").document("info")
                .get().await()
        }
        val fcmMap = privateSnap.get("fcmTokens") as? Map<*, *>
        assertEquals("token_phone_a", fcmMap?.get("device_a"))
        assertEquals("token_tablet_b", fcmMap?.get("device_b"))

        withTimeout(TIMEOUT_MS) { userRepo.clearDeviceFcmToken("device_a") }
        val updatedPrivate = withTimeout(TIMEOUT_MS) {
            firestore.collection("users").document(aliceUid)
                .collection("private").document("info")
                .get().await()
        }
        val updatedMap = updatedPrivate.get("fcmTokens") as? Map<*, *>
        assertTrue(updatedMap?.containsKey("device_a") != true)
        assertEquals("token_tablet_b", updatedMap?.get("device_b"))

        val bobUid = signInTestUser(BOB_EMAIL)
        withTimeout(TIMEOUT_MS) { userRepo.ensureAccountInitialized(checkNotNull(auth.currentUser)) }
        signInTestUser(ALICE_EMAIL)
        val chatRepo = ChatRepository(firestore)
        val chatId = withTimeout(TIMEOUT_MS) { chatRepo.getOrCreateChat(bobUid).getOrThrow() }

        val locId = withTimeout(TIMEOUT_MS) {
            chatRepo.sendMessage(
                chatId = chatId,
                peerUid = bobUid,
                type = MessageType.LOCATION,
                text = "Ferry Building, San Francisco",
                latitude = 37.7955,
                longitude = -122.3937,
                locationName = "Ferry Building, San Francisco",
                messageIdOverride = "loc_${UUID.randomUUID().toString().take(8)}"
            ).getOrThrow()
        }

        val locSnap = withTimeout(TIMEOUT_MS) {
            firestore.collection("chats").document(chatId)
                .collection("messages").document(locId)
                .get().await()
        }
        assertEquals("Ferry Building, San Francisco", locSnap.getString("locationName"))
    }

    @Test
    fun callSignalingLifecycle_deleteCallLogForMe_and_unauthenticatedRejection(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val bobUid = signInTestUser(BOB_EMAIL)

        signInTestUser(ALICE_EMAIL)
        val callRepo = CallRepository(firestore)
        val callId = withTimeout(TIMEOUT_MS) {
            callRepo.startCall(
                receiverUid = bobUid,
                callerName = "Alice",
                callerPhotoUrl = null,
                receiverName = "Bob",
                receiverPhotoUrl = null,
                callType = CallType.VIDEO,
                offerSdp = "v=0\no=- 123 2 IN IP4 127.0.0.1"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val incoming = withTimeout(FLOW_TIMEOUT_MS) {
            callRepo.observeIncomingRingingCall(bobUid).first { it?.id == callId }
        }
        assertNotNull(incoming)

        val acceptPendingRes = withTimeout(TIMEOUT_MS) {
            callRepo.markCallAcceptedPendingSdp(callId)
        }
        assertTrue(acceptPendingRes.isSuccess)

        val acceptRes = withTimeout(TIMEOUT_MS) {
            callRepo.acceptCall(callId, "v=0\no=- 456 2 IN IP4 127.0.0.1")
        }
        assertTrue(acceptRes.isSuccess)

        withTimeout(TIMEOUT_MS) { callRepo.endCall(callId, CallStatus.ENDED, 12L) }

        // Bob deletes the ended call log for himself while Alice still retains her copy
        val delBobRes = withTimeout(TIMEOUT_MS) { callRepo.deleteCallLogForMe(callId) }
        assertTrue("deleteCallLogForMe failed for Bob: ${delBobRes.exceptionOrNull()}", delBobRes.isSuccess)
        val bobHistory = withTimeout(FLOW_TIMEOUT_MS) {
            callRepo.observeCallHistory(bobUid).first()
        }
        assertFalse(bobHistory.any { it.id == callId })

        signInTestUser(ALICE_EMAIL)
        val aliceHistory = withTimeout(FLOW_TIMEOUT_MS) {
            callRepo.observeCallHistory(aliceUid).first()
        }
        assertTrue(aliceHistory.any { it.id == callId })

        // Negative security test: Unauthenticated caller must receive PERMISSION_DENIED
        auth.signOut()
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                ChatRepository(firestore).observeChats(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when querying chats unauthenticated")
        } catch (e: Exception) {
            val fsEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    @Test
    fun chatRepository_markChatAsRead_and_deleteConversation_workUnderRules(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val bobUid = signInTestUser(BOB_EMAIL)

        signInTestUser(ALICE_EMAIL)
        val chatRepo = ChatRepository(firestore)
        val chatId = withTimeout(TIMEOUT_MS) { chatRepo.getOrCreateChat(bobUid).getOrThrow() }
        withTimeout(TIMEOUT_MS) {
            chatRepo.sendMessage(chatId, bobUid, MessageType.TEXT, "Unread ping").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val markRes = withTimeout(TIMEOUT_MS) { chatRepo.markChatAsRead(chatId, sendReadReceipts = true) }
        assertTrue("markChatAsRead failed: ${markRes.exceptionOrNull()}", markRes.isSuccess)

        val chatAfterRead = withTimeout(TIMEOUT_MS) {
            firestore.collection("chats").document(chatId).get().await()
        }
        val unreadMap = chatAfterRead.get("unreadBy") as? Map<*, *>
        assertEquals(0L, (unreadMap?.get(bobUid) as? Number)?.toLong() ?: -1L)

        val deleteRes = withTimeout(TIMEOUT_MS) { chatRepo.deleteConversation(chatId) }
        assertTrue("deleteConversation failed: ${deleteRes.exceptionOrNull()}", deleteRes.isSuccess)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_limon@test.com"
        const val BOB_EMAIL = "bob_limon@test.com"
        const val CHARLIE_EMAIL = "charlie_limon@test.com"
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
