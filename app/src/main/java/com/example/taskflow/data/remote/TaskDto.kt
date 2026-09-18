package com.example.taskflow.data.remote

import com.example.taskflow.data.Priority
import com.example.taskflow.data.Task

data class TaskDto(
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: String = Priority.MEDIA.toString(),
    val updatedAt: Long = 0L,
    val pendingDelete: Boolean = false
)

fun TaskDto.toTask(
    id: String,
    userId: String,
    categoryId: Int?)
= Task(
    id = id,
    userId = userId,
    isSynced = true,
    categoryId = categoryId,
    description = description,
    isCompleted = isCompleted,
    priority = Priority.entries.find { it.name == priority } ?: Priority.MEDIA,
    updatedAt = updatedAt,
    pendingDelete = pendingDelete
)

fun Task.toDto() = TaskDto(
    description = description,
    isCompleted = isCompleted,
    priority = priority.name,
    updatedAt = updatedAt,
    pendingDelete = pendingDelete
)