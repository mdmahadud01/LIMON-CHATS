package com.example

import android.Manifest
import android.app.Application
import android.app.Service
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.notifications.CallForegroundService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class CallForegroundServiceTest {

    @Before
    fun setUp() {
        CallForegroundService.resetStateForTesting()
    }

    @Test
    fun start_withoutRecordAudioPermission_doesNotStartService() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).denyPermissions(Manifest.permission.RECORD_AUDIO)

        CallForegroundService.start(app, callId = "call_1", peerName = "Alice", callType = "voice")

        val startedIntent = Shadows.shadowOf(app).peekNextStartedService()
        assertNull("CallForegroundService must not start when RECORD_AUDIO is denied", startedIntent)
    }

    @Test
    fun onStartCommand_withoutRecordAudioPermission_stopsSafelyWithoutCrash() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).denyPermissions(Manifest.permission.RECORD_AUDIO)

        val controller = Robolectric.buildService(CallForegroundService::class.java, Intent())
        val service = controller.create().get()
        val startResult = service.onStartCommand(Intent(), 0, 1)

        assertEquals(Service.START_NOT_STICKY, startResult)
    }

    @Test
    fun start_withRecordAudioPermission_enqueuesForegroundService() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)

        CallForegroundService.start(app, callId = "call_2", peerName = "Bob", callType = "video")

        val startedIntent = Shadows.shadowOf(app).peekNextStartedService()
        assertNotNull("CallForegroundService should start when RECORD_AUDIO is granted", startedIntent)
        assertEquals(CallForegroundService::class.java.name, startedIntent?.component?.className)
    }

    @Test
    fun stop_immediatelyAfterStart_defersUntilStartForegroundRuns() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)

        CallForegroundService.start(app, callId = "call_3", peerName = "Cara", callType = "voice")
        val startedIntent = Shadows.shadowOf(app).nextStartedService
        assertNotNull(startedIntent)

        CallForegroundService.stop(app)
        assertNull(Shadows.shadowOf(app).nextStoppedService)

        val controller = Robolectric.buildService(CallForegroundService::class.java, startedIntent)
        val service = controller.create().get()
        val result = service.onStartCommand(startedIntent, 0, 1)

        assertEquals(Service.START_NOT_STICKY, result)
        assertEquals(
            "startForeground() must have been called before stopping to prevent ForegroundServiceDidNotStartInTimeException",
            1,
            CallForegroundService.startForegroundCallCount.get()
        )
        assertTrue(Shadows.shadowOf(service).isForegroundStopped)
        assertTrue(Shadows.shadowOf(service).isStoppedBySelf)
    }
}
