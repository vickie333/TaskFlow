package com.example.taskflow.ui.task

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.taskflow.data.Category
import com.example.taskflow.data.Priority
import com.example.taskflow.data.Task
import com.example.taskflow.ui.auth.AuthViewModel
import com.example.taskflow.ui.category.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    viewModelAuth: AuthViewModel,
    viewModelCategory: CategoryViewModel,
    onTaskClick: (Task) -> Unit,
    onCategoryClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val uid by viewModelAuth.uid.collectAsState()
    val allTasks by viewModel.tasks.collectAsState()
    val allCategories by viewModelCategory.categories.collectAsState()
    val categories = allCategories.filter { it.userId == uid }
    val tasks = allTasks.filter { it.userId == uid }
    val query by viewModel.msgBusqueda.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()
    val error by viewModel.message.collectAsState()
    val busy by viewModel.busy.collectAsState()
    var text by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf(Priority.MEDIA) }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var showPriority by rememberSaveable { mutableStateOf(false) }
    var showCategory by rememberSaveable { mutableStateOf(false) }
    var showFilter by rememberSaveable { mutableStateOf(false) }
    var showActions by rememberSaveable { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("TaskFlow") }, actions = {
            Box {
                TextButton(onClick = { showActions = true }) { Text("Menú") }
                DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                    DropdownMenuItem(text = { Text("Categorías") }, onClick = { showActions = false; onCategoryClick() })
                    DropdownMenuItem(text = { Text("Ajustes") }, onClick = { showActions = false; onSettingsClick() })
                    DropdownMenuItem(text = { Text("Salir") }, onClick = { showActions = false; viewModelAuth.signOut() })
                }
            }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(query, viewModel::userFilterTask, label = { Text("Buscar") }, singleLine = true, modifier = Modifier.weight(1f))
                Box {
                    OutlinedButton(onClick = { showFilter = true }) { Text(categories.find { it.id == categoryFilter }?.name ?: "Todas") }
                    DropdownMenu(expanded = showFilter, onDismissRequest = { showFilter = false }) {
                        DropdownMenuItem(text = { Text("Todas") }, onClick = { viewModel.userFilterCategory(null); showFilter = false })
                        categories.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { viewModel.userFilterCategory(item.id); showFilter = false }) }
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(text, { text = it }, label = { Text("Nueva tarea") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { showCategory = true }, modifier = Modifier.fillMaxWidth()) { Text(categories.find { it.id == categoryId }?.name ?: "Sin categoría", maxLines = 1) }
                            DropdownMenu(expanded = showCategory, onDismissRequest = { showCategory = false }) {
                                DropdownMenuItem(text = { Text("Sin categoría") }, onClick = { categoryId = null; showCategory = false })
                                categories.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { categoryId = item.id; showCategory = false }) }
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { showPriority = true }, modifier = Modifier.fillMaxWidth()) { Text(priority.name) }
                            DropdownMenu(expanded = showPriority, onDismissRequest = { showPriority = false }) {
                                Priority.entries.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { priority = item; showPriority = false }) }
                            }
                        }
                    }
                    Button(onClick = {
                        viewModel.addTask(text, priority, categoryId) {
                            text = ""; priority = Priority.MEDIA; categoryId = null
                        }
                    }, enabled = text.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) { Text("Agregar tarea") }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (tasks.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("No hay tareas para mostrar") }
            } else LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(tasks, key = { it.id }) { task ->
                    Row(Modifier.fillMaxWidth().clickable { onTaskClick(task) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(task.isCompleted, onCheckedChange = { viewModel.toggleCompleted(task) })
                        Column(Modifier.weight(1f)) {
                            Text(task.description, style = MaterialTheme.typography.bodyLarge, textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None)
                            Text("${categories.find { it.id == task.categoryId }?.name ?: "Sin categoría"} · ${task.priority.name}", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { viewModel.removeTask(task) }) { Text("Eliminar") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
