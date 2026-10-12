package com.example.taskflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.taskflow.ui.auth.AuthViewModel
import com.example.taskflow.ui.auth.LoginScreen
import com.example.taskflow.ui.category.CategoryScreen
import com.example.taskflow.ui.category.CategoryViewModel
import com.example.taskflow.ui.settings.SettingsScreen
import com.example.taskflow.ui.settings.SettingsViewModel
import com.example.taskflow.ui.settings.ThemeChoice
import com.example.taskflow.ui.task.TaskDetailScreen
import com.example.taskflow.ui.task.TaskEditScreen
import com.example.taskflow.ui.task.TaskListScreen
import com.example.taskflow.ui.task.TaskViewModel
import com.example.taskflow.ui.theme.TaskFlowTheme
import dagger.hilt.android.AndroidEntryPoint

private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val auth: AuthViewModel = hiltViewModel()
            val settings: SettingsViewModel = hiltViewModel()
            val theme by settings.theme.collectAsState()
            val uid by auth.uid.collectAsState()
            val dark = when (theme) {
                ThemeChoice.SYSTEM -> isSystemInDarkTheme()
                ThemeChoice.LIGHT -> false
                ThemeChoice.DARK -> true
            }
            // Bar icons must follow the in-app theme, not only the system one.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark }
                )
                onDispose {}
            }
            TaskFlowTheme(darkTheme = dark) {
                // A new graph and saveable state scope prevents old account routes and drafts from returning.
                key(uid) {
                    val navController = rememberNavController()
                    if (uid == null) {
                        NavHost(navController, startDestination = "login") {
                            composable("login") { LoginScreen(auth) }
                        }
                    } else {
                        val tasks: TaskViewModel = hiltViewModel()
                        val categories: CategoryViewModel = hiltViewModel()
                        NavHost(navController, startDestination = "lista") {
                            composable("lista") {
                                TaskListScreen(
                                    tasks, auth, categories,
                                    onTaskClick = { navController.navigate("detalle/${it.id}") },
                                    onCategoryClick = { navController.navigate("categories") },
                                    onSettingsClick = { navController.navigate("settings") }
                                )
                            }
                            composable("detalle/{taskId}") { entry ->
                                TaskDetailScreen(entry.arguments?.getString("taskId").orEmpty(), tasks, categories,
                                    onBack = { navController.popBackStack() },
                                    onEdit = { navController.navigate("edit/${entry.arguments?.getString("taskId")}") })
                            }
                            composable("edit/{taskId}") { entry ->
                                TaskEditScreen(entry.arguments?.getString("taskId").orEmpty(), tasks, categories,
                                    onBack = { navController.popBackStack() })
                            }
                            composable("categories") { CategoryScreen(categories, onBack = { navController.popBackStack() }) }
                            composable("settings") { SettingsScreen(settings, onBack = { navController.popBackStack() }) }
                        }
                    }
                }
            }
        }
    }
}
