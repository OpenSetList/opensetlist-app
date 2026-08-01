package com.opensetlist.app.data

import androidx.compose.runtime.Composable

/**
 * Mantém a tela acesa enquanto [enabled] for verdadeiro.
 *
 * @author ruanitto
 */
@Composable
expect fun KeepScreenOn(enabled: Boolean)
