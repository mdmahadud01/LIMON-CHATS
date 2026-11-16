package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LimonBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val appCtx = context.applicationContext
            NotificationHelper.ensureChannels(appCtx)
            LimonRealtimeSyncService.schedule(appCtx)
            LimonRealtimeSyncService.scheduleImmediateSync(appCtx)
            LimonBackgroundSyncWorker.schedulePeriodic(appCtx)
            LimonBackgroundSyncWorker.enqueueImmediateSync(appCtx)
        }
    }
}
