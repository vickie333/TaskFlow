package com.example.taskflow.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35])
class TaskRepositoryTest {
    private val db = androidx.room.Room.inMemoryDatabaseBuilder(org.robolectric.RuntimeEnvironment.getApplication(), AppDatabase::class.java).allowMainThreadQueries().build()
    private val session = TestSession()
    private val scheduled = mutableListOf<String>()
    private val scheduler = object : SyncScheduler { override fun requestSync(uid: String) { scheduled += uid } }
    private val repository = TaskRepository(db, session, scheduler, TaskSynchronizer(db, MemoryRemoteStore(), MemoryCategoryStore(), session))
    @org.junit.After fun close() { db.close() }

    @Test fun removeThenRestoreBringsTheTaskBackAsPendingSync() = runBlocking {
        db.taskDao().insertTask(Task(id = "t", description = "Hacer", userId = "alice", updatedAt = 10, isSynced = true))
        val task = requireNotNull(db.taskDao().getTaskById("t"))
        repository.removeTask(task)
        val deleted = requireNotNull(db.taskDao().getTaskById("t"))
        assertTrue(deleted.pendingDelete)
        repository.restoreTask(deleted)
        val restored = requireNotNull(db.taskDao().getTaskById("t"))
        assertFalse(restored.pendingDelete)
        assertFalse(restored.isSynced)
        assertTrue(restored.updatedAt > deleted.updatedAt)
        assertEquals(2, scheduled.size)
    }

    @Test fun restoreDropsACategoryDeletedInTheMeantime() = runBlocking {
        db.categoryDao().insertCategory(Category(id = "c", name = "Work", userId = "alice", isSynced = true))
        db.taskDao().insertTask(Task(id = "t", description = "Hacer", userId = "alice", categoryId = "c", pendingDelete = true))
        db.categoryDao().editCategory(requireNotNull(db.categoryDao().getCategoryById("c")).copy(pendingDelete = true))
        repository.restoreTask(requireNotNull(db.taskDao().getTaskById("t")))
        assertNull(requireNotNull(db.taskDao().getTaskById("t")).categoryId)
    }

    @Test fun restoreRejectsATaskThatIsNotDeleted() = runBlocking {
        db.taskDao().insertTask(Task(id = "t", description = "Viva", userId = "alice"))
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.restoreTask(requireNotNull(db.taskDao().getTaskById("t"))) } }
        Unit
    }

    @Test fun restoreRejectsAnotherUsersTask() = runBlocking {
        db.taskDao().insertTask(Task(id = "t", description = "Ajena", userId = "bob", pendingDelete = true))
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.restoreTask(requireNotNull(db.taskDao().getTaskById("t"))) } }
        assertTrue(requireNotNull(db.taskDao().getTaskById("t")).pendingDelete)
    }
}

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35])
class SizeLimitsTest {
    private val db = androidx.room.Room.inMemoryDatabaseBuilder(org.robolectric.RuntimeEnvironment.getApplication(), AppDatabase::class.java).allowMainThreadQueries().build()
    private val session = TestSession()
    private val scheduler = object : SyncScheduler { override fun requestSync(uid: String) = Unit }
    private val tasks = TaskRepository(db, session, scheduler, TaskSynchronizer(db, MemoryRemoteStore(), MemoryCategoryStore(), session))
    private val categories = CategoryRepository(db, session, scheduler)
    @org.junit.After fun close() { db.close() }

    @Test fun descriptionAtTheLimitIsAcceptedAndOneOverIsRejected() = runBlocking {
        tasks.addTask("a".repeat(MAX_DESCRIPTION_LENGTH), Priority.MEDIA, null)
        assertThrows(IllegalArgumentException::class.java) { runBlocking { tasks.addTask("a".repeat(MAX_DESCRIPTION_LENGTH + 1), Priority.MEDIA, null) } }
        assertEquals(1, db.taskDao().getTasks("alice").size)
        Unit
    }

    @Test fun limitIgnoresSurroundingWhitespace() = runBlocking {
        tasks.addTask("  " + "a".repeat(MAX_DESCRIPTION_LENGTH) + "  ", Priority.MEDIA, null)
        assertEquals(1, db.taskDao().getTasks("alice").size)
    }

    @Test fun categoryNameLimitIsEnforced() = runBlocking {
        categories.addCategory("n".repeat(MAX_CATEGORY_NAME_LENGTH))
        assertThrows(IllegalArgumentException::class.java) { runBlocking { categories.addCategory("n".repeat(MAX_CATEGORY_NAME_LENGTH + 1)) } }
        assertEquals(1, db.categoryDao().getCategories("alice").size)
        Unit
    }
}
