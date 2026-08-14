package com.example.taskflow.ui.category

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskflow.data.Category
import com.example.taskflow.data.CategoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(private val dao: CategoryDao): ViewModel() {
    val categories: StateFlow<List<Category>> = dao.getAllCategories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _msg = MutableStateFlow<String?>(null)
    val msg: StateFlow<String?> = _msg

    fun addCategory(name: String) {
        if (name.isBlank()) {
            _msg.value = "La categoría necesita un nombre"
            return
        }
        viewModelScope.launch {
            dao.insertCategory(Category(0, name.trim()))
            _msg.value = null
        }
    }

    fun removeCategory(category: Category) {
        viewModelScope.launch {
            try {
                dao.deleteCategory(category)
                _msg.value = null
            } catch (e: SQLiteConstraintException) {
                _msg.value = "No se puede borrar '${category.name}': todavía tiene tareas"
            }
        }
    }
}