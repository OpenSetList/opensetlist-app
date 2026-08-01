package com.opensetlist.app.data

import androidx.compose.runtime.Composable

/**
 * Manter a tela acesa não é aplicável no desktop: mantém o padrão da plataforma.
 *
 * @author ruanitto
 */
@Composable
actual fun KeepScreenOn(enabled: Boolean) {
}
