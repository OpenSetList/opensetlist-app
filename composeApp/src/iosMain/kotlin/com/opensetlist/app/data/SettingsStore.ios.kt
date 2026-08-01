package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

private const val KEY_DARK_MODE = "dark_mode"
private const val KEY_KEEP_SCREEN_ON_VIEWER = "keep_screen_on_viewer"
private const val KEY_KEEP_SCREEN_ON_PLAYLIST = "keep_screen_on_playlist"
private const val KEY_KEEP_SCREEN_ON_ALWAYS = "keep_screen_on_always"

/**
 * Persistência de preferências no iOS (NSUserDefaults).
 *
 * @author ruanitto
 */
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
            },
            keepScreenOnViewer = { defaults.boolForKey(KEY_KEEP_SCREEN_ON_VIEWER) },
            setKeepScreenOnViewer = { value ->
                defaults.setBool(value, KEY_KEEP_SCREEN_ON_VIEWER)
            },
            keepScreenOnPlaylist = { defaults.boolForKey(KEY_KEEP_SCREEN_ON_PLAYLIST) },
            setKeepScreenOnPlaylist = { value ->
                defaults.setBool(value, KEY_KEEP_SCREEN_ON_PLAYLIST)
            },
            keepScreenOnAlways = { defaults.boolForKey(KEY_KEEP_SCREEN_ON_ALWAYS) },
            setKeepScreenOnAlways = { value ->
                defaults.setBool(value, KEY_KEEP_SCREEN_ON_ALWAYS)
            }
        )
    }
}
