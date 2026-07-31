package com.opensetlist.app.ui.components

import androidx.compose.runtime.Composable

/**
 * Botão voltar no desktop (sem operação: não há botão de sistema).
 *
 * @author ruanitto
 */
@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op on desktop: there is no system back button.
}
