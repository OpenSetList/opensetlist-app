package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.prefs.Preferences

private const val KEY_DARK_MODE = "dark_mode"

@Composable
actual fun rememberSettingsStore(): SettingsStore {
    return remember {
        val prefs = Preferences.userNodeForPackage(SettingsStore::class.java)
        SettingsStore(
            isDarkMode = {
                if (prefs.get(KEY_DARK_MODE, null) != null) prefs.getBoolean(KEY_DARK_MODE, false)
                else null
            },
            setDarkMode = { value ->
                prefs.putBoolean(KEY_DARK_MODE, value)
                prefs.flush()
            }
        )
    }
}
