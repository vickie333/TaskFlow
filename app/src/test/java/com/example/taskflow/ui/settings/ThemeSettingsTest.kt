package com.example.taskflow.ui.settings

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ThemeSettingsTest {
    @Test fun themeChoiceSurvivesRecreation() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().clear().commit()
        val first = ThemeSettings(context)
        assertEquals(ThemeChoice.SYSTEM, first.choice.value)
        first.setTheme(ThemeChoice.DARK)
        assertEquals(ThemeChoice.DARK, ThemeSettings(context).choice.value)
        first.setTheme(ThemeChoice.LIGHT)
        assertEquals(ThemeChoice.LIGHT, ThemeSettings(context).choice.value)
    }
}
