package com.example.taskflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModelTask: TaskViewModel = hiltViewModel()
            val viewModelAuth: AuthViewModel = hiltViewModel()

            val isLogged by viewModelAuth.isLogged.collectAsState()

            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = if (isLogged) "lista" else "login") {
                composable("lista") {
                    TaskListScreen(
                        viewModelTask,
                        viewModelAuth,
                        onTaskClick = {task -> navController.navigate("detalle/${task.id}")},
                        onLogout = { navController.navigate("login") {
                            popUpTo("lista") {
                                inclusive = true
                            }
                        } }
                    )
                }
                composable("detalle/{taskId}") {
                    backStackEntry ->
                    val taskId = backStackEntry.arguments?.getString("taskId")?.toIntOrNull() ?: 0
                    TaskDetailScreen(taskId, viewModelTask, onBack = { navController.popBackStack()})
                }
                composable("login") {
                    LoginScreen(
                        onLoginClick = { navController.navigate("lista") {
                            popUpTo("login") {
                                inclusive = true
                            }
                        } }
                    )
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

@Composable
fun TaskListScreen(viewModel: TaskViewModel, viewModelAuth: AuthViewModel, onTaskClick: (Task) -> Unit, onLogout: () -> Unit) {
    val tasks by viewModel.tasks.collectAsState()
    var text by rememberSaveable() { mutableStateOf("") }

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
                Row(modifier = Modifier.clickable {onTaskClick(task)}) {
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

@Composable
fun TaskDetailScreen(taskId: Int?, viewModel: TaskViewModel, onBack: () -> Unit) {
    val tasks = viewModel.tasks.collectAsState()
    val task = tasks.value.find { it.id == taskId }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (task != null) {
            Text(text = "The description of the task ${task.id} is ${task.description}",
                fontSize = 30.sp)
        } else {
            Text("No existe una tarea con ese id")
        }
        Button(onClick = onBack) {
            Text("Volver")
        }
    }
}