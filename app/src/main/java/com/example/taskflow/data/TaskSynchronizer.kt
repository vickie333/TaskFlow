package com.example.taskflow.data

import com.example.taskflow.data.remote.TaskDto
import com.example.taskflow.data.remote.TaskRemoteStore
import com.example.taskflow.data.remote.toTask
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

// On equal timestamps, the committed server version is authoritative.
internal fun chooseTaskVersion(local: TaskDto, remote: TaskDto?): TaskDto =
    if (remote == null || local.updatedAt > remote.updatedAt) local else remote

@Singleton
class TaskSynchronizer @Inject constructor(
    private val dao: TaskDao,
    private val remote: TaskRemoteStore
) {
    private val mutex = Mutex()

    suspend fun sync(uid: String) = mutex.withLock {
        require(uid.isNotBlank()) { "Se necesita una sesión para sincronizar" }
        for (task in dao.getUnsyncedTasks(uid)) {
            val confirmed = remote.exchange(uid, task)
            dao.applyRemoteTask(confirmed.toTask(task.id, uid, null))
        }
        // A server read must succeed: an offline cache is not a completed pull.
        for ((id, dto) in remote.fetchAll(uid)) {
            dao.applyRemoteTask(dto.toTask(id, uid, null))
        }
    }
}
