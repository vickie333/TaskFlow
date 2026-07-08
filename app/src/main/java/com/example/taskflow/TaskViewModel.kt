package com.example.taskflow

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Task(
    val id: Number,
    val description: String
)

class TaskViewModel: ViewModel() {
    private val _tasks = MutableStateFlow(listOf(
        Task(1,"tarea 1"),
        Task(2,"tarea 2"),
        Task(3,"tarea 3")
    ))

    val tasks: StateFlow<List<Task>> = _tasks
}