package com.example.taskflow.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taskflow.data.Priority
import com.example.taskflow.ui.category.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(taskId: String, viewModel: TaskViewModel, viewModelCategory: CategoryViewModel, onBack: () -> Unit) {
    val uid by viewModel.accountUid.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()
    val allCategories by viewModelCategory.categories.collectAsState()
    val categories = allCategories.filter { it.userId == uid }
    val task = tasks.find { it.id == taskId && it.userId == uid }
    val error by viewModel.message.collectAsState()
    val busy by viewModel.busy.collectAsState()
    var description by rememberSaveable(taskId, uid) { mutableStateOf<String?>(null) }
    var categoryId by rememberSaveable(taskId, uid) { mutableStateOf<String?>(null) }
    var priority by rememberSaveable(taskId, uid) { mutableStateOf<Priority?>(null) }
    var categoryExpanded by rememberSaveable { mutableStateOf(false) }
    var priorityExpanded by rememberSaveable { mutableStateOf(false) }
    val currentDescription = description ?: task?.description.orEmpty()
    val currentCategory = categoryId ?: task?.categoryId?.takeIf { id -> categories.any { it.id == id } }
    val currentPriority = priority ?: task?.priority ?: Priority.MEDIA

    Scaffold(topBar = { TopAppBar(title = { Text("Editar tarea") }, navigationIcon = { TextButton(onClick = onBack) { Text("Volver") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (task == null) Text("No se encontró la tarea") else {
                OutlinedTextField(currentDescription, { description = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
                Box {
                    OutlinedButton(onClick = { categoryExpanded = true }) { Text(categories.find { it.id == currentCategory }?.name ?: "Sin categoría") }
                    DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        DropdownMenuItem(text = { Text("Sin categoría") }, onClick = { categoryId = ""; categoryExpanded = false })
                        categories.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { categoryId = item.id; categoryExpanded = false }) }
                    }
                }
                Box {
                    OutlinedButton(onClick = { priorityExpanded = true }) { Text("Prioridad: ${currentPriority.name}") }
                    DropdownMenu(expanded = priorityExpanded, onDismissRequest = { priorityExpanded = false }) {
                        Priority.entries.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { priority = item; priorityExpanded = false }) }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(enabled = currentDescription.isNotBlank() && !busy, onClick = {
                    viewModel.editTask(task.copy(description = currentDescription, categoryId = currentCategory?.takeIf { it.isNotEmpty() }, priority = currentPriority), onSuccess = onBack)
                }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Guardando…" else "Guardar") }
            }
        }
    }
}
