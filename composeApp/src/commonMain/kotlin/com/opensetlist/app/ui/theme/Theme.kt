package com.opensetlist.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBB86FC),
    secondary = Color(0xFF03DAC6),
    surface = Color(0xFF1E1E1E),
    background = Color(0xFF121212),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onSurface = Color(0xFFE1E1E1),
    onBackground = Color(0xFFE1E1E1)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6200EE),
    secondary = Color(0xFF03DAC6),
    surface = Color(0xFFFFFBFE),
    background = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onSurface = Color(0xFF1C1B1F),
    onBackground = Color(0xFF1C1B1F)
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
