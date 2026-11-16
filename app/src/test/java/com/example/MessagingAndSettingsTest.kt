package com.example

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DeviceIdentity
import com.example.data.SettingsDataStore
import com.example.media.LimonMediaResolver
import com.example.media.VoicePlayerManager
import com.example.media.VoiceRecorderManager
import com.example.model.ChatWallpaperPreset
import com.example.notifications.LimonMessagingService
import com.example.notifications.LimonRealtimeSyncService
import com.example.notifications.NotificationHelper
import com.example.ui.screens.formatCallClockTime
import com.example.ui.screens.formatCallDurationCompact
import com.example.ui.screens.formatCallLogDisplay
import com.example.ui.screens.formatShortTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MessagingAndSettingsTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationHelper.setAppForeground(false)
        NotificationHelper.setActiveOpenChatId(null)
    }

    @Test
    fun deviceIdentity_returnsStablePerInstallId() {
        val first = DeviceIdentity.getDeviceId(app)
        val second = DeviceIdentity.getDeviceId(app)
        assertTrue(first.startsWith("android_"))
        assertEquals(first, second)
    }

    @Test
    fun settingsDataStore_persistsWallpaperAndBlurPreferences() = runBlocking {
        val store = SettingsDataStore(app)
        store.setChatWallpaper(ChatWallpaperPreset.LAVENDER_SKY)
        store.setWallpaperBlur(true)

        val current = store.currentSettings()
        assertEquals(ChatWallpaperPreset.LAVENDER_SKY, current.chatWallpaper)
        assertTrue(current.wallpaperBlur)

        store.setCustomWallpaperUri("file:///data/user/0/app/files/wallpaper.jpg")
        val updated = store.currentSettings()
        assertEquals(ChatWallpaperPreset.CUSTOM, updated.chatWallpaper)
        assertEquals("file:///data/user/0/app/files/wallpaper.jpg", updated.customWallpaperUri)
    }

    @Test
    fun notificationHelper_postsMessageAndCallNotifications_andCancelsCleanly() {
        NotificationHelper.showMessageNotification(
            context = app,
            chatId = "chat_abc",
            peerUid = "peer_123",
            senderName = "Maya Lin",
            previewText = "See you at 5pm",
            showPreview = true,
            playSound = true,
            vibrate = true
        )

        val mgr = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val posted = shadowOf(mgr).allNotifications
        assertTrue("Expected posted notifications, got empty list", posted.isNotEmpty())
        assertTrue(
            "Expected message notification for Maya Lin in ${posted.map { it.extras.keySet() }}",
            posted.any { notif ->
                val style = androidx.core.app.NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notif)
                val title = notif.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()
                val convTitle = notif.extras.getCharSequence(android.app.Notification.EXTRA_CONVERSATION_TITLE)?.toString()
                val hiddenTitle = notif.extras.getCharSequence("android.hiddenConversationTitle")?.toString()
                title == "Maya Lin" ||
                    convTitle == "Maya Lin" ||
                    hiddenTitle == "Maya Lin" ||
                    style?.conversationTitle?.toString() == "Maya Lin" ||
                    style?.messages?.any { it.text?.toString() == "See you at 5pm" } == true
            }
        )
        assertTrue(posted.any { it.group == NotificationHelper.GROUP_MESSAGES })

        NotificationHelper.showIncomingCallNotification(
            context = app,
            callId = "call_xyz",
            callerName = "Maya Lin",
            callType = "video"
        )
        assertNotNull(shadowOf(mgr).getNotification("call_xyz".hashCode()))

        NotificationHelper.cancelCallNotification(app, "call_xyz")
        NotificationHelper.showMissedCallNotification(app, "call_xyz", "Maya Lin", "video", "peer_123")
        assertNotNull(shadowOf(mgr).getNotification("call_xyz".hashCode() + 100))
    }

    @Test
    fun voiceManagers_trackPlaybackSpeedAndRecordingDefaults() {
        val recorder = VoiceRecorderManager(app)
        assertFalse(recorder.isRecording.value)
        assertFalse(recorder.isPaused.value)

        val player = VoicePlayerManager(app.cacheDir)
        assertEquals(1.0f, player.state.value.speed, 0.01f)
        player.cycleSpeed()
        assertEquals(1.5f, player.state.value.speed, 0.01f)
        player.cycleSpeed()
        assertEquals(2.0f, player.state.value.speed, 0.01f)
        player.cycleSpeed()
        assertEquals(1.0f, player.state.value.speed, 0.01f)
    }

    @Test
    fun notificationHelper_deduplicatesRepeatedMessagesAndCalls() {
        val uid = "user_dedup"
        val chatId = "chat_dedup"
        NotificationHelper.markChatTimestampNotifiedIfMissing(app, uid, chatId, 1000L)
        assertFalse(NotificationHelper.shouldNotifyMessage(app, uid, chatId, 1000L))
        assertTrue(NotificationHelper.shouldNotifyMessage(app, uid, chatId, 2000L))
        assertFalse(NotificationHelper.shouldNotifyMessage(app, uid, chatId, 2000L))

        val callId = "call_dedup_1"
        assertTrue(NotificationHelper.shouldNotifyCall(app, callId))
        assertFalse(NotificationHelper.shouldNotifyCall(app, callId))
    }

    @Test
    fun appSettings_defaultsToLiquidGlass() = runBlocking {
        val store = SettingsDataStore(app)
        val defaults = store.currentSettings()
        assertEquals(com.example.model.AppearanceMode.LIQUID_GLASS, defaults.appearanceMode)
        assertEquals(com.example.model.AppearanceMode.LIQUID_GLASS, com.example.model.AppSettings().appearanceMode)
    }

    @Test
    fun realtimeNotification_suppressesOnlyWhenForegroundAndInboxOpen_andSchedulesJobs() {
        // When inbox is open in background, notifications MUST still fire (isChatCurrentlyOpen == false)
        NotificationHelper.setActiveOpenChatId("chat_open_1")
        NotificationHelper.setAppForeground(false)
        assertFalse(NotificationHelper.isChatCurrentlyOpen("chat_open_1"))

        // When user is actively viewing the chat in foreground, suppress duplicate notifications
        NotificationHelper.setAppForeground(true)
        assertTrue(NotificationHelper.isChatCurrentlyOpen("chat_open_1"))
        assertFalse(NotificationHelper.isChatCurrentlyOpen("chat_other"))

        NotificationHelper.setActiveOpenChatId(null)
        assertFalse(NotificationHelper.isChatCurrentlyOpen("chat_open_1"))

        // ensureChannels cleans up legacy sync channel
        NotificationHelper.ensureChannels(app)
        val mgr = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertTrue(shadowOf(mgr).allNotifications.none { it.channelId == NotificationHelper.CHANNEL_REALTIME_SYNC })

        LimonMessagingService.cacheFcmToken(app, "test_fcm_token_xyz")
        assertEquals("test_fcm_token_xyz", LimonMessagingService.getCachedFcmToken(app))

        LimonRealtimeSyncService.schedule(app)
        LimonRealtimeSyncService.scheduleImmediateSync(app)
        val scheduler = app.getSystemService(Context.JOB_SCHEDULER_SERVICE) as android.app.job.JobScheduler
        assertTrue(scheduler.allPendingJobs.isNotEmpty())
    }

    @Test
    fun timeFormatting_respects12HourAnd24HourPreferences() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
        }
        val date = cal.time
        assertEquals("15:05", formatShortTime(date, is24Hour = true))
        assertTrue(formatShortTime(date, is24Hour = false).contains("3:05"))
        assertEquals("15:05", formatCallClockTime(date, is24Hour = true))
        assertTrue(formatCallClockTime(date, is24Hour = false).contains("3:05"))
    }

    @Test
    fun mediaUrlResolver_stripsCacheBustingQueryAndExtractsVersion() {
        val url = "rtdb://avatars/uid_123?v=1731654321"
        assertEquals("rtdb://avatars/uid_123", LimonMediaResolver.stripQuery(url))
        assertEquals("1731654321", LimonMediaResolver.extractVersion(url))
        assertEquals("limonmedia://chats/c1/media/m1", LimonMediaResolver.stripQuery("limonmedia://chats/c1/media/m1"))
    }

    @Test
    fun callLogBubble_formatsIncomingOutgoingAndMissedCalls() {
        assertEquals("45s", formatCallDurationCompact(45L))
        assertEquals("1 minute", formatCallDurationCompact(68L))
        assertEquals("3 minutes", formatCallDurationCompact(185L))

        val incoming = com.example.model.CallSession(
            id = "c1",
            callerId = "peer_1",
            receiverId = "me_1",
            callType = com.example.model.CallType.VOICE.value,
            status = com.example.model.CallStatus.ENDED.value,
            durationSeconds = 64L
        )
        val incomingDisplay = formatCallLogDisplay(incoming, "me_1")
        assertEquals("Incoming Call", incomingDisplay.title)
        assertEquals("(1 minute)", incomingDisplay.subtitle)
        assertFalse(incomingDisplay.isOutgoing)
        assertFalse(incomingDisplay.isMissedOrDeclined)

        val missed = incoming.copy(
            id = "c2",
            status = com.example.model.CallStatus.MISSED.value,
            durationSeconds = 0L
        )
        val missedDisplay = formatCallLogDisplay(missed, "me_1")
        assertEquals("Missed Call", missedDisplay.title)
        assertEquals("(Missed)", missedDisplay.subtitle)
        assertTrue(missedDisplay.isMissedOrDeclined)

        val outgoingVideo = com.example.model.CallSession(
            id = "c3",
            callerId = "me_1",
            receiverId = "peer_1",
            callType = com.example.model.CallType.VIDEO.value,
            status = com.example.model.CallStatus.ENDED.value,
            durationSeconds = 30L
        )
        val outgoingDisplay = formatCallLogDisplay(outgoingVideo, "me_1")
        assertEquals("Outgoing Video Call", outgoingDisplay.title)
        assertEquals("(30s)", outgoingDisplay.subtitle)
        assertTrue(outgoingDisplay.isOutgoing)
    }
}
