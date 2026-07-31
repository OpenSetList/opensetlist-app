package com.opensetlist.app.data.pedal

import androidx.compose.runtime.Composable

enum class PedalEvent {
    PREVIOUS,
    NEXT,
    PLAY_PAUSE
}

class PedalState(
    val isEnabled: Boolean,
    val setEnabled: (Boolean) -> Unit
)

@Composable
expect fun rememberPedalEvents(onEvent: (PedalEvent) -> Unit): PedalState
