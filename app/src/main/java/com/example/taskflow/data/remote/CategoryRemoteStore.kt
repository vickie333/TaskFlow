package com.example.taskflow.data.remote

import com.example.taskflow.data.Category
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class CategoryDto(val name: String = "", val updatedAt: Long = 0, val pendingDelete: Boolean = false)
// Tolerant read: a malformed document loses conflicts (updatedAt = 0) instead of aborting the whole sync.
internal fun DocumentSnapshot.readCategory() = CategoryDto(
    name = get("name") as? String ?: "",
    updatedAt = (get("updatedAt") as? Number)?.toLong() ?: 0L,
    pendingDelete = get("pendingDelete") as? Boolean ?: false
)
fun Category.toDto() = CategoryDto(name, updatedAt, pendingDelete)
fun CategoryDto.toCategory(id: String, uid: String) = Category(id, name, uid, updatedAt, true, pendingDelete)
interface CategoryRemoteStore {
    suspend fun exchange(uid: String, category: Category): CategoryDto
    suspend fun fetchAll(uid: String): Map<String, CategoryDto>
    /** Deletes the cloud document only if it is still the tombstone written at [updatedAt] (or already gone). */
    suspend fun purge(uid: String, id: String, updatedAt: Long)
}
class FirestoreCategoryRemoteStore @Inject constructor(private val firestore: FirebaseFirestore) : CategoryRemoteStore {
    private fun collection(uid: String) = firestore.collection("users").document(uid).collection("categories")
    override suspend fun exchange(uid: String, category: Category): CategoryDto {
        require(category.userId == uid)
        val document = collection(uid).document(category.id)
        val local = category.toDto()
        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(document)
            val remote = if (snapshot.exists()) snapshot.readCategory() else null
            val winner = if (remote == null || local.updatedAt > remote.updatedAt) local else remote
            if (remote == null || local.updatedAt > remote.updatedAt) transaction.set(document, mapOf("name" to winner.name, "updatedAt" to winner.updatedAt, "pendingDelete" to winner.pendingDelete))
            winner
        }.await()
    }
    override suspend fun purge(uid: String, id: String, updatedAt: Long) {
        val document = collection(uid).document(id)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(document)
            if (snapshot.exists()) {
                val remote = snapshot.readCategory()
                if (remote.pendingDelete && remote.updatedAt <= updatedAt) transaction.delete(document)
            }
        }.await()
    }
    override suspend fun fetchAll(uid: String): Map<String, CategoryDto> = collection(uid).get(Source.SERVER).await().documents.associate { it.id to it.readCategory() }
}
