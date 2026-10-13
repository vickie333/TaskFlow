package com.example.taskflow.data

import com.example.taskflow.data.remote.CategoryDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35])
class CategorySyncTest {
    private val db = androidx.room.Room.inMemoryDatabaseBuilder(org.robolectric.RuntimeEnvironment.getApplication(), AppDatabase::class.java).allowMainThreadQueries().build()
    private val session = TestSession()
    private val categories = MemoryCategoryStore()
    private val sync = TaskSynchronizer(db, MemoryRemoteStore(), categories, session)
    private val dao = db.categoryDao()
    @org.junit.After fun close() { db.close() }

    @Test fun remoteDeletionHidesCategoryAndKeepsTombstone() = runBlocking {
        dao.insertCategory(category("Work", 10, isSynced = true))
        categories.values["c1"] = CategoryDto("Work", 20, pendingDelete = true)
        sync.sync("alice")
        val local = requireNotNull(dao.getCategoryById("c1"))
        assertTrue(local.pendingDelete)
        assertTrue(local.isSynced)
    }

    @Test fun localDeletionIsPropagated() = runBlocking {
        dao.insertCategory(category("Work", 30, pendingDelete = true))
        categories.values["c1"] = CategoryDto("Work", 10)
        sync.sync("alice")
        assertTrue(categories.values.getValue("c1").pendingDelete)
        assertTrue(requireNotNull(dao.getCategoryById("c1")).isSynced)
    }

    @Test fun olderDownloadCannotResurrectDeletedCategory() = runBlocking {
        dao.insertCategory(category("Work", 30, isSynced = true, pendingDelete = true))
        categories.values["c1"] = CategoryDto("Work", 10)
        sync.sync("alice")
        assertTrue(requireNotNull(dao.getCategoryById("c1")).pendingDelete)
    }

    @Test fun newerLocalRenameWinsOverRemote() = runBlocking {
        dao.insertCategory(category("Local", 30))
        categories.values["c1"] = CategoryDto("Remote", 10)
        sync.sync("alice")
        assertEquals("Local", categories.values.getValue("c1").name)
        assertEquals("Local", requireNotNull(dao.getCategoryById("c1")).name)
    }

    @Test fun newerRemoteRenameOverwritesSyncedLocal() = runBlocking {
        dao.insertCategory(category("Local", 10, isSynced = true))
        categories.values["c1"] = CategoryDto("Remote", 30)
        sync.sync("alice")
        assertEquals("Remote", requireNotNull(dao.getCategoryById("c1")).name)
    }

    @Test fun equalTimestampsConvergeToServerVersion() = runBlocking {
        dao.insertCategory(category("Local", 10))
        categories.values["c1"] = CategoryDto("Server", 10)
        sync.sync("alice")
        assertEquals("Server", requireNotNull(dao.getCategoryById("c1")).name)
        assertTrue(requireNotNull(dao.getCategoryById("c1")).isSynced)
    }

    @Test fun accountSwitchDuringSyncAbortsWithoutTouchingTheNewAccount() = runBlocking {
        dao.insertCategory(category("Work", 10))
        categories.values["other"] = CategoryDto("Remote", 20)
        categories.afterExchange = { session.uid.value = "bob" }
        assertThrows(IllegalStateException::class.java) { runBlocking { sync.sync("alice") } }
        assertNull(dao.getCategoryById("other"))
        assertTrue(dao.getCategories("bob").isEmpty())
        assertFalse(requireNotNull(dao.getCategoryById("c1")).isSynced)
    }

    @Test fun anotherUsersCategoryIsNeverUploaded() = runBlocking {
        dao.insertCategory(category("Privada", 10).copy(userId = "bob"))
        sync.sync("alice")
        assertTrue(categories.values.isEmpty())
    }

    private fun category(name: String, time: Long, isSynced: Boolean = false, pendingDelete: Boolean = false) =
        Category(id = "c1", name = name, userId = "alice", updatedAt = time, isSynced = isSynced, pendingDelete = pendingDelete)
}
