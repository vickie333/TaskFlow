package com.example.taskflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val userId: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val pendingDelete: Boolean = false
)
