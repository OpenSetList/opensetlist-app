package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import platform.UIKit.UIApplication

/**
 * Mantém a tela acesa no iOS via [UIApplication.sharedApplication] idleTimerDisabled.
 *
 * @author ruanitto
 */
@Composable
actual fun KeepScreenOn(enabled: Boolean) {
    SideEffect {
        UIApplication.sharedApplication.idleTimerDisabled = enabled
    }
}
