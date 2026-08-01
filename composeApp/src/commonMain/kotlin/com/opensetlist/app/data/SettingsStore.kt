package com.opensetlist.app.data

import androidx.compose.runtime.Composable

/**
 * Acesso às preferências persistentes do app.
 *
 * @author ruanitto
 */
class SettingsStore(
    val isDarkMode: () -> Boolean?,
    val setDarkMode: (Boolean) -> Unit,
    val keepScreenOnViewer: () -> Boolean,
    val setKeepScreenOnViewer: (Boolean) -> Unit,
    val keepScreenOnPlaylist: () -> Boolean,
    val setKeepScreenOnPlaylist: (Boolean) -> Unit,
    val keepScreenOnAlways: () -> Boolean,
    val setKeepScreenOnAlways: (Boolean) -> Unit
)

/**
 * Cria o armazenamento de preferências conforme a plataforma atual.
 *
 * @author ruanitto
 */
@Composable
expect fun rememberSettingsStore(): SettingsStore
