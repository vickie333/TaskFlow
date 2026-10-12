package com.example.taskflow.ui.settings

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
import androidx.compose.runtime.collectAsState
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
    val theme by viewModel.theme.collectAsState()
    val uid by viewModel.accountUid.collectAsState()
    val syncState by viewModel.syncStatus.collectAsState()
    val status = if (syncState.first == uid) syncState.second else SyncStatus()
    val message by viewModel.message.collectAsState()
    val recovering by viewModel.recovering.collectAsState()
    var confirmRecovery by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Ajustes") }, navigationIcon = { TextButton(onClick = onBack) { Text("Volver") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Apariencia", style = MaterialTheme.typography.titleMedium)
            ThemeChoice.entries.forEach { choice ->
                TextButton(onClick = { viewModel.setTheme(choice) }, modifier = Modifier.fillMaxWidth()) {
                    Text(choice.label, modifier = Modifier.weight(1f))
                    RadioButton(selected = theme == choice, onClick = { viewModel.setTheme(choice) })
                }
            }
            HorizontalDivider()
            Text("Sincronización", style = MaterialTheme.typography.titleMedium)
            Text(when (status.phase) {
                SyncPhase.IDLE -> "Sin actividad reciente"
                SyncPhase.WAITING -> "Esperando conexión para sincronizar"
                SyncPhase.RUNNING -> "Sincronizando…"
                SyncPhase.SUCCESS -> "Sincronización completada"
                SyncPhase.ERROR -> "Error de sincronización"
            })
            status.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = viewModel::retrySync) { Text(if (status.phase == SyncPhase.ERROR) "Reintentar" else "Sincronizar ahora") }
            HorizontalDivider()
            Text("Recuperación", style = MaterialTheme.typography.titleMedium)
            Text("Puedes recuperar tareas antiguas sin cuenta. Se añadirán a la cuenta actual.")
            Button(enabled = !recovering, onClick = { confirmRecovery = true }) { Text(if (recovering) "Recuperando…" else "Recuperar tareas antiguas") }
            message?.let { Text(it, color = if (it.startsWith("Se recuperaron") || it.startsWith("No hay")) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error) }
        }
    }
    if (confirmRecovery) AlertDialog(
        onDismissRequest = { confirmRecovery = false },
        title = { Text("Recuperar tareas antiguas") },
        text = { Text("Las tareas antiguas de este dispositivo se añadirán a tu cuenta. ¿Continuar?") },
        confirmButton = { TextButton(onClick = { confirmRecovery = false; viewModel.recoverLegacy() }) { Text("Recuperar") } },
        dismissButton = { TextButton(onClick = { confirmRecovery = false }) { Text("Cancelar") } }
    )
}
