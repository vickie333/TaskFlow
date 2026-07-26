package com.example.taskflow.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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

@Composable
fun TaskEditScreen(taskId: Int?, viewModel: TaskViewModel, onBack: () -> Unit) {
    val tasks by viewModel.tasks.collectAsState()
    val task = tasks.find { it.id == taskId }

    var description by remember(task) { mutableStateOf(task?.description ?: "") }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(description, onValueChange = {
            userDescription -> description = userDescription
        }, placeholder = { Text("Enter the new description") },
            label = { Text("Editar tarea")})

        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Button(onClick = {
                onBack()
            }) {
                Text("Volver")
            }

            if (task != null && !description.isEmpty()) {
                Button(onClick = {
                    var taskEdited = task.copy(description = description)
                    viewModel.editTask(taskEdited)
                    onBack()
                }) {
                    Text("Guardar")
                }
            }
        }
    }
}