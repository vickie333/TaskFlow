package com.example.taskflow.data

import androidx.room.withTransaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

const val MAX_CATEGORY_NAME_LENGTH = 100 // keep in sync with firestore.rules

internal fun String.cleanCategoryName(): String = trim().also {
    require(it.isNotEmpty()) { "El nombre no puede estar vacío" }
    require(it.length <= MAX_CATEGORY_NAME_LENGTH) { "El nombre admite hasta $MAX_CATEGORY_NAME_LENGTH caracteres" }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryRepository @Inject constructor(private val db: AppDatabase, private val session: AccountSession, private val scheduler: SyncScheduler) {
    fun getAllCategories() = session.uid.flatMapLatest { uid -> if (uid.isNullOrBlank() || uid == "USER") flowOf(emptyList()) else db.categoryDao().getAllCategories(uid) }
    suspend fun addCategory(name: String) {
        val uid = session.requireUid()
        db.withTransaction {
            check(session.currentUid() == uid)
            db.categoryDao().insertCategory(Category(name = name.cleanCategoryName(), userId = uid))
        }
        scheduler.requestSync(uid)
    }
    private suspend fun mutate(category: Category, delete: Boolean) {
        val uid = session.requireUid()
        db.withTransaction {
            val existing = requireNotNull(db.categoryDao().getCategoryById(category.id))
            require(existing.userId == uid && category.userId == uid && !existing.pendingDelete)
            if (delete) require(db.taskDao().activeCategoryTasks(category.id) == 0) { "La categoría tiene tareas activas" }
            check(session.currentUid() == uid)
            db.categoryDao().editCategory(existing.copy(name = category.name.cleanCategoryName(), pendingDelete = delete, updatedAt = nextTimestamp(existing.updatedAt), isSynced = false))
        }
        scheduler.requestSync(uid)
    }
    suspend fun editCategory(category: Category) = mutate(category, false)
    suspend fun removeCategory(category: Category) = mutate(category, true)
}
