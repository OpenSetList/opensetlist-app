package com.opensetlist.app.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val PREFS_NAME = "opensetlist_settings"
private const val KEY_DARK_MODE = "dark_mode"

@Composable
actual fun rememberSettingsStore(): SettingsStore {
    val context = LocalContext.current.applicationContext
    return remember {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        SettingsStore(
            isDarkMode = {
                if (prefs.contains(KEY_DARK_MODE)) prefs.getBoolean(KEY_DARK_MODE, false) else null
            },
            setDarkMode = { value ->
                prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()
            }
        )
    }
}
