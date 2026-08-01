package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.prefs.Preferences

private const val KEY_DARK_MODE = "dark_mode"
private const val KEY_KEEP_SCREEN_ON_VIEWER = "keep_screen_on_viewer"
private const val KEY_KEEP_SCREEN_ON_PLAYLIST = "keep_screen_on_playlist"
private const val KEY_KEEP_SCREEN_ON_ALWAYS = "keep_screen_on_always"

/**
 * Persistência de preferências no desktop (java.util.prefs).
 *
 * @author ruanitto
 */
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
            },
            keepScreenOnViewer = { prefs.getBoolean(KEY_KEEP_SCREEN_ON_VIEWER, false) },
            setKeepScreenOnViewer = { value ->
                prefs.putBoolean(KEY_KEEP_SCREEN_ON_VIEWER, value)
                prefs.flush()
            },
            keepScreenOnPlaylist = { prefs.getBoolean(KEY_KEEP_SCREEN_ON_PLAYLIST, false) },
            setKeepScreenOnPlaylist = { value ->
                prefs.putBoolean(KEY_KEEP_SCREEN_ON_PLAYLIST, value)
                prefs.flush()
            },
            keepScreenOnAlways = { prefs.getBoolean(KEY_KEEP_SCREEN_ON_ALWAYS, false) },
            setKeepScreenOnAlways = { value ->
                prefs.putBoolean(KEY_KEEP_SCREEN_ON_ALWAYS, value)
                prefs.flush()
            }
        )
    }
}
