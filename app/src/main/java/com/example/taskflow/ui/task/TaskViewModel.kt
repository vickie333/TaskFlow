package com.example.taskflow.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskflow.data.Priority
import com.example.taskflow.data.Task
import com.example.taskflow.data.TaskDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(private val dao: TaskDao): ViewModel() {
    private val _msgBusqueda = MutableStateFlow("")
    val msgBusqueda: StateFlow<String> = _msgBusqueda

    private val _categoryFilter = MutableStateFlow<Int?>(null)
    val categoryFilter: StateFlow<Int?> = _categoryFilter
    val tasks: StateFlow<List<Task>> = combine(dao.getAllTasks(), _msgBusqueda, _categoryFilter) {
        lista, textoBusqueda, categoryBusqueda -> lista.filter { it.description.contains(textoBusqueda, ignoreCase = true) && ( categoryBusqueda == null || it.categoryId == categoryBusqueda) }
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
    )

    fun userFilterCategory(cat: Int?) {
        _categoryFilter.value = cat
    }
    fun userFilterTask(msg: String) {
        _msgBusqueda.value = msg
    }
    fun addTask(description: String, priority: Priority, categoryId: Int?) {
        viewModelScope.launch {
            dao.insertTask(Task(0,description, priority = priority, categoryId = categoryId))
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