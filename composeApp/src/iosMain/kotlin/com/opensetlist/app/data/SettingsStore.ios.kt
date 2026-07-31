package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

private const val KEY_DARK_MODE = "dark_mode"

@Composable
actual fun rememberSettingsStore(): SettingsStore {
    return remember {
        val defaults = NSUserDefaults.standardUserDefaults
        SettingsStore(
            isDarkMode = {
                val stored = defaults.objectForKey(KEY_DARK_MODE)
                if (stored != null) defaults.boolForKey(KEY_DARK_MODE) else null
            },
            setDarkMode = { value ->
                defaults.setBool(value, KEY_DARK_MODE)
            }
        )
    }
}
