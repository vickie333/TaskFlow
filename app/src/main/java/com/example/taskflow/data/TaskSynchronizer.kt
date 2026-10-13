package com.example.taskflow.data

import android.util.Log
import androidx.room.withTransaction
import com.example.taskflow.data.remote.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** How long a synced deletion marker is kept before it is purged locally and in the cloud. */
internal const val TOMBSTONE_TTL_MS = 30L * 24 * 60 * 60 * 1000

internal fun chooseTaskVersion(local: TaskDto, remote: TaskDto?): TaskDto = if (remote == null || local.updatedAt > remote.updatedAt) local else remote

@Singleton
class TaskSynchronizer @Inject constructor(private val db: AppDatabase, private val remote: TaskRemoteStore, private val categories: CategoryRemoteStore, private val session: AccountSession) {
    private val mutex = Mutex()
    internal var clock: () -> Long = { System.currentTimeMillis() }
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
        purgeOldTombstones(uid, taskSnapshot.values, categorySnapshot.values)
        // Not a success: the worker must report it and retry once the category has synced.
        if (skippedTasks.isNotEmpty()) throw java.io.IOException("${skippedTasks.size} tarea(s) esperan que su categoría se sincronice")
    }
    /**
     * Housekeeping: drops deletion markers that every device has had [TOMBSTONE_TTL_MS] to see. A device that stays
     * offline longer than that could resurrect the item, which is the accepted trade-off. Failures never fail the sync.
     */
    private suspend fun purgeOldTombstones(uid: String, taskSnapshot: Collection<Task>, categorySnapshot: Collection<Category>) {
        val cutoff = clock() - TOMBSTONE_TTL_MS
        // Only rows that were already synced when the cycle began: a delete uploaded just now must reach the other devices first.
        // The DELETE queries match the exact revision, so a row edited meanwhile is left alone.
        for (task in taskSnapshot.filter { it.pendingDelete && it.isSynced && it.updatedAt < cutoff }) {
            checkSession(uid)
            if (!tryPurge("task ${task.id}") { remote.purge(uid, task.id, task.updatedAt) }) return
            db.taskDao().purgeTombstone(uid, task.id, task.updatedAt)
        }
        // Tasks go first: a category tombstone is kept while any task row still points at it.
        for (category in categorySnapshot.filter { it.pendingDelete && it.isSynced && it.updatedAt < cutoff }) {
            checkSession(uid)
            if (db.taskDao().tasksReferencing(category.id) > 0) continue
            if (!tryPurge("category ${category.id}") { categories.purge(uid, category.id, category.updatedAt) }) return
            db.categoryDao().purgeTombstone(uid, category.id, category.updatedAt)
        }
    }
    private suspend fun tryPurge(what: String, block: suspend () -> Unit): Boolean = try {
        block(); true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w("TaskSynchronizer", "No se pudo purgar $what; se reintenta en la próxima sincronización", e)
        false
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
