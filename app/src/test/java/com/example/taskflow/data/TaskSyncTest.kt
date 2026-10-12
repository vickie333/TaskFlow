package com.example.taskflow.data

import com.example.taskflow.data.remote.TaskDto
import com.example.taskflow.data.remote.TaskRemoteStore
import com.example.taskflow.data.remote.toDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35])
class TaskSyncTest {
    private val db = androidx.room.Room.inMemoryDatabaseBuilder(org.robolectric.RuntimeEnvironment.getApplication(), AppDatabase::class.java).allowMainThreadQueries().build()
    private val session = TestSession()
    private val categories = MemoryCategoryStore()
    @org.junit.After fun close() { db.close() }

    private val dao = db.taskDao()
    private val remote = MemoryRemoteStore()
    private val sync = TaskSynchronizer(db, remote, categories, session)

    @Test fun downloadsTasksIntoRoom() = runBlocking {
        remote.tasks["task"] = TaskDto(description = "Remota", updatedAt = 20)
        sync.sync("alice")
        val task = requireNotNull(dao.getTaskById("task"))
        assertEquals("Remota", task.description)
        assertEquals("alice", task.userId)
        assertTrue(task.isSynced)
        assertNull(task.categoryId)
    }

    @Test fun uploadsPendingTasksAndAcknowledgesThem() = runBlocking {
        dao.insertTask(task("Local", 30))
        sync.sync("alice")
        assertEquals("Local", remote.tasks.getValue("task").description)
        assertTrue(requireNotNull(dao.getTaskById("task")).isSynced)
    }

    @Test fun categoriesSyncBeforeTaskReferences() = runBlocking {
        categories.values["category"] = com.example.taskflow.data.remote.CategoryDto("Work", 20)
        remote.tasks["task"] = TaskDto(description = "Nueva", updatedAt = 20, categoryId = "category")
        sync.sync("alice")
        assertEquals("category", dao.getTaskById("task")?.categoryId)
        assertEquals("Work", db.categoryDao().getCategoryById("category")?.name)
    }

    @Test fun localEditDuringUploadStaysPending() = runBlocking {
        dao.insertTask(task("Antes", 10))
        remote.afterExchange = { dao.editTask(task("Durante", 30)) }
        sync.sync("alice")
        assertEquals("Durante", requireNotNull(dao.getTaskById("task")).description)
        assertFalse(requireNotNull(dao.getTaskById("task")).isSynced)
        assertEquals("Antes", remote.tasks.getValue("task").description)
    }

    @Test fun remoteDeletionHidesTaskAndKeepsTombstone() = runBlocking {
        dao.insertTask(task("Eliminar", 10))
        remote.tasks["task"] = TaskDto(updatedAt = 20, pendingDelete = true)
        sync.sync("alice")
        assertTrue(requireNotNull(dao.getTaskById("task")).pendingDelete)
        assertTrue(requireNotNull(dao.getTaskById("task")).isSynced)
    }

    @Test fun localDeletionIsPropagated() = runBlocking {
        dao.insertTask(task("Eliminar", 30).copy(pendingDelete = true))
        remote.tasks["task"] = TaskDto(updatedAt = 10)
        sync.sync("alice")
        assertTrue(remote.tasks.getValue("task").pendingDelete)
        assertTrue(requireNotNull(dao.getTaskById("task")).isSynced)
    }

    @Test fun failedUploadLeavesTaskPending() = runBlocking {
        dao.insertTask(task("Pendiente", 10))
        remote.fail = true
        try {
            sync.sync("alice")
            fail("Expected network failure")
        } catch (_: java.io.IOException) {
            assertFalse(requireNotNull(dao.getTaskById("task")).isSynced)
        }
    }

    @Test fun syncDoesNotUploadAnotherUsersTasks() = runBlocking {
        dao.insertTask(task("Privada", 10).copy(userId = "bob"))
        sync.sync("alice")
        assertTrue(remote.tasks.isEmpty())
        assertFalse(requireNotNull(dao.getTaskById("task")).isSynced)
    }

    @Test fun equalTimestampsConvergeToServerVersion() = runBlocking {
        dao.insertTask(task("Local", 10))
        remote.tasks["task"] = TaskDto(description = "Servidor", updatedAt = 10)
        sync.sync("alice")
        assertEquals("Servidor", requireNotNull(dao.getTaskById("task")).description)
        assertTrue(requireNotNull(dao.getTaskById("task")).isSynced)
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
        assertTrue(requireNotNull(dao.getTaskById("task")).isSynced)
    }

    @Test fun missingRemoteDocumentDoesNotDeleteLocalTask() = runBlocking {
        dao.insertTask(task("Conservar", 10).copy(isSynced = true))
        sync.sync("alice")
        assertEquals("Conservar", requireNotNull(dao.getTaskById("task")).description)
        assertFalse(requireNotNull(dao.getTaskById("task")).pendingDelete)
    }

    @Test fun olderDownloadCannotResurrectDeletedTask() = runBlocking {
        dao.insertTask(task("Eliminada", 30).copy(isSynced = true, pendingDelete = true))
        remote.tasks["task"] = TaskDto(description = "Antigua", updatedAt = 10)
        sync.sync("alice")
        assertTrue(requireNotNull(dao.getTaskById("task")).pendingDelete)
    }

    @Test fun idCollisionCannotOverwriteAnotherUsersTask() = runBlocking {
        dao.insertTask(task("Privada", 10).copy(userId = "bob", isSynced = true))
        remote.tasks["task"] = TaskDto(description = "Otra", updatedAt = 30)
        try {
            sync.sync("alice")
            fail("Expected account collision to be rejected")
        } catch (_: IllegalStateException) {
            assertEquals("Privada", requireNotNull(dao.getTaskById("task")).description)
            assertEquals("bob", requireNotNull(dao.getTaskById("task")).userId)
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

    @Test fun pendingCategoryPreventsTaskPullFromLosingLocalAssignment() = runBlocking {
        val category = Category(id = "category", name = "Before", userId = "alice", updatedAt = 10)
        db.categoryDao().insertCategory(category)
        val pending = task("Pending", 20).copy(categoryId = category.id)
        dao.insertTask(pending)
        remote.tasks["task"] = TaskDto(description = "Server", updatedAt = 100)
        categories.afterExchange = {
            db.categoryDao().editCategory(category.copy(name = "During", updatedAt = 30))
        }
        // A skipped task is not a success: the worker must retry once the category has synced.
        assertThrows(java.io.IOException::class.java) { runBlocking { sync.sync("alice") } }
        assertEquals(pending, dao.getTaskById("task"))
        assertEquals("Server", remote.tasks.getValue("task").description)
        categories.afterExchange = {}
        sync.sync("alice")
        assertTrue(requireNotNull(db.categoryDao().getCategoryById(category.id)).isSynced)
        assertTrue(requireNotNull(dao.getTaskById("task")).isSynced)
    }

    @Test fun taskEditDuringCategoryExchangeIsProtectedByCycleSnapshot() = runBlocking {
        val category = Category(id = "category", name = "Work", userId = "alice", updatedAt = 10)
        db.categoryDao().insertCategory(category)
        dao.insertTask(task("Before", 10))
        val edited = task("During", 30)
        categories.afterExchange = { dao.editTask(edited) }
        remote.tasks["task"] = TaskDto(description = "Server", updatedAt = 100)
        sync.sync("alice")
        assertEquals(edited, dao.getTaskById("task"))
    }

    private fun task(description: String, time: Long) = Task(
        id = "task", description = description, userId = "alice", updatedAt = time
    )
}

internal class MemoryRemoteStore : TaskRemoteStore {
    val tasks = mutableMapOf<String, TaskDto>()
    var fail = false
    var afterExchange: suspend () -> Unit = {}
    override suspend fun exchange(uid: String, task: Task): TaskDto {
        if (fail) throw java.io.IOException("Offline")
        val winner = chooseTaskVersion(task.toDto(), tasks[task.id])
        tasks[task.id] = winner
        afterExchange()
        return winner
    }
    override suspend fun fetchAll(uid: String): Map<String, TaskDto> = tasks.toMap()
}

internal class TestSession : AccountSession {
    override val uid = kotlinx.coroutines.flow.MutableStateFlow<String?>("alice")
}
internal class MemoryCategoryStore : com.example.taskflow.data.remote.CategoryRemoteStore {
    val values = mutableMapOf<String, com.example.taskflow.data.remote.CategoryDto>()
    var afterExchange: suspend () -> Unit = {}
    override suspend fun fetchAll(uid: String) = values.toMap()
    override suspend fun exchange(uid: String, category: Category): com.example.taskflow.data.remote.CategoryDto {
        val local = com.example.taskflow.data.remote.CategoryDto(category.name, category.updatedAt, category.pendingDelete)
        val remote = values[category.id]
        val winner = if (remote == null || local.updatedAt > remote.updatedAt) local else remote
        values[category.id] = winner
        afterExchange()
        return winner
    }
}
