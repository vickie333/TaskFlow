package com.example.taskflow.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskflow.data.AccountSession
import com.example.taskflow.data.Priority
import com.example.taskflow.data.Task
import com.example.taskflow.data.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val session: AccountSession
) : ViewModel() {
    val accountUid: StateFlow<String?> = session.uid
    private val search = MutableStateFlow("")
    val msgBusqueda: StateFlow<String> = search
    private val filter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = filter
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    // False until Room delivers its first real list, so the UI can tell "loading" from "no tasks".
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    // Lazily, not WhileSubscribed(5000): the upstream must keep following the account even when no screen is
    // collecting, otherwise the cache keeps the previous account's list until someone subscribes again.
    val allTasks: StateFlow<List<Task>> = session.uid.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList<Task>()).onStart { _loading.value = false }
        else repository.getAllTasks().onEach { _loading.value = false }.onStart { _loading.value = true; emit(emptyList()) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val tasks: StateFlow<List<Task>> = combine(session.uid, allTasks, search, filter) { uid, all, query, category ->
        all.filter { it.userId == uid && it.description.contains(query, ignoreCase = true) && (category == null || it.categoryId == category) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            var previous = session.uid.value
            session.uid.collect { uid ->
                if (uid != previous) {
                    search.value = ""
                    filter.value = null
                    _message.value = null
                    _busy.value = false
                }
                previous = uid
            }
        }
    }

    fun userFilterTask(value: String) { search.value = value }
    fun userFilterCategory(value: String?) { filter.value = value }
    fun clearMessage() { _message.value = null }

    private fun perform(action: suspend () -> Unit, onSuccess: () -> Unit = {}) {
        val uid = session.currentUid()
        viewModelScope.launch {
            if (uid == null || session.currentUid() != uid) return@launch
            _busy.value = true
            _message.value = null
            try {
                action()
                if (session.currentUid() == uid) onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (session.currentUid() == uid) _message.value = e.message ?: "No se pudo guardar el cambio"
            } finally {
                if (session.currentUid() == uid) _busy.value = false
            }
        }
    }

    fun addTask(description: String, priority: Priority, categoryId: String?, onSuccess: () -> Unit = {}) =
        perform({ repository.addTask(description, priority, categoryId) }, onSuccess)
    fun removeTask(task: Task, onSuccess: () -> Unit = {}) = perform({ repository.removeTask(task) }, onSuccess)
    fun editTask(task: Task, onSuccess: () -> Unit = {}) = perform({ repository.editTask(task) }, onSuccess)
    fun toggleCompleted(task: Task) = perform({ repository.toggleCompleted(task) })
    fun restoreTask(task: Task) = perform({ repository.restoreTask(task) })
}
