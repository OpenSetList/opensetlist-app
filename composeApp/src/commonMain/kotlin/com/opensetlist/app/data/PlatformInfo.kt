package com.opensetlist.app.data

import androidx.compose.runtime.Composable

/**
 * Nome da versão do app na plataforma atual, exibido na gaveta lateral.
 *
 * @author ruanitto
 */
@Composable
expect fun appVersionName(): String
