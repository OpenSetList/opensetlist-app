package com.opensetlist.app.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val PREFS_NAME = "opensetlist_settings"
private const val KEY_DARK_MODE = "dark_mode"
private const val KEY_KEEP_SCREEN_ON_VIEWER = "keep_screen_on_viewer"
private const val KEY_KEEP_SCREEN_ON_PLAYLIST = "keep_screen_on_playlist"
private const val KEY_KEEP_SCREEN_ON_ALWAYS = "keep_screen_on_always"

/**
 * Persistência de preferências no Android (SharedPreferences).
 *
 * @author ruanitto
 */
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
            },
            keepScreenOnViewer = { prefs.getBoolean(KEY_KEEP_SCREEN_ON_VIEWER, false) },
            setKeepScreenOnViewer = { value ->
                prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON_VIEWER, value).apply()
            },
            keepScreenOnPlaylist = { prefs.getBoolean(KEY_KEEP_SCREEN_ON_PLAYLIST, false) },
            setKeepScreenOnPlaylist = { value ->
                prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON_PLAYLIST, value).apply()
            },
            keepScreenOnAlways = { prefs.getBoolean(KEY_KEEP_SCREEN_ON_ALWAYS, false) },
            setKeepScreenOnAlways = { value ->
                prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON_ALWAYS, value).apply()
            }
        )
    }
}
