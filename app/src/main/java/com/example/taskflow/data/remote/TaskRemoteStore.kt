package com.example.taskflow.data.remote

import com.example.taskflow.data.Task
import com.example.taskflow.data.chooseTaskVersion
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

interface TaskRemoteStore {
    suspend fun exchange(uid: String, task: Task): TaskDto
    suspend fun fetchAll(uid: String): Map<String, TaskDto>
    /** Deletes the cloud document only if it is still the tombstone written at [updatedAt] (or already gone). */
    suspend fun purge(uid: String, id: String, updatedAt: Long)
}

class FirestoreTaskRemoteStore @Inject constructor(
    private val firestore: FirebaseFirestore
) : TaskRemoteStore {
    private fun tasks(uid: String) = firestore.collection("users").document(uid).collection("tasks")

    override suspend fun exchange(uid: String, task: Task): TaskDto {
        require(task.userId == uid)
        val document = tasks(uid).document(task.id)
        val local = task.toDto()
        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(document)
            val remote = if (snapshot.exists()) {
                snapshot.readTask()
            } else null
            val winner = chooseTaskVersion(local, remote)
            if (remote == null || local.updatedAt > remote.updatedAt) {
                transaction.set(document, mapOf("description" to winner.description, "isCompleted" to winner.isCompleted, "priority" to winner.priority, "updatedAt" to winner.updatedAt, "categoryId" to winner.categoryId, "pendingDelete" to winner.pendingDelete))
            }
            winner
        }.await()
    }

    override suspend fun purge(uid: String, id: String, updatedAt: Long) {
        val document = tasks(uid).document(id)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(document)
            if (snapshot.exists()) {
                val remote = snapshot.readTask()
                if (remote.pendingDelete && remote.updatedAt <= updatedAt) transaction.delete(document)
            }
        }.await()
    }

    override suspend fun fetchAll(uid: String): Map<String, TaskDto> =
        tasks(uid).get(Source.SERVER).await().documents.associate { document ->
            document.id to document.readTask()
        }
}

private fun com.google.firebase.firestore.DocumentSnapshot.readTask() = TaskDto(
    description = getString("description") ?: "",
    isCompleted = getBoolean("isCompleted") ?: getBoolean("completed") ?: false,
    priority = getString("priority") ?: "MEDIA",
    updatedAt = getLong("updatedAt") ?: 0,
    categoryId = getString("categoryId"),
    pendingDelete = getBoolean("pendingDelete") ?: false
)
