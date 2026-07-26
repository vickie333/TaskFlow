package com.example.taskflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.taskflow.ui.auth.AuthViewModel
import com.example.taskflow.ui.auth.LoginScreen
import com.example.taskflow.ui.task.TaskDetailScreen
import com.example.taskflow.ui.task.TaskEditScreen
import com.example.taskflow.ui.task.TaskListScreen
import com.example.taskflow.ui.task.TaskViewModel
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
import com.example.taskflow.ui.theme.TaskFlowTheme
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

            TaskFlowTheme() {
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
                        TaskDetailScreen(taskId,
                            viewModelTask,
                            onBack = { navController.popBackStack()},
                            onEdit = { navController.navigate("edit/${taskId}")})
                    }
                    composable("edit/{taskId}") {
                            backStackEntry ->
                        val taskId = backStackEntry.arguments?.getString("taskId")?.toIntOrNull() ?: 0
                        TaskEditScreen(taskId,
                            viewModelTask,
                            onBack = { navController.popBackStack()})
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
}