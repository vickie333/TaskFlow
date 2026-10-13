package com.example.taskflow.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :uid AND pendingDelete = 0 ORDER BY name")
    fun getAllCategories(uid: String): Flow<List<Category>>
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): Category?
    @Query("SELECT * FROM categories WHERE userId = :uid")
    suspend fun getCategories(uid: String): List<Category>
    @Insert suspend fun insertCategory(category: Category)
    @Query("DELETE FROM categories WHERE id = :id AND userId = :uid AND pendingDelete = 1 AND isSynced = 1 AND updatedAt = :updatedAt AND NOT EXISTS (SELECT 1 FROM tasks WHERE categoryId = :id)")
    suspend fun purgeTombstone(uid: String, id: String, updatedAt: Long): Int
    @Update suspend fun editCategory(category: Category)
}
