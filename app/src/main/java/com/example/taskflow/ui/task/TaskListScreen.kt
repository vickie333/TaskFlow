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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import com.example.taskflow.data.Category
import com.example.taskflow.data.Priority
import com.example.taskflow.ui.category.CategoryViewModel

@Composable
fun TaskListScreen(viewModel: TaskViewModel, viewModelAuth: AuthViewModel, viewModelCategory: CategoryViewModel, onTaskClick: (Task) -> Unit, onLogout:() -> Unit, onCategoryClick:() -> Unit) {
    val tasks by viewModel.tasks.collectAsState()
    val categories by viewModelCategory.categories.collectAsState()
    var text by rememberSaveable() { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf(Priority.MEDIA) }
    var category by remember { mutableStateOf<Category?>(null) }
    var expandedCategory by remember { mutableStateOf(false) }


    Column(modifier = Modifier
        .fillMaxSize()
        .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.Center) {
        Button(onClick = {
            viewModelAuth.signOut()
            onLogout()
        }) {
            Text("Salir")
        }
        Button(onClick = {
            onCategoryClick()
        }) {
            Text("Categories")
        }

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp))
        {
            OutlinedTextField(text, onValueChange = {
                    userText -> text = userText
            }, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = {
                if (text.isNotBlank()) {
                    viewModel.addTask(text, priority, category?.id)
                    text = ""
                    priority = Priority.MEDIA
                    category = null
                }
            }) {
                Text("Add a task")
            }
        }

        Box() {
            Button(onClick = {expandedCategory = true}) {
                Text(category?.name ?: "Elegir categoría")
            }
            DropdownMenu(expanded = expandedCategory,
                onDismissRequest = { expandedCategory = false}) {
                    DropdownMenuItem(text= { Text("Sin categoría") },
                        onClick = {
                            category = null
                            expandedCategory = false
                        }
                    )
                    categories.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.name)},
                            onClick = {
                                category = c
                                expandedCategory = false
                            }
                        )
                    }
            }
        }
        Box() {
            Button(onClick = {expanded = true},
                colors = when (priority) {
                    Priority.ALTA -> ButtonDefaults.buttonColors(containerColor = Color.Red)
                    Priority.MEDIA -> ButtonDefaults.buttonColors(containerColor = Color.Blue)
                    Priority.BAJA -> ButtonDefaults.buttonColors(containerColor = Color.Gray)
                }) {
                Text(priority.name)
            }
            DropdownMenu(expanded = expanded,
                onDismissRequest = { expanded = false}) {
                Priority.entries.forEach { p ->
                    DropdownMenuItem(
                        text = { Text(p.name) },
                        onClick = {
                            priority= p
                            expanded = false
                        }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(tasks) { task ->
                Row(modifier = Modifier
                    .clickable { onTaskClick(task) }
                    .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { viewModel.toggleCompleted(task) }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.description,
                            fontSize = 20.sp,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            color = when (task.priority) {
                                Priority.ALTA -> Color.Red
                                Priority.MEDIA -> Color.Blue
                                Priority.BAJA -> Color.Gray
                            }
                        )
                        Text(
                            text = categories.find { it.id == task.categoryId }?.name ?: "Sin categoría",
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = { viewModel.removeTask(task)}) {
                        Text(text = "Eliminar tarea")
                    }
                }
            }
        }
    }
}