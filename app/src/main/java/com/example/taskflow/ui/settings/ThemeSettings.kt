package com.example.taskflow.ui.settings

import androidx.annotation.StringRes
import com.example.taskflow.R
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeChoice(@StringRes val labelRes: Int) { SYSTEM(R.string.theme_system), LIGHT(R.string.theme_light), DARK(R.string.theme_dark) }

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
