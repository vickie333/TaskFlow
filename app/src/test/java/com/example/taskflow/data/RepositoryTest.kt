package com.example.taskflow.data

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java).allowMainThreadQueries().build()
    private val session = TestSession()
    private val scheduled = mutableListOf<String>()
    private val scheduler = object : SyncScheduler { override fun requestSync(uid: String) { scheduled += uid } }
    private val repository = CategoryRepository(db, session, scheduler)
    @After fun close() { db.close() }

    @Test fun persistedOwnershipRejectsForgedCategory() = runBlocking {
        val category = Category(name = "Private", userId = "bob")
        db.categoryDao().insertCategory(category)
        try {
            repository.editCategory(category.copy(userId = "alice", name = "Forged"))
            fail("Should reject persisted owner")
        } catch (_: IllegalArgumentException) { }
        assertEquals("Private", db.categoryDao().getCategoryById(category.id)?.name)
        assertTrue(scheduled.isEmpty())
    }
    @Test fun categoryWithActiveTasksCannotBeDeleted() = runBlocking {
        val category = Category(name = "Work", userId = "alice")
        db.categoryDao().insertCategory(category)
        db.taskDao().insertTask(Task(description = "Task", userId = "alice", categoryId = category.id))
        try { repository.removeCategory(category); fail("Should reject active tasks") } catch (_: IllegalArgumentException) { }
        assertFalse(requireNotNull(db.categoryDao().getCategoryById(category.id)).pendingDelete)
    }
    @Test fun recoveryPreservesSharedRelationshipsAndIsIdempotent() = runBlocking {
        val category = Category(name = "Legacy", userId = "USER")
        db.categoryDao().insertCategory(category)
        val tasks = listOf(Task(description = "One", userId = "USER", categoryId = category.id), Task(description = "Two", userId = "", categoryId = category.id))
        tasks.forEach { db.taskDao().insertTask(it) }
        val recovery = LegacyRecoveryRepository(db, session, scheduler)
        assertEquals(2, recovery.recover())
        assertEquals(0, recovery.recover())
        val recovered = db.taskDao().getTasks("alice")
        assertEquals(setOf("One", "Two"), recovered.map { it.description }.toSet())
        assertEquals(1, recovered.map { it.categoryId }.distinct().size)
        assertTrue(recovered.none { it.id in tasks.map { old -> old.id } })
        assertTrue(db.taskDao().getLegacyTasks().isEmpty())
        assertEquals("alice", db.categoryDao().getCategoryById(requireNotNull(recovered.first().categoryId))?.userId)
    }
    @Test fun foreignCategoryRecoveryRollsBackAndDoesNotSchedule() = runBlocking {
        val category = Category(name = "Bob private", userId = "bob")
        db.categoryDao().insertCategory(category)
        val legacy = Task(description = "Legacy", userId = "USER", categoryId = category.id)
        db.taskDao().insertTask(legacy)
        try {
            LegacyRecoveryRepository(db, session, scheduler).recover()
            fail("Foreign category must reject recovery")
        } catch (_: IllegalArgumentException) { }
        assertEquals(listOf(legacy), db.taskDao().getLegacyTasks())
        assertEquals(category, db.categoryDao().getCategoryById(category.id))
        assertTrue(db.taskDao().getTasks("alice").isEmpty())
        assertTrue(db.categoryDao().getCategories("alice").isEmpty())
        assertTrue(scheduled.isEmpty())
    }

    private fun taskRepository() = TaskRepository(db, session, scheduler,
        TaskSynchronizer(db, MemoryRemoteStore(), MemoryCategoryStore(), session))

    @Test fun taskMutationsRejectForeignPersistedOwnerAndCategory() = runBlocking {
        val foreign = Task(description = "Private", userId = "bob")
        val own = Task(description = "Own", userId = "alice")
        val category = Category(name = "Private category", userId = "bob")
        db.taskDao().insertTask(foreign)
        db.taskDao().insertTask(own)
        db.categoryDao().insertCategory(category)
        val tasks = taskRepository()
        try { tasks.editTask(foreign.copy(userId = "alice")); fail("Reject forged owner") }
        catch (_: IllegalArgumentException) { }
        try { tasks.editTask(own.copy(categoryId = category.id)); fail("Reject foreign category") }
        catch (_: IllegalArgumentException) { }
        try { tasks.addTask("New", Priority.MEDIA, category.id); fail("Reject foreign category on insert") }
        catch (_: IllegalArgumentException) { }
        assertEquals(foreign, db.taskDao().getTaskById(foreign.id))
        assertEquals(listOf(own), db.taskDao().getTasks("alice"))
        assertTrue(scheduled.isEmpty())
    }

    @Test fun liveRepositoriesSwitchFromAliceToBobWithoutDatabaseWrite() = runBlocking {
        val alice = Task(description = "Alice", userId = "alice")
        val bob = Task(description = "Bob", userId = "bob")
        val aliceCategory = Category(name = "Alice", userId = "alice")
        val bobCategory = Category(name = "Bob", userId = "bob")
        db.taskDao().insertTask(alice)
        db.taskDao().insertTask(bob)
        db.categoryDao().insertCategory(aliceCategory)
        db.categoryDao().insertCategory(bobCategory)
        val taskEmissions = Channel<List<Task>>(Channel.UNLIMITED)
        val categoryEmissions = Channel<List<Category>>(Channel.UNLIMITED)
        val taskJob = launch { taskRepository().getAllTasks().collect { taskEmissions.send(it) } }
        val categoryJob = launch { repository.getAllCategories().collect { categoryEmissions.send(it) } }
        try {
            withTimeout(10_000) {
                assertEquals(listOf(alice), taskEmissions.receive())
                assertEquals(listOf(aliceCategory), categoryEmissions.receive())
                session.uid.value = "bob"
                assertEquals(listOf(bob), taskEmissions.receive())
                assertEquals(listOf(bobCategory), categoryEmissions.receive())
                session.uid.value = null
                assertTrue(taskEmissions.receive().isEmpty())
                assertTrue(categoryEmissions.receive().isEmpty())
            }
        } finally { taskJob.cancel(); categoryJob.cancel() }
    }

    @Test fun localClockMovesPastFutureRevision() = runBlocking {
        val category = Category(name = "Future", userId = "alice", updatedAt = Long.MAX_VALUE - 10)
        db.categoryDao().insertCategory(category)
        repository.editCategory(category.copy(name = "Changed"))
        assertEquals(category.updatedAt + 1, db.categoryDao().getCategoryById(category.id)?.updatedAt)
    }
}
