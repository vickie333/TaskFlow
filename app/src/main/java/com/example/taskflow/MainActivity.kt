package com.example.taskflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val dao = AppDatabase.getInstance(applicationContext).taskDao()

            val viewModel: TaskViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        TaskViewModel(dao)
                    }
                }
            )

            val tasks by viewModel.tasks.collectAsState()
            var text by rememberSaveable() { mutableStateOf("") }

            Column(modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.Center) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp))
                {
                    OutlinedTextField(text, onValueChange = {
                            userText -> text = userText
                    },
                        modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = {
                        viewModel.addTask(text)
                        text = ""
                    }) {
                        Text("Add a task")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(tasks) { task ->
                        Row() {
                            TaskName(task.description)
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(onClick = { viewModel.removeTask(task)}) {
                                Text(text = "Eliminar tarea")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskName(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Task: $name!",
        modifier = modifier,
        fontSize = 35.sp
    )
}