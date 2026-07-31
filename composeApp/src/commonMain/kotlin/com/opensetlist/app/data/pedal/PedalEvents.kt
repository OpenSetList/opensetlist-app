package com.opensetlist.app.data.pedal

import androidx.compose.runtime.Composable

/** Eventos emitidos por um pedal externo conectado. */
enum class PedalEvent {
    PREVIOUS,
    NEXT,
    PLAY_PAUSE
}

/** Estado de conexão e habilitação do pedal externo. */
class PedalState(
    val isEnabled: Boolean,
    val setEnabled: (Boolean) -> Unit
)

/**
 * Cria o observador de eventos de pedal conforme a plataforma atual.
 *
 * @author ruanitto
 */
@Composable
expect fun rememberPedalEvents(onEvent: (PedalEvent) -> Unit): PedalState
