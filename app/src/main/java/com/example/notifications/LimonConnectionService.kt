package com.example.notifications

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

/**
 * Compatibility coordinator that replaces the old persistent foreground sync service.
 * Ensures the "Connected for real-time messages & calls" ongoing notification is removed
 * while keeping background sync jobs scheduled via JobScheduler & FCM.
 */
object LimonConnectionService {

    fun startIfSignedIn(context: Context) {
        val appCtx = context.applicationContext
        NotificationHelper.cancelNotification(appCtx, NotificationHelper.REALTIME_SYNC_NOTIF_ID)
        if (Firebase.auth.currentUser == null) return
        LimonRealtimeSyncService.schedule(appCtx)
        LimonRealtimeSyncService.scheduleImmediateSync(appCtx)
    }

    fun stop(context: Context) {
        val appCtx = context.applicationContext
        NotificationHelper.cancelNotification(appCtx, NotificationHelper.REALTIME_SYNC_NOTIF_ID)
    }
}
