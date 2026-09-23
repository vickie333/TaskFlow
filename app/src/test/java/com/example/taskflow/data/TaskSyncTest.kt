package com.example.taskflow.data

import com.example.taskflow.data.remote.TaskDto
import com.example.taskflow.data.remote.TaskRemoteStore
import com.example.taskflow.data.remote.toDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TaskSyncTest {
    private val dao = MemoryTaskDao()
    private val remote = MemoryRemoteStore()
    private val sync = TaskSynchronizer(dao, remote)

    @Test fun downloadsTasksIntoRoom() = runBlocking {
        remote.tasks["task"] = TaskDto(description = "Remota", updatedAt = 20)
        sync.sync("alice")
        val task = dao.tasks.getValue("task")
        assertEquals("Remota", task.description)
        assertEquals("alice", task.userId)
        assertTrue(task.isSynced)
        assertNull(task.categoryId)
    }

    @Test fun uploadsPendingTasksAndAcknowledgesThem() = runBlocking {
        dao.insertTask(task("Local", 30))
        sync.sync("alice")
        assertEquals("Local", remote.tasks.getValue("task").description)
        assertTrue(dao.tasks.getValue("task").isSynced)
    }

    @Test fun newerRemoteWinsWithoutLosingLocalCategory() = runBlocking {
        dao.insertTask(task("Vieja", 10).copy(categoryId = 7))
        remote.tasks["task"] = TaskDto(description = "Nueva", updatedAt = 20)
        sync.sync("alice")
        assertEquals("Nueva", dao.tasks.getValue("task").description)
        assertEquals(7, dao.tasks.getValue("task").categoryId)
        assertEquals("Nueva", remote.tasks.getValue("task").description)
    }

    @Test fun localEditDuringUploadStaysPending() = runBlocking {
        dao.insertTask(task("Antes", 10))
        remote.afterExchange = { dao.tasks["task"] = task("Durante", 30) }
        sync.sync("alice")
        assertEquals("Durante", dao.tasks.getValue("task").description)
        assertFalse(dao.tasks.getValue("task").isSynced)
        assertEquals("Antes", remote.tasks.getValue("task").description)
    }

    @Test fun remoteDeletionHidesTaskAndKeepsTombstone() = runBlocking {
        dao.insertTask(task("Eliminar", 10))
        remote.tasks["task"] = TaskDto(updatedAt = 20, pendingDelete = true)
        sync.sync("alice")
        assertTrue(dao.tasks.getValue("task").pendingDelete)
        assertTrue(dao.tasks.getValue("task").isSynced)
    }

    @Test fun localDeletionIsPropagated() = runBlocking {
        dao.insertTask(task("Eliminar", 30).copy(pendingDelete = true))
        remote.tasks["task"] = TaskDto(updatedAt = 10)
        sync.sync("alice")
        assertTrue(remote.tasks.getValue("task").pendingDelete)
        assertTrue(dao.tasks.getValue("task").isSynced)
    }

    @Test fun failedUploadLeavesTaskPending() = runBlocking {
        dao.insertTask(task("Pendiente", 10))
        remote.fail = true
        try {
            sync.sync("alice")
            fail("Expected network failure")
        } catch (_: java.io.IOException) {
            assertFalse(dao.tasks.getValue("task").isSynced)
        }
    }

    @Test fun syncDoesNotUploadAnotherUsersTasks() = runBlocking {
        dao.insertTask(task("Privada", 10).copy(userId = "bob"))
        sync.sync("alice")
        assertTrue(remote.tasks.isEmpty())
        assertFalse(dao.tasks.getValue("task").isSynced)
    }

    @Test fun equalTimestampsConvergeToServerVersion() = runBlocking {
        dao.insertTask(task("Local", 10))
        remote.tasks["task"] = TaskDto(description = "Servidor", updatedAt = 10)
        sync.sync("alice")
        assertEquals("Servidor", dao.tasks.getValue("task").description)
        assertTrue(dao.tasks.getValue("task").isSynced)
    }

    @Test fun retryAfterFailureUploadsPendingChange() = runBlocking {
        dao.insertTask(task("Pendiente", 10))
        remote.fail = true
        try { sync.sync("alice") } catch (_: java.io.IOException) { }
        remote.fail = false
        sync.sync("alice")
        sync.sync("alice")
        assertEquals(1, remote.tasks.size)
        assertEquals("Pendiente", remote.tasks.getValue("task").description)
        assertTrue(dao.tasks.getValue("task").isSynced)
    }

    @Test fun missingRemoteDocumentDoesNotDeleteLocalTask() = runBlocking {
        dao.insertTask(task("Conservar", 10).copy(isSynced = true))
        sync.sync("alice")
        assertEquals("Conservar", dao.tasks.getValue("task").description)
        assertFalse(dao.tasks.getValue("task").pendingDelete)
    }

    @Test fun olderDownloadCannotResurrectDeletedTask() = runBlocking {
        dao.insertTask(task("Eliminada", 30).copy(isSynced = true, pendingDelete = true))
        remote.tasks["task"] = TaskDto(description = "Antigua", updatedAt = 10)
        sync.sync("alice")
        assertTrue(dao.tasks.getValue("task").pendingDelete)
    }

    @Test fun idCollisionCannotOverwriteAnotherUsersTask() = runBlocking {
        dao.insertTask(task("Privada", 10).copy(userId = "bob", isSynced = true))
        remote.tasks["task"] = TaskDto(description = "Otra", updatedAt = 30)
        try {
            sync.sync("alice")
            fail("Expected account collision to be rejected")
        } catch (_: IllegalStateException) {
            assertEquals("Privada", dao.tasks.getValue("task").description)
            assertEquals("bob", dao.tasks.getValue("task").userId)
        }
    }

    @Test fun equalTimestampsUseServerVersion() {
        val local = TaskDto(description = "Local", updatedAt = 10)
        val server = TaskDto(description = "Servidor", updatedAt = 10)
        assertEquals(server, chooseTaskVersion(local, server))
    }

    @Test fun newerLocalVersionWins() {
        val local = TaskDto(description = "Local", updatedAt = 20)
        val server = TaskDto(description = "Servidor", updatedAt = 10)
        assertEquals(local, chooseTaskVersion(local, server))
    }

    private fun task(description: String, time: Long) = Task(
        id = "task", description = description, userId = "alice", updatedAt = time
    )
}

private class MemoryRemoteStore : TaskRemoteStore {
    val tasks = mutableMapOf<String, TaskDto>()
    var fail = false
    var afterExchange: () -> Unit = {}
    override suspend fun exchange(uid: String, task: Task): TaskDto {
        if (fail) throw java.io.IOException("Offline")
        val winner = chooseTaskVersion(task.toDto(), tasks[task.id])
        tasks[task.id] = winner
        afterExchange()
        return winner
    }
    override suspend fun fetchAll(uid: String): Map<String, TaskDto> = tasks.toMap()
}

private class MemoryTaskDao : TaskDao {
    val tasks = mutableMapOf<String, Task>()
    override fun getAllTasks(uid: String): Flow<List<Task>> =
        flowOf(tasks.values.filter { it.userId == uid && !it.pendingDelete })
    override suspend fun getUnsyncedTasks(uid: String) =
        tasks.values.filter { it.userId == uid && !it.isSynced }
    override suspend fun getTaskById(taskId: String) = tasks[taskId]
    override suspend fun markAsSynced(taskId: String, uploadedAt: Long): Int {
        val task = tasks[taskId] ?: return 0
        if (task.updatedAt != uploadedAt) return 0
        tasks[taskId] = task.copy(isSynced = true)
        return 1
    }
    override suspend fun insertTask(task: Task) { tasks[task.id] = task }
    override suspend fun editTask(task: Task) { tasks[task.id] = task }
    override suspend fun deleteTask(task: Task) { tasks.remove(task.id) }
}
