package com.example.taskflow.ui.task

import androidx.compose.ui.res.stringResource
import com.example.taskflow.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taskflow.ui.category.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(taskId: String, viewModel: TaskViewModel, viewModelCategory: CategoryViewModel, onBack: () -> Unit, onEdit: () -> Unit) {
    val uid by viewModel.accountUid.collectAsStateWithLifecycle()
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val allCategories by viewModelCategory.categories.collectAsStateWithLifecycle()
    val task = tasks.find { it.id == taskId && it.userId == uid }
    val categories = allCategories.filter { it.userId == uid }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.detail_title)) }, navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (task == null) Text(stringResource(R.string.task_not_found)) else {
                Text(task.description, style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.priority_value, task.priority.name))
                Text(stringResource(R.string.category_value, categories.find { it.id == task.categoryId }?.name ?: stringResource(R.string.no_category)))
                Text(stringResource(if (task.isCompleted) R.string.status_completed else R.string.status_pending))
                Button(onClick = onEdit) { Text(stringResource(R.string.edit_task)) }
            }
        }
    }
}
