package com.example.taskflow.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taskflow.ui.category.CategoryViewModel

@Composable
fun TaskEditScreen(taskId: String, viewModel: TaskViewModel, viewModelCategory: CategoryViewModel, onBack: () -> Unit) {
    val tasks by viewModel.tasks.collectAsState()
    val categories by viewModelCategory.categories.collectAsState()
    val task = tasks.find { it.id == taskId }

    var description by remember(task) { mutableStateOf(task?.description ?: "") }
    var category by remember(task, categories) {
        mutableStateOf(categories.find { it.id == task?.categoryId })
    }
    var expandedCategory by remember { mutableStateOf(false) }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(description, onValueChange = {
            userDescription -> description = userDescription
        }, placeholder = { Text("Enter the new description") },
            label = { Text("Editar tarea")})

        Box() {
            Button(onClick = { expandedCategory = true }) {
                Text(category?.name ?: "Elegir categoría")
            }
            DropdownMenu(expanded = expandedCategory,
                onDismissRequest = { expandedCategory = false }) {
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

        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Button(onClick = {
                onBack()
            }) {
                Text("Volver")
            }

            if (task != null && description.isNotBlank()) {
                Button(onClick = {
                    val taskEdited = task.copy(description = description, categoryId = category?.id)
                    viewModel.editTask(taskEdited)
                    onBack()
                }) {
                    Text("Guardar")
                }
            }
        }
    }
}