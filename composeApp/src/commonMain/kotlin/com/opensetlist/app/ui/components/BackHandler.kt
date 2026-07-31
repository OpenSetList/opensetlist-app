package com.opensetlist.app.ui.components

import androidx.compose.runtime.Composable

/**
 * Intercepta o botão voltar de cada plataforma.
 *
 * @author ruanitto
 */
@Composable
expect fun AppBackHandler(enabled: Boolean = true, onBack: () -> Unit)
