package com.example.taskflow.data

import com.example.taskflow.data.remote.CategoryDto
import com.example.taskflow.data.remote.TaskDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35])
class TombstonePurgeTest {
    private val db = androidx.room.Room.inMemoryDatabaseBuilder(org.robolectric.RuntimeEnvironment.getApplication(), AppDatabase::class.java).allowMainThreadQueries().build()
    private val session = TestSession()
    private val remote = MemoryRemoteStore()
    private val categories = MemoryCategoryStore()
    private val now = 1_000L * TOMBSTONE_TTL_MS
    private val old = now - TOMBSTONE_TTL_MS - 1
    private val recent = now - TOMBSTONE_TTL_MS + 1_000
    private val sync = TaskSynchronizer(db, remote, categories, session).apply { clock = { now } }
    @org.junit.After fun close() { db.close() }

    private fun task(id: String, time: Long, isSynced: Boolean = true, pendingDelete: Boolean = true) =
        Task(id = id, description = id, userId = "alice", updatedAt = time, isSynced = isSynced, pendingDelete = pendingDelete)

    @Test fun expiredSyncedTombstoneIsRemovedLocallyAndInTheCloud() = runBlocking {
        db.taskDao().insertTask(task("t", old))
        remote.tasks["t"] = TaskDto(description = "t", updatedAt = old, pendingDelete = true)
        sync.sync("alice")
        assertNull(db.taskDao().getTaskById("t"))
        assertFalse(remote.tasks.containsKey("t"))
    }

    @Test fun recentTombstoneIsKept() = runBlocking {
        db.taskDao().insertTask(task("t", recent))
        remote.tasks["t"] = TaskDto(description = "t", updatedAt = recent, pendingDelete = true)
        sync.sync("alice")
        assertNotNull(db.taskDao().getTaskById("t"))
        assertTrue(remote.tasks.containsKey("t"))
    }

    @Test fun tombstoneUploadedInThisCycleIsPurgedOnlyOnTheNextOne() = runBlocking {
        db.taskDao().insertTask(task("t", old, isSynced = false))
        sync.sync("alice")
        // The delete is uploaded first so the other devices can learn about it; the purge waits for a later cycle.
        assertTrue(remote.tasks.getValue("t").pendingDelete)
        assertNotNull(db.taskDao().getTaskById("t"))
        sync.sync("alice")
        assertNull(db.taskDao().getTaskById("t"))
        assertFalse(remote.tasks.containsKey("t"))
    }

    @Test fun liveTasksAreNeverPurged() = runBlocking {
        db.taskDao().insertTask(task("t", old, pendingDelete = false))
        remote.tasks["t"] = TaskDto(description = "t", updatedAt = old)
        sync.sync("alice")
        assertNotNull(db.taskDao().getTaskById("t"))
        assertTrue(remote.tasks.containsKey("t"))
    }

    @Test fun cloudDocumentRestoredByAnotherDeviceSurvivesThePurge() = runBlocking {
        db.taskDao().insertTask(task("t", old))
        // Another device restored it after our last fetch: the cloud copy is live and newer.
        remote.tasks["t"] = TaskDto(description = "t", updatedAt = now, pendingDelete = false)
        sync.sync("alice")
        assertTrue(remote.tasks.containsKey("t"))
        assertFalse(requireNotNull(db.taskDao().getTaskById("t")).pendingDelete)
    }

    @Test fun purgeFailureDoesNotFailTheSyncAndKeepsTheTombstone() = runBlocking {
        db.taskDao().insertTask(task("t", old))
        remote.tasks["t"] = TaskDto(description = "t", updatedAt = old, pendingDelete = true)
        remote.failPurge = true
        sync.sync("alice")
        assertNotNull(db.taskDao().getTaskById("t"))
        remote.failPurge = false
        sync.sync("alice")
        assertNull(db.taskDao().getTaskById("t"))
    }

    @Test fun categoryTombstoneWaitsForTasksThatStillReferenceIt() = runBlocking {
        db.categoryDao().insertCategory(Category(id = "c", name = "Work", userId = "alice", updatedAt = old, isSynced = true, pendingDelete = true))
        categories.values["c"] = CategoryDto("Work", old, pendingDelete = true)
        db.taskDao().insertTask(task("t", recent).copy(categoryId = "c"))
        remote.tasks["t"] = TaskDto(description = "t", updatedAt = recent, pendingDelete = true, categoryId = "c")
        sync.sync("alice")
        assertNotNull(db.categoryDao().getCategoryById("c"))
        assertTrue(categories.values.containsKey("c"))
    }

    @Test fun expiredCategoryTombstoneIsPurgedOnceNothingReferencesIt() = runBlocking {
        db.categoryDao().insertCategory(Category(id = "c", name = "Work", userId = "alice", updatedAt = old, isSynced = true, pendingDelete = true))
        categories.values["c"] = CategoryDto("Work", old, pendingDelete = true)
        sync.sync("alice")
        assertNull(db.categoryDao().getCategoryById("c"))
        assertFalse(categories.values.containsKey("c"))
    }

    @Test fun anotherUsersTombstoneIsNotPurged() = runBlocking {
        db.taskDao().insertTask(task("t", old).copy(userId = "bob"))
        sync.sync("alice")
        assertNotNull(db.taskDao().getTaskById("t"))
    }
}
