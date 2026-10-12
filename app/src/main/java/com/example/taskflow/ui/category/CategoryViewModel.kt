package com.example.taskflow.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskflow.data.AccountSession
import com.example.taskflow.data.Category
import com.example.taskflow.data.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: CategoryRepository,
    private val session: AccountSession
) : ViewModel() {
    val accountUid: StateFlow<String?> = session.uid
    val categories: StateFlow<List<Category>> = session.uid.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList())
        else repository.getAllCategories().onStart { emit(emptyList()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _msg = MutableStateFlow<String?>(null)
    val msg: StateFlow<String?> = _msg
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    init {
        viewModelScope.launch {
            var previous = session.uid.value
            session.uid.collect { uid ->
                if (uid != previous) {
                    _msg.value = null
                    _busy.value = false
                }
                previous = uid
            }
        }
    }

    private fun perform(action: suspend () -> Unit, onSuccess: () -> Unit = {}) {
        val uid = session.currentUid()
        viewModelScope.launch {
            if (uid == null || session.currentUid() != uid) return@launch
            _busy.value = true
            _msg.value = null
            try {
                action()
                if (session.currentUid() == uid) onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (session.currentUid() == uid) _msg.value = e.message ?: "No se pudo guardar la categoría"
            } finally {
                if (session.currentUid() == uid) _busy.value = false
            }
        }
    }
    fun addCategory(name: String, onSuccess: () -> Unit = {}) = perform({ repository.addCategory(name) }, onSuccess)
    fun removeCategory(category: Category) = perform({ repository.removeCategory(category) })
}
