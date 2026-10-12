package com.example.taskflow.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taskflow.ui.category.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(taskId: String, viewModel: TaskViewModel, viewModelCategory: CategoryViewModel, onBack: () -> Unit, onEdit: () -> Unit) {
    val uid by viewModel.accountUid.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()
    val allCategories by viewModelCategory.categories.collectAsState()
    val task = tasks.find { it.id == taskId && it.userId == uid }
    val categories = allCategories.filter { it.userId == uid }
    Scaffold(topBar = { TopAppBar(title = { Text("Detalle de tarea") }, navigationIcon = { TextButton(onClick = onBack) { Text("Volver") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (task == null) Text("No se encontró la tarea") else {
                Text(task.description, style = MaterialTheme.typography.headlineSmall)
                Text("Prioridad: ${task.priority.name}")
                Text("Categoría: ${categories.find { it.id == task.categoryId }?.name ?: "Sin categoría"}")
                Text(if (task.isCompleted) "Completada" else "Pendiente")
                Button(onClick = onEdit) { Text("Editar tarea") }
            }
        }
    }
}
