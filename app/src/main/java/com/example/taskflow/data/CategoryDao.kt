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
    @Update suspend fun editCategory(category: Category)
}
