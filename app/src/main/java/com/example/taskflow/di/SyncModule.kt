package com.example.taskflow.di

import com.example.taskflow.data.SyncScheduler
import com.example.taskflow.data.sync.WorkSyncScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds abstract fun bindScheduler(scheduler: WorkSyncScheduler): SyncScheduler
}
