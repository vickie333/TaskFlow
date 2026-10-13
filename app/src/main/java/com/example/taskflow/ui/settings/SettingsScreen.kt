package com.example.taskflow.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import com.example.taskflow.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taskflow.data.sync.SyncPhase
import com.example.taskflow.data.sync.SyncStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val uid by viewModel.accountUid.collectAsStateWithLifecycle()
    val syncState by viewModel.syncStatus.collectAsStateWithLifecycle()
    val status = if (syncState.first == uid) syncState.second else SyncStatus()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val recovering by viewModel.recovering.collectAsStateWithLifecycle()
    var confirmRecovery by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.menu_settings)) }, navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            ThemeChoice.entries.forEach { choice ->
                // One selectable row (>= 48dp) so TalkBack reads "label, radio button, selected" as a single control.
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = theme == choice, role = Role.RadioButton, onClick = { viewModel.setTheme(choice) }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(choice.labelRes), modifier = Modifier.weight(1f))
                    RadioButton(selected = theme == choice, onClick = null)
                }
            }
            HorizontalDivider()
            Text(stringResource(R.string.settings_sync), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(stringResource(when (status.phase) {
                SyncPhase.IDLE -> R.string.sync_idle
                SyncPhase.WAITING -> R.string.sync_waiting
                SyncPhase.RUNNING -> R.string.sync_running
                SyncPhase.SUCCESS -> R.string.sync_success
                SyncPhase.ERROR -> R.string.sync_error
            }))
            status.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = viewModel::retrySync) { Text(stringResource(if (status.phase == SyncPhase.ERROR) R.string.sync_retry else R.string.sync_now)) }
            HorizontalDivider()
            Text(stringResource(R.string.settings_recovery), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.recovery_hint))
            Button(enabled = !recovering, onClick = { confirmRecovery = true }) { Text(stringResource(if (recovering) R.string.recovering else R.string.recover_legacy)) }
            message?.let { Text(it, color = if (it.startsWith("Se recuperaron") || it.startsWith("No hay")) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error) }
        }
    }
    if (confirmRecovery) AlertDialog(
        onDismissRequest = { confirmRecovery = false },
        title = { Text(stringResource(R.string.recover_legacy)) },
        text = { Text(stringResource(R.string.recover_confirm)) },
        confirmButton = { TextButton(onClick = { confirmRecovery = false; viewModel.recoverLegacy() }) { Text(stringResource(R.string.recover)) } },
        dismissButton = { TextButton(onClick = { confirmRecovery = false }) { Text(stringResource(R.string.action_cancel)) } }
    )
}
