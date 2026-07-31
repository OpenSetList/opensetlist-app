package com.opensetlist.app.data

import androidx.compose.runtime.Composable

class SettingsStore(
    val isDarkMode: () -> Boolean?,
    val setDarkMode: (Boolean) -> Unit
)

@Composable
expect fun rememberSettingsStore(): SettingsStore
