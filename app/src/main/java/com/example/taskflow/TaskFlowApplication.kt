package com.example.taskflow

import android.app.Application
import androidx.work.Configuration
import com.example.taskflow.data.sync.SyncWorkerFactory
import com.example.taskflow.data.sync.WorkSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltAndroidApp
class TaskFlowApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: SyncWorkerFactory
    @Inject lateinit var scheduler: WorkSyncScheduler
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        scheduler.start(applicationScope)
    }
}
