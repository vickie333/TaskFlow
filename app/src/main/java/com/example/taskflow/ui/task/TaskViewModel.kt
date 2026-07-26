package com.example.taskflow.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskflow.data.Task
import com.example.taskflow.data.TaskDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(private val dao: TaskDao): ViewModel() {
    val tasks: StateFlow<List<Task>> = dao.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTask(description: String) {
        viewModelScope.launch {
            dao.insertTask(Task(0,description))
        }
    }

    fun removeTask(task: Task) {
        viewModelScope.launch {
            dao.deleteTask(task)
        }
    }

    fun editTask(task: Task) {
        viewModelScope.launch {
            dao.editTask(task)
        }
    }

    fun toggleCompleted(task: Task) {
        viewModelScope.launch {
            dao.editTask(task.copy(isCompleted = !task.isCompleted))
        }
    }
}