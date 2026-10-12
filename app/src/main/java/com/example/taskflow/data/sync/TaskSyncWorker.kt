package com.example.taskflow.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.taskflow.data.AccountSession
import com.example.taskflow.data.TaskSynchronizer
import javax.inject.Inject
import javax.inject.Singleton

class TaskSyncWorker(
    context: Context,
    parameters: WorkerParameters,
    private val session: AccountSession,
    private val synchronizer: TaskSynchronizer,
    private val status: SyncStatusStore
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val uid = inputData.getString(WorkSyncScheduler.ACCOUNT_KEY) ?: return Result.failure()
        if (session.currentUid() != uid || status.isBlocked(uid)) return Result.success()
        status.update(uid, SyncStatus(SyncPhase.RUNNING))
        val outcome = try {
            runAccountSync(uid, session) { synchronizer.sync(uid) }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            // Don't leave the UI stuck on "syncing" when the system stops the worker.
            status.update(uid, SyncStatus(SyncPhase.WAITING))
            throw cancelled
        }
        return when (outcome) {
            SyncOutcome.Obsolete -> Result.success()
            SyncOutcome.Success -> {
                status.update(uid, SyncStatus(SyncPhase.SUCCESS))
                Result.success()
            }
            is SyncOutcome.Retry -> {
                status.update(uid, SyncStatus(SyncPhase.ERROR, outcome.message))
                Result.retry()
            }
            is SyncOutcome.Failed -> {
                status.block(uid, outcome.message)
                WorkManager.getInstance(applicationContext).cancelUniqueWork(WorkSyncScheduler.periodicName(uid))
                Result.failure()
            }
        }
    }
}

@Singleton
class SyncWorkerFactory @Inject constructor(
    private val session: AccountSession,
    private val synchronizer: TaskSynchronizer,
    private val status: SyncStatusStore
) : WorkerFactory() {
    override fun createWorker(context: Context, workerClassName: String, parameters: WorkerParameters): ListenableWorker? =
        if (workerClassName == TaskSyncWorker::class.java.name) TaskSyncWorker(context, parameters, session, synchronizer, status) else null
}
