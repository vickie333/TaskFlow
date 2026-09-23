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
                requireNotNull(snapshot.toObject(TaskDto::class.java))
            } else null
            val winner = chooseTaskVersion(local, remote)
            if (remote == null || local.updatedAt > remote.updatedAt) {
                transaction.set(document, winner)
            }
            winner
        }.await()
    }

    override suspend fun fetchAll(uid: String): Map<String, TaskDto> =
        tasks(uid).get(Source.SERVER).await().documents.associate { document ->
            document.id to requireNotNull(document.toObject(TaskDto::class.java))
        }
}
