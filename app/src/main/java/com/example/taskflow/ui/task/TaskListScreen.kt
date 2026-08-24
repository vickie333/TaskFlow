package com.example.taskflow.ui.task

import com.example.taskflow.data.Task
import com.example.taskflow.ui.auth.AuthViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.taskflow.data.Category
import com.example.taskflow.data.Priority
import com.example.taskflow.ui.category.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    viewModelAuth: AuthViewModel,
    viewModelCategory: CategoryViewModel,
    onTaskClick: (Task) -> Unit,
    onLogout: () -> Unit,
    onCategoryClick: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val categories by viewModelCategory.categories.collectAsState()
    val busqueda by viewModel.msgBusqueda.collectAsState()
    val categoryFilter by viewModel.categoryFilter.collectAsState()

    var text by rememberSaveable() { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf(Priority.MEDIA) }
    var category by remember { mutableStateOf<Category?>(null) }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedCategoryFilter by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TaskFlow") },
                actions = {
                    TextButton(onClick = onCategoryClick) {
                        Text("Categorías")
                    }
                    TextButton(onClick = {
                        viewModelAuth.signOut()
                        onLogout()
                    }) {
                        Text("Salir")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { userText -> viewModel.userFilterTask(userText) },
                    label = { Text("Buscar") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    OutlinedButton(onClick = { expandedCategoryFilter = true }) {
                        Text(categories.find { it.id == categoryFilter }?.name ?: "Todas")
                    }
                    DropdownMenu(
                        expanded = expandedCategoryFilter,
                        onDismissRequest = { expandedCategoryFilter = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todas") },
                            onClick = {
                                viewModel.userFilterCategory(null)
                                expandedCategoryFilter = false
                            }
                        )
                        categories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = {
                                    viewModel.userFilterCategory(c.id)
                                    expandedCategoryFilter = false
                                }
                            )
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { userText -> text = userText },
                        label = { Text("Nueva tarea") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            OutlinedButton(onClick = { expandedCategory = true }) {
                                Text(category?.name ?: "Categoría")
                            }
                            DropdownMenu(
                                expanded = expandedCategory,
                                onDismissRequest = { expandedCategory = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sin categoría") },
                                    onClick = {
                                        category = null
                                        expandedCategory = false
                                    }
                                )
                                categories.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.name) },
                                        onClick = {
                                            category = c
                                            expandedCategory = false
                                        }
                                    )
                                }
                            }
                        }

                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = colorOf(priority)
                                )
                            ) {
                                Text(priority.name)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                Priority.entries.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.name) },
                                        onClick = {
                                            priority = p
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            enabled = text.isNotBlank(),
                            onClick = {
                                viewModel.addTask(text, priority, category?.id)
                                text = ""
                                priority = Priority.MEDIA
                                category = null
                            }
                        ) {
                            Text("Agregar")
                        }
                    }
                }
            }

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay tareas para mostrar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(tasks) { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTaskClick(task) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { viewModel.toggleCompleted(task) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textDecoration = if (task.isCompleted) {
                                        TextDecoration.LineThrough
                                    } else {
                                        TextDecoration.None
                                    }
                                )
                                Text(
                                    text = "${categories.find { it.id == task.categoryId }?.name ?: "Sin categoría"} · ${task.priority.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorOf(task.priority)
                                )
                            }
                            TextButton(onClick = { viewModel.removeTask(task) }) {
                                Text("Eliminar")
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

private fun colorOf(priority: Priority): Color = when (priority) {
    Priority.ALTA -> Color.Red
    Priority.MEDIA -> Color.Blue
    Priority.BAJA -> Color.Gray
}
