package com.opensetlist.app.data.pedal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Pedal no desktop (sem suporte: estado desabilitado).
 *
 * @author ruanitto
 */
@Composable
actual fun rememberPedalEvents(onEvent: (PedalEvent) -> Unit): PedalState {
    var enabled by remember { mutableStateOf(false) }
    return PedalState(
        isEnabled = enabled,
        setEnabled = { enabled = it }
    )
}
