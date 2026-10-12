package com.example.taskflow.data

import androidx.room.withTransaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class TaskRepository @Inject constructor(private val db: AppDatabase, private val session: AccountSession, private val scheduler: SyncScheduler, private val synchronizer: TaskSynchronizer) {
    private val dao get() = db.taskDao()
    suspend fun addTask(description: String, priority: Priority, categoryId: String?) {
        val uid = session.requireUid()
        db.withTransaction {
            validateCategory(categoryId, uid)
            check(session.currentUid() == uid)
            dao.insertTask(Task(description = description.trim().also { require(it.isNotEmpty()) }, priority = priority, categoryId = categoryId, userId = uid))
        }
        scheduler.requestSync(uid)
    }
    private suspend fun validateCategory(id: String?, uid: String) {
        if (id != null) {
            val category = requireNotNull(db.categoryDao().getCategoryById(id))
            require(category.userId == uid && !category.pendingDelete) { "Categoría no disponible" }
        }
    }
    private suspend fun mutate(task: Task, transform: (Task) -> Task) {
        val uid = session.requireUid()
        db.withTransaction {
            val persisted = requireNotNull(dao.getTaskById(task.id))
            require(task.userId == uid && persisted.userId == uid && !persisted.pendingDelete)
            val edited = transform(persisted)
            // An existing remote tombstone reference may be cleared while editing.
            val category = edited.categoryId?.let { db.categoryDao().getCategoryById(it) }
            val normalized = if (edited.categoryId == persisted.categoryId && category?.pendingDelete == true) edited.copy(categoryId = null) else edited
            validateCategory(normalized.categoryId, uid)
            check(session.currentUid() == uid)
            dao.editTask(normalized.copy(updatedAt = nextTimestamp(persisted.updatedAt), isSynced = false))
        }
        scheduler.requestSync(uid)
    }
    suspend fun removeTask(task: Task) = mutate(task) { it.copy(pendingDelete = true) }
    suspend fun editTask(task: Task) = mutate(task) { it.copy(description = task.description.trim().also { text -> require(text.isNotEmpty()) }, priority = task.priority, categoryId = task.categoryId, isCompleted = task.isCompleted) }
    suspend fun toggleCompleted(task: Task) = mutate(task) { it.copy(isCompleted = !it.isCompleted) }
    suspend fun sync() { session.currentUid()?.let { synchronizer.sync(it) } }
    fun getAllTasks() = session.uid.flatMapLatest { uid -> if (uid.isNullOrBlank() || uid == "USER") flowOf(emptyList()) else dao.getAllTasks(uid) }
}
