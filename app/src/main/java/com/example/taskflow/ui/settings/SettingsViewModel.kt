package com.example.taskflow.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskflow.data.AccountSession
import com.example.taskflow.data.LegacyRecoveryRepository
import com.example.taskflow.data.sync.SyncStatus
import com.example.taskflow.data.sync.SyncStatusStore
import com.example.taskflow.data.sync.WorkSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val session: AccountSession,
    private val themeSettings: ThemeSettings,
    private val statuses: SyncStatusStore,
    private val scheduler: WorkSyncScheduler,
    private val recovery: LegacyRecoveryRepository
) : ViewModel() {
    val theme: StateFlow<ThemeChoice> = themeSettings.choice
    val accountUid: StateFlow<String?> = session.uid
    val syncStatus: StateFlow<Pair<String?, SyncStatus>> = session.uid.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(uid to SyncStatus())
        else statuses.observe(uid).onStart { emit(SyncStatus()) }.map { uid to it }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null to SyncStatus())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    private val _recovering = MutableStateFlow(false)
    val recovering: StateFlow<Boolean> = _recovering

    init {
        viewModelScope.launch {
            var previous = session.uid.value
            session.uid.collect { uid ->
                if (uid != previous) { _message.value = null; _recovering.value = false }
                previous = uid
            }
        }
    }

    fun setTheme(value: ThemeChoice) = themeSettings.setTheme(value)
    fun retrySync() = scheduler.retry()
    fun recoverLegacy() {
        val uid = session.currentUid()
        viewModelScope.launch {
            if (uid == null || session.currentUid() != uid) return@launch
            _recovering.value = true
            _message.value = null
            try {
                val count = recovery.recover()
                if (session.currentUid() == uid) _message.value = if (count == 0) "No hay tareas antiguas para recuperar" else "Se recuperaron $count tareas"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (session.currentUid() == uid) _message.value = e.message ?: "No se pudieron recuperar las tareas"
            } finally {
                if (session.currentUid() == uid) _recovering.value = false
            }
        }
    }
}
