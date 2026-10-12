package com.example.taskflow.data

import androidx.room.withTransaction
import com.example.taskflow.data.remote.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

internal fun chooseTaskVersion(local: TaskDto, remote: TaskDto?): TaskDto = if (remote == null || local.updatedAt > remote.updatedAt) local else remote

@Singleton
class TaskSynchronizer @Inject constructor(private val db: AppDatabase, private val remote: TaskRemoteStore, private val categories: CategoryRemoteStore, private val session: AccountSession) {
    private val mutex = Mutex()
    private suspend fun checkSession(uid: String) {
        currentCoroutineContext().ensureActive()
        check(session.currentUid() == uid) { "La sesión cambió" }
    }
    suspend fun sync(uid: String) = mutex.withLock {
        require(uid.isNotBlank() && uid != "USER")
        checkSession(uid)
        // Snapshots identify exactly which revision may be replaced after network suspension.
        val (categorySnapshot, taskSnapshot) = db.withTransaction {
            checkSession(uid)
            db.categoryDao().getCategories(uid).associateBy { it.id } to
                db.taskDao().getTasks(uid).associateBy { it.id }
        }
        val skippedTasks = mutableSetOf<String>()
        for (category in categorySnapshot.values.filter { !it.isSynced }) {
            checkSession(uid)
            val confirmed = categories.exchange(uid, category)
            applyCategory(uid, confirmed.toCategory(category.id, uid), category)
        }
        checkSession(uid)
        val downloadedCategories = categories.fetchAll(uid)
        for ((id, dto) in downloadedCategories) applyCategory(uid, dto.toCategory(id, uid), categorySnapshot[id])
        for (task in taskSnapshot.values.filter { !it.isSynced }) {
            checkSession(uid)
            if (task.categoryId != null && db.categoryDao().getCategoryById(task.categoryId)?.isSynced != true) {
                skippedTasks += task.id
                continue
            }
            val confirmed = remote.exchange(uid, task)
            applyTask(uid, confirmed.toTask(task.id, uid), task)
        }
        checkSession(uid)
        val downloadedTasks = remote.fetchAll(uid)
        for ((id, dto) in downloadedTasks) {
            if (id !in skippedTasks) applyTask(uid, dto.toTask(id, uid), taskSnapshot[id])
        }
        // Not a success: the worker must report it and retry once the category has synced.
        if (skippedTasks.isNotEmpty()) throw java.io.IOException("${skippedTasks.size} tarea(s) esperan que su categoría se sincronice")
    }
    private suspend fun applyCategory(uid: String, remote: Category, snapshot: Category?) = db.withTransaction {
        checkSession(uid)
        val local = db.categoryDao().getCategoryById(remote.id)
        check(local == null || local.userId == uid)
        if (local == null) db.categoryDao().insertCategory(remote)
        else if ((local == snapshot || local.isSynced) && remote.updatedAt >= local.updatedAt) db.categoryDao().editCategory(remote)
    }
    private suspend fun applyTask(uid: String, remote: Task, snapshot: Task?) = db.withTransaction {
        checkSession(uid)
        val local = db.taskDao().getTaskById(remote.id)
        check(local == null || local.userId == uid)
        val category = remote.categoryId?.let { db.categoryDao().getCategoryById(it) }
        require(category == null || category.userId == uid) { "Categoría de otra cuenta" }
        val valid = if (category == null) remote.copy(categoryId = null) else remote
        if (local == null) db.taskDao().insertTask(valid)
        else if ((local == snapshot || local.isSynced) && remote.updatedAt >= local.updatedAt) db.taskDao().editTask(valid)
    }
}
