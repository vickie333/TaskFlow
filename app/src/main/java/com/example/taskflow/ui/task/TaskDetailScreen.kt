package com.example.taskflow.ui.task

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TaskDetailScreen(taskId: Int?, viewModel: TaskViewModel, onBack: () -> Unit, onEdit: () -> Unit) {
    val tasks = viewModel.tasks.collectAsState()
    val task = tasks.value.find { it.id == taskId }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (task != null) {
            Text(text = "The description of the task ${task.id} is ${task.description}",
                fontSize = 30.sp)
        } else {
            Text("No existe una tarea con ese id")
        }
        Row(modifier = Modifier.fillMaxSize().weight(1f)) {
            Button(onClick = onBack) {
                Text("Volver")
            }
            if (task != null) {
                Button(onClick = {
                    onEdit()
                }) {
                    Text("Editar")
                }
            }
        }
    }
}