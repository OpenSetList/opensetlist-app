package com.opensetlist.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Intercepta o botão voltar do Android.
 *
 * @author ruanitto
 */
@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
