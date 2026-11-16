package com.example.notifications

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.ChatRepository
import com.example.model.CallSession
import com.example.model.CallStatus
import com.example.model.ChatThread
import com.example.model.PublicProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class LimonRealtimeSyncService : JobService() {

    private var syncJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters?): Boolean {
        val uid = Firebase.auth.currentUser?.uid ?: return false
        syncJob?.cancel()
        syncJob = scope.launch {
            try {
                withTimeoutOrNull(20_000L) {
                    RealtimeBackgroundSyncEngine.runSync(applicationContext)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Background sync error ($uid)", e)
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        syncJob?.cancel()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val TAG = "LimonRealtimeSync"
        private const val JOB_ID = 44091
        private const val JOB_ID_IMMEDIATE = 44092
        private const val SYNC_INTERVAL_MS = 15L * 60_000L

        fun schedule(context: Context) {
            val appCtx = context.applicationContext
            val scheduler = appCtx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
            val existing = scheduler.getPendingJob(JOB_ID)
            if (existing != null) return

            val component = ComponentName(appCtx, LimonRealtimeSyncService::class.java)
            val jobInfo = JobInfo.Builder(JOB_ID, component)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(SYNC_INTERVAL_MS)
                .setPersisted(true)
                .build()
            runCatching { scheduler.schedule(jobInfo) }
        }

        fun scheduleImmediateSync(context: Context) {
            val appCtx = context.applicationContext
            val scheduler = appCtx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
            val component = ComponentName(appCtx, LimonRealtimeSyncService::class.java)
            val jobInfo = JobInfo.Builder(JOB_ID_IMMEDIATE, component)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(1_500L)
                .setOverrideDeadline(10_000L)
                .build()
            runCatching { scheduler.schedule(jobInfo) }
        }
    }
}
