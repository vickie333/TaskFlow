package com.example.taskflow.ui.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeChoice(val label: String) { SYSTEM("Sistema"), LIGHT("Claro"), DARK("Oscuro") }

@Singleton
class ThemeSettings @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(readChoice(preferences.getString("theme", null)))
    val choice: StateFlow<ThemeChoice> = state

    fun setTheme(choice: ThemeChoice) {
        preferences.edit().putString("theme", choice.name).apply()
        state.value = choice
    }

    private fun readChoice(value: String?): ThemeChoice =
        ThemeChoice.entries.firstOrNull { it.name == value } ?: ThemeChoice.SYSTEM
}
