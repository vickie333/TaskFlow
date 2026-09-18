package com.example.taskflow.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.taskflow.data.remote.toDto
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TaskRepository @Inject constructor(private val dao: TaskDao, private val auth: FirebaseAuth, private val firestore: FirebaseFirestore) {
    suspend fun addTask(description: String, priority: Priority, categoryId: Int?) {
        dao.insertTask(Task(description = description, priority = priority, categoryId = categoryId, userId = auth.uid ?: ""))
    }

    suspend fun removeTask(task: Task) {
        dao.editTask(task.copy(pendingDelete = true, isSynced = false, updatedAt = System.currentTimeMillis()))
    }

    suspend fun editTask(task: Task) {
            dao.editTask(task.copy(isSynced = false, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleCompleted(task: Task) {
            dao.editTask(task.copy(isCompleted = !task.isCompleted, isSynced = false, updatedAt = System.currentTimeMillis()))
    }

    suspend fun pushPendingTasks() {
        val uid = auth.currentUser?.uid ?: return
        val pendingTaks = dao.getUnsyncedTasks(uid)

        for (task in pendingTaks) {
            firestore
                .collection("users")
                .document(uid)
                .collection("tasks")
                .document(task.id)
                .set(task.toDto())
                .await()

            dao.markAsSynced(
                taskId = task.id,
                uploadedAt = task.updatedAt
            )
        }
    }

    fun getAllTasks() = flow {
        emitAll(dao.getAllTasks(auth.uid ?: ""))
    }
}