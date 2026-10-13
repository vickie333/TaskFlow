package com.example.taskflow.ui.task

import com.example.taskflow.data.*
import com.example.taskflow.ui.category.CategoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35])
class AccountSwitchTest {
    private val db = androidx.room.Room.inMemoryDatabaseBuilder(org.robolectric.RuntimeEnvironment.getApplication(), AppDatabase::class.java).allowMainThreadQueries().build()
    private val session = TestSession()
    private val scheduler = object : SyncScheduler { override fun requestSync(uid: String) = Unit }
    private lateinit var tasks: TaskViewModel
    private lateinit var categories: CategoryViewModel

    // The ViewModels must be built after setMain: viewModelScope binds to Dispatchers.Main when it is created.
    @Before fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        tasks = TaskViewModel(TaskRepository(db, session, scheduler, TaskSynchronizer(db, MemoryRemoteStore(), MemoryCategoryStore(), session)), session)
        categories = CategoryViewModel(CategoryRepository(db, session, scheduler), session)
    }
    @After fun tearDown() { Dispatchers.resetMain(); db.close() }

    @Test fun cachedTasksOfThePreviousAccountAreClearedImmediatelyOnSwitch() = runBlocking {
        db.taskDao().insertTask(Task(description = "De alice", userId = "alice"))
        withTimeout(5_000) { tasks.allTasks.first { it.isNotEmpty() } }
        // Nobody is collecting any more (screen gone), and the account changes.
        session.uid.value = "bob"
        assertTrue(tasks.allTasks.value.none { it.userId == "alice" })
        assertTrue(tasks.tasks.value.isEmpty())
    }

    @Test fun cachedCategoriesOfThePreviousAccountAreClearedImmediatelyOnSwitch() = runBlocking {
        db.categoryDao().insertCategory(Category(name = "De alice", userId = "alice"))
        withTimeout(5_000) { categories.categories.first { it.isNotEmpty() } }
        session.uid.value = "bob"
        assertTrue(categories.categories.value.none { it.userId == "alice" })
    }

    @Test fun signingOutLeavesNoTasksAndStopsLoading() = runBlocking {
        db.taskDao().insertTask(Task(description = "De alice", userId = "alice"))
        withTimeout(5_000) { tasks.allTasks.first { it.isNotEmpty() } }
        session.uid.value = null
        assertTrue(tasks.allTasks.value.isEmpty())
        assertFalse(tasks.loading.value)
    }

    @Test fun searchAndFilterAreResetWhenTheAccountChanges() = runBlocking {
        tasks.userFilterTask("algo")
        tasks.userFilterCategory("c")
        session.uid.value = "bob"
        assertEquals("", tasks.msgBusqueda.value)
        assertNull(tasks.categoryFilter.value)
    }
}
