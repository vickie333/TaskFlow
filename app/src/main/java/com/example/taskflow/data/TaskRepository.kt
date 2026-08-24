package com.example.taskflow.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class TaskRepository @Inject constructor(private val dao: TaskDao, private val auth: FirebaseAuth) {
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

    fun getAllTasks() = flow {
        emitAll(dao.getAllTasks(auth.uid ?: ""))
    }
}