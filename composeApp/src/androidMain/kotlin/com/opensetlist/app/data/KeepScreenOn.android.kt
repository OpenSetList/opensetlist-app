package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView

/**
 * Mantém a tela acesa no Android via [android.view.View.keepScreenOn].
 *
 * @author ruanitto
 */
@Composable
actual fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    SideEffect {
        view.keepScreenOn = enabled
    }
}
