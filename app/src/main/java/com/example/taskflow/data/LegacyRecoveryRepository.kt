package com.example.taskflow.data

import androidx.room.withTransaction
import java.util.UUID
import javax.inject.Inject

class LegacyRecoveryRepository @Inject constructor(private val db: AppDatabase, private val session: AccountSession, private val scheduler: SyncScheduler) {
    suspend fun recover(): Int {
        val uid = session.requireUid()
        val count = db.withTransaction {
            val tasks = db.taskDao().getLegacyTasks()
            val categories = mutableMapOf<String, String>()
            for (task in tasks) {
                if (task.pendingDelete) { db.taskDao().deleteTask(task); continue } // a tombstone for a task never uploaded has nothing to propagate
                val categoryId = task.categoryId?.let { oldId ->
                    categories[oldId] ?: db.categoryDao().getCategoryById(oldId)?.let { old ->
                        require(old.userId == "USER" || old.userId.isBlank()) { "La categoría pertenece a otra cuenta" }
                        val id = UUID.randomUUID().toString()
                        db.categoryDao().insertCategory(old.copy(id = id, userId = uid, updatedAt = nextTimestamp(old.updatedAt), isSynced = false))
                        categories[oldId] = id
                        id
                    }
                }
                db.taskDao().insertTask(task.copy(id = UUID.randomUUID().toString(), userId = uid, categoryId = categoryId, updatedAt = nextTimestamp(task.updatedAt), isSynced = false))
                db.taskDao().deleteTask(task)
            }
            check(session.currentUid() == uid)
            tasks.count { !it.pendingDelete }
        }
        if (count > 0) scheduler.requestSync(uid)
        return count
    }
}
