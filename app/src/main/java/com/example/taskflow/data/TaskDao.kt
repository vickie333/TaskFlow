package com.example.taskflow.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query(
        """
        SELECT * FROM tasks 
        WHERE pendingDelete = 0 AND userId = :uid ORDER BY isCompleted ASC, CASE priority WHEN 'ALTA' THEN 1 WHEN 'MEDIA' THEN 2 WHEN 'BAJA' THEN 3 END ASC
        """
    )
    fun getAllTasks(uid: String): Flow<List<Task>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE userId = :uid AND isSynced = 0
        """
    )
    suspend fun getUnsyncedTasks(uid: String): List<Task>

    @Query(
        """
            UPDATE tasks
            SET isSynced = 1
            WHERE id = :taskId AND updatedAt = :uploadedAt
        """
    )
    suspend fun markAsSynced(
        taskId: String,
        uploadedAt: Long
    ): Int

    @Insert
    suspend fun insertTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Update
    suspend fun editTask(task: Task)
}