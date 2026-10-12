package com.example.taskflow.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.taskflow.data.AccountSession
import com.example.taskflow.data.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val session: AccountSession,
    private val status: SyncStatusStore
) : SyncScheduler {
    // Lazy lookup: WorkManager's custom factory also needs the data graph.
    private val workManager get() = WorkManager.getInstance(context)
    private val preferences = context.getSharedPreferences("sync-session", Context.MODE_PRIVATE)
    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun start(scope: CoroutineScope) {
        scope.launch {
            var previous = preferences.getString("account", null)
            session.uid.collect { uid ->
                try {
                    if (previous != uid) {
                        previous?.let { workManager.cancelAllWorkByTag(tag(it)) }
                        uid?.let { status.unblock(it) }
                    }
                    preferences.edit().putString("account", uid).apply()
                    previous = uid
                    if (!uid.isNullOrBlank() && uid != "USER" && !status.isBlocked(uid)) {
                        schedulePeriodic(uid)
                        requestSync(uid)
                    }
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    // One bad scheduling attempt must not kill the collector for later account changes.
                    android.util.Log.e("WorkSyncScheduler", "Could not schedule sync", error)
                }
            }
        }
    }

    override fun requestSync(uid: String) {
        if (session.currentUid() != uid || status.isBlocked(uid)) return
        status.queued(uid)
        val request = OneTimeWorkRequestBuilder<TaskSyncWorker>()
            .setInputData(workDataOf(ACCOUNT_KEY to uid))
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(tag(uid))
            .build()
        // A write during an active cycle must get another pass, not be discarded by KEEP.
        workManager.enqueueUniqueWork("sync-now:$uid", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    fun retry() {
        val uid = session.currentUid()?.takeIf { it.isNotBlank() && it != "USER" } ?: return
        status.unblock(uid)
        schedulePeriodic(uid)
        requestSync(uid)
    }

    private fun schedulePeriodic(uid: String) {
        val request = PeriodicWorkRequestBuilder<TaskSyncWorker>(15, TimeUnit.MINUTES)
            .setInputData(workDataOf(ACCOUNT_KEY to uid))
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(tag(uid))
            .build()
        workManager.enqueueUniquePeriodicWork(periodicName(uid), ExistingPeriodicWorkPolicy.KEEP, request)
    }

    companion object {
        const val ACCOUNT_KEY = "account_uid"
        fun tag(uid: String) = "account-sync:$uid"
        fun periodicName(uid: String) = "sync-periodic:$uid"
    }
}
