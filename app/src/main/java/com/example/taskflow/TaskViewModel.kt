package com.example.taskflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Entity
import androidx.room.PrimaryKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val description: String
)

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
}