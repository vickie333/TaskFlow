package com.example.taskflow.ui.task

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.taskflow.R
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.taskflow.data.Category
import com.example.taskflow.data.Priority
import com.example.taskflow.data.Task
import com.example.taskflow.ui.auth.AuthViewModel
import com.example.taskflow.ui.category.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    viewModelAuth: AuthViewModel,
    viewModelCategory: CategoryViewModel,
    onTaskClick: (Task) -> Unit,
    onCategoryClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val uid by viewModelAuth.uid.collectAsStateWithLifecycle()
    val allTasks by viewModel.tasks.collectAsStateWithLifecycle()
    val allCategories by viewModelCategory.categories.collectAsStateWithLifecycle()
    val categories = allCategories.filter { it.userId == uid }
    val tasks = allTasks.filter { it.userId == uid }
    val query by viewModel.msgBusqueda.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val error by viewModel.message.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.task_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    val noCategory = stringResource(R.string.no_category)
    var text by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf(Priority.MEDIA) }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var showPriority by rememberSaveable { mutableStateOf(false) }
    var showCategory by rememberSaveable { mutableStateOf(false) }
    var showFilter by rememberSaveable { mutableStateOf(false) }
    var showActions by rememberSaveable { mutableStateOf(false) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }, topBar = {
        TopAppBar(title = { Text(stringResource(R.string.app_name)) }, actions = {
            Box {
                TextButton(onClick = { showActions = true }) { Text(stringResource(R.string.menu)) }
                DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.menu_categories)) }, onClick = { showActions = false; onCategoryClick() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.menu_settings)) }, onClick = { showActions = false; onSettingsClick() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.menu_sign_out)) }, onClick = { showActions = false; viewModelAuth.signOut() })
                }
            }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(query, viewModel::userFilterTask, label = { Text(stringResource(R.string.search)) }, singleLine = true, modifier = Modifier.weight(1f))
                Box {
                    OutlinedButton(onClick = { showFilter = true }) { Text(categories.find { it.id == categoryFilter }?.name ?: stringResource(R.string.filter_all)) }
                    DropdownMenu(expanded = showFilter, onDismissRequest = { showFilter = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.filter_all)) }, onClick = { viewModel.userFilterCategory(null); showFilter = false })
                        categories.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { viewModel.userFilterCategory(item.id); showFilter = false }) }
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(text, { text = it }, label = { Text(stringResource(R.string.new_task)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { showCategory = true }, modifier = Modifier.fillMaxWidth()) { Text(categories.find { it.id == categoryId }?.name ?: noCategory, maxLines = 1) }
                            DropdownMenu(expanded = showCategory, onDismissRequest = { showCategory = false }) {
                                DropdownMenuItem(text = { Text(noCategory) }, onClick = { categoryId = null; showCategory = false })
                                categories.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { categoryId = item.id; showCategory = false }) }
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { showPriority = true }, modifier = Modifier.fillMaxWidth()) { Text(priority.name) }
                            DropdownMenu(expanded = showPriority, onDismissRequest = { showPriority = false }) {
                                Priority.entries.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { priority = item; showPriority = false }) }
                            }
                        }
                    }
                    Button(onClick = {
                        viewModel.addTask(text, priority, categoryId) {
                            text = ""; priority = Priority.MEDIA; categoryId = null
                        }
                    }, enabled = text.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_task)) }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            if (tasks.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    if (loading) {
                        val loadingText = stringResource(R.string.loading_tasks)
                        CircularProgressIndicator(Modifier.semantics { contentDescription = loadingText })
                    } else Text(stringResource(R.string.empty_tasks))
                }
            } else LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(tasks, key = { it.id }) { task ->
                    val completeLabel = stringResource(R.string.cd_complete_task, task.description)
                    val deleteLabel = stringResource(R.string.cd_delete_task, task.description)
                    Row(Modifier.fillMaxWidth().clickable { onTaskClick(task) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(task.isCompleted, onCheckedChange = { viewModel.toggleCompleted(task) }, modifier = Modifier.semantics { contentDescription = completeLabel })
                        Column(Modifier.weight(1f)) {
                            Text(task.description, style = MaterialTheme.typography.bodyLarge, textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None)
                            Text(stringResource(R.string.task_meta, categories.find { it.id == task.categoryId }?.name ?: noCategory, task.priority.name), style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = {
                            viewModel.removeTask(task) {
                                scope.launch {
                                    snackbarHost.currentSnackbarData?.dismiss()
                                    val result = snackbarHost.showSnackbar(deletedMessage, actionLabel = undoLabel, duration = SnackbarDuration.Long)
                                    if (result == SnackbarResult.ActionPerformed) viewModel.restoreTask(task)
                                }
                            }
                        }, modifier = Modifier.semantics { contentDescription = deleteLabel }) { Text(stringResource(R.string.action_delete)) }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
